package vn.com.apartment.billing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankPaymentReconciliationService {
    private final PaymentQrRequestRepository qrRequests;
    private final PaymentRepository payments;
    private final BankTransactionRepository transactions;

    public BankPaymentReconciliationService(PaymentQrRequestRepository qrRequests,
            PaymentRepository payments, BankTransactionRepository transactions) {
        this.qrRequests = qrRequests;
        this.payments = payments;
        this.transactions = transactions;
    }

    @Transactional
    public Result reconcile(String provider, String transactionId, String creditAccount,
            BigDecimal amount, String description, LocalDateTime transactionTime,
            String orderId, String rawPayload) {
        String normalizedProvider = provider.trim().toUpperCase();
        String normalizedTransactionId = transactionId.trim();
        if (transactions.existsByProviderAndProviderTransactionId(normalizedProvider, normalizedTransactionId)) {
            return new Result("DUPLICATE", orderId, "Giao dịch đã được tiếp nhận trước đó.");
        }

        PaymentQrRequest qr = findPendingQr(orderId, description);
        String matchStatus = "UNMATCHED_REFERENCE";
        String message = "Không tìm thấy mã đối soát hoặc QR không còn chờ thanh toán.";
        if (qr != null && creditAccount != null && !digits(creditAccount).equals(digits(qr.getBankAccount()))) {
            matchStatus = "WRONG_ACCOUNT";
            message = "Giao dịch vào sai tài khoản nhận.";
        } else if (qr != null && amount.compareTo(qr.getAmount()) != 0) {
            matchStatus = amount.compareTo(qr.getAmount()) < 0 ? "UNDERPAID" : "OVERPAID";
            message = "Số tiền chuyển không khớp QR, đã đưa vào hàng chờ đối soát.";
        } else if (qr != null) {
            boolean settled = qr.getAllocations().stream()
                .anyMatch(a -> a.getReceivableItem().outstanding().compareTo(a.getAmount()) < 0);
            if (settled) {
                matchStatus = "ALREADY_SETTLED";
                message = "Một dịch vụ trong QR đã được thanh toán bởi giao dịch khác.";
            } else {
                Receivable bill = qr.getReceivable();
                Payment payment = new Payment(bill, qr.getAmount(), transactionTime, "QR",
                    normalizedTransactionId, "Tự động đối soát QR " + qr.getReferenceCode());
                for (PaymentQrAllocation allocation : qr.getAllocations()) {
                    allocation.getReceivableItem().recordPayment(allocation.getAmount());
                    payment.addAllocation(allocation.getReceivableItem(), allocation.getAmount());
                }
                bill.recordPayment(qr.getAmount());
                payments.save(payment);
                qr.markPaid(transactionTime);
                matchStatus = "MATCHED";
                message = "Đã đối soát và phân bổ thanh toán cho " + qr.getAllocations().size() + " dịch vụ.";
            }
        }

        transactions.save(new BankTransaction(normalizedProvider, normalizedTransactionId,
            creditAccount, amount, description, transactionTime, qr, matchStatus, rawPayload));
        return new Result(matchStatus, qr == null ? orderId : qr.getReferenceCode(), message);
    }

    private PaymentQrRequest findPendingQr(String orderId, String description) {
        if (orderId != null && !orderId.isBlank()) {
            Optional<PaymentQrRequest> exact = qrRequests.findDetailedByReference(orderId.trim().toUpperCase());
            if (exact.isPresent() && "PENDING".equals(exact.get().getStatus())) return exact.get();
        }
        String normalizedDescription = Optional.ofNullable(description).orElse("").toUpperCase();
        return qrRequests.findAllDetailed().stream()
            .filter(q -> "PENDING".equals(q.getStatus()) && normalizedDescription.contains(q.getReferenceCode()))
            .findFirst().orElse(null);
    }

    private String digits(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }
    public record Result(String status, String referenceCode, String message) {}
}
