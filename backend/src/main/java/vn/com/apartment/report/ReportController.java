package vn.com.apartment.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import vn.com.apartment.billing.*;

@RestController @RequestMapping("/api/reports")
public class ReportController {
 private final PaymentRepository payments;
 public ReportController(PaymentRepository payments){this.payments=payments;}
 @GetMapping("/payments")
 public List<PaymentLineResponse> payments(@RequestParam(required=false) Long periodId){
  List<PaymentLineResponse> result=new ArrayList<>();
  for(Payment payment:payments.searchDetailed(periodId)){
   Receivable bill=payment.getReceivable();boolean commercial=bill.isCommercial();
   Long buildingId=commercial?bill.getCommercialSpace().getBuilding().getId():bill.getUnit().getBuilding().getId();
   String building=commercial?bill.getCommercialSpace().getBuilding().getName():bill.getUnit().getBuilding().getName();
   String location=commercial?bill.getCommercialSpace().getCode():bill.getUnit().getCode();
   String customer=commercial?bill.getCommercialTenant().getName():bill.getCustomer().getFullName();
   for(PaymentAllocation allocation:payment.getAllocations())result.add(new PaymentLineResponse(payment.getId(),bill.getBillingPeriod().getCode(),commercial?"COMMERCIAL":"RESIDENTIAL",buildingId,building,location,customer,bill.getReferenceCode(),payment.getPaymentDate(),payment.getPaymentMethod(),payment.getTransactionReference(),payment.getNote(),allocation.getReceivableItem().getDescription(),allocation.getAmount()));
  }
  return result;
 }
 public record PaymentLineResponse(Long paymentId,String periodCode,String sourceType,Long buildingId,String buildingName,String locationCode,String customerName,String receivableReference,LocalDateTime paymentDate,String paymentMethod,String transactionReference,String note,String serviceDescription,BigDecimal amount){}
}
