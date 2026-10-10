package vn.com.apartment.billing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.com.apartment.building.*;
import vn.com.apartment.resident.*;
import vn.com.apartment.tariff.*;
import vn.com.apartment.unitprofile.*;
import vn.com.apartment.commercial.*;

@RestController @RequestMapping("/api/billing")
public class BillingController {
    private final BillingPeriodRepository periods; private final ReceivableRepository receivables;
    private final PaymentRepository payments; private final UnitRepository units;
    private final UnitOccupancyRepository occupancies; private final UnitServiceAssignmentRepository assignments;
    private final ServiceTypeRepository serviceTypes; private final ServiceTariffRepository tariffs;
    private final LeaseContractRepository contracts; private final ElectricityReadingRepository electricityReadings;
    public BillingController(BillingPeriodRepository periods,ReceivableRepository receivables,PaymentRepository payments,UnitRepository units,UnitOccupancyRepository occupancies,UnitServiceAssignmentRepository assignments,ServiceTypeRepository serviceTypes,ServiceTariffRepository tariffs,LeaseContractRepository contracts,ElectricityReadingRepository electricityReadings){this.periods=periods;this.receivables=receivables;this.payments=payments;this.units=units;this.occupancies=occupancies;this.assignments=assignments;this.serviceTypes=serviceTypes;this.tariffs=tariffs;this.contracts=contracts;this.electricityReadings=electricityReadings;}

    @GetMapping("/periods") @Transactional(readOnly=true)
    public List<PeriodResponse> periodList(){return periods.findAllByOrderByPeriodStartDesc().stream().map(PeriodResponse::from).toList();}

    @PostMapping("/generate") @Transactional
    public GenerationResponse generate(@Valid @RequestBody GenerateRequest request){
        YearMonth month;
        try{month=YearMonth.parse(request.month());}catch(Exception e){throw bad("Kỳ thu phải có định dạng YYYY-MM.");}
        LocalDate start=month.atDay(1),end=month.atEndOfMonth();
        if(request.dueDate().isBefore(start))throw bad("Hạn thanh toán không được trước ngày bắt đầu kỳ thu.");
        String code=month.format(DateTimeFormatter.ofPattern("yyyyMM"));
        BillingPeriod period=periods.findByCode(code).orElseGet(()->periods.save(new BillingPeriod(code,start,end,request.dueDate())));
        int created=0,updated=0,skipped=0,locked=0;
        for(Unit unit:units.search(request.buildingId(),null)){
            if(unit.getStatus()!=UnitStatus.ACTIVE){skipped++;continue;}
            List<UnitOccupancy> owners=occupancies.findActiveOwnersByUnitId(unit.getId());
            if(owners.isEmpty()){skipped++;continue;}
            Customer owner=owners.getFirst().getCustomer();
            Optional<Receivable> existing=receivables.findByUnitIdAndBillingPeriodId(unit.getId(),period.getId());
            Receivable bill;
            boolean refreshing=false;
            if(existing.isPresent()){
                if(!request.refreshExisting()){skipped++;continue;}
                bill=existing.get();
                if(bill.getPaidAmount().signum()>0){locked++;continue;}
                bill.clearItems();
                refreshing=true;
            }else{
                String ref="FEE-"+code+"-"+unit.getBuilding().getCode()+"-"+unit.getCode();
                bill=new Receivable(ref,owner,unit,period,LocalDate.now(),request.dueDate());
            }
            populateItems(bill,unit,start,end);
            if(bill.getTotalAmount().signum()>0){receivables.save(bill);if(refreshing)updated++;else created++;}else skipped++;
        }
        for(LeaseContract contract:contracts.findBillable(start,end,request.buildingId())){
            Optional<Receivable> existing=receivables.findByLeaseContractIdAndBillingPeriodId(contract.getId(),period.getId());
            Receivable bill;boolean refreshing=false;
            if(existing.isPresent()){
                if(!request.refreshExisting()){skipped++;continue;}
                bill=existing.get();
                if(bill.getPaidAmount().signum()>0){locked++;continue;}
                bill.clearItems();refreshing=true;
            }else{
                CommercialSpace space=contract.getCommercialSpace();
                String ref="LEASE-"+code+"-"+space.getBuilding().getCode()+"-"+space.getCode()+"-"+contract.getId();
                LocalDate contractDue=month.atDay(Math.min(contract.getPaymentDay(),month.lengthOfMonth()));
                if(contractDue.isBefore(contract.getStartDate()))contractDue=contract.getStartDate();
                bill=new Receivable(ref,contract.getTenant(),space,contract,period,LocalDate.now(),contractDue);
            }
            populateCommercialItems(bill,contract,start,end,month.toString());
            if(bill.getTotalAmount().signum()>0){receivables.save(bill);if(refreshing)updated++;else created++;}else skipped++;
        }
        return new GenerationResponse(period.getId(),period.getCode(),created,updated,skipped,locked);
    }

    private void populateItems(Receivable bill,Unit unit,LocalDate start,LocalDate end){
        for(UnitServiceAssignment service:assignments.findByUnitIdAndActiveTrueOrderByMandatoryDescServiceNameAsc(unit.getId())){
            if(service.getStartDate().isAfter(end)||(service.getEndDate()!=null&&service.getEndDate().isBefore(start)))continue;
            if(service.getCollectionMode()!=CollectionMode.MANAGEMENT)continue;
            ServiceType type=serviceTypes.findByCode(service.getServiceCode()).orElse(null);if(type==null)continue;
            BigDecimal quantity=service.getQuantity(),price=service.getUnitPrice();
            if("SERVICE_FEE".equals(service.getServiceCode())){
                quantity=unit.getAreaM2()==null?BigDecimal.ZERO:unit.getAreaM2();
                if(service.getTariff()!=null)price=service.getTariff().getUnitPrice();
                else{List<ServiceTariff> found=tariffs.findEffective("SERVICE_FEE",unit.getBuilding().getId(),start);if(!found.isEmpty())price=found.getFirst().getUnitPrice();}
            }
            if(quantity.signum()>0&&price.signum()>0)bill.addItem(type,service.getServiceName()+(service.getVariant()==null?"":" - "+service.getVariant()),quantity,price,start,end);
        }
    }

    private void populateCommercialItems(Receivable bill,LeaseContract contract,LocalDate start,LocalDate end,String periodCode){
        ServiceType rent=serviceTypes.findByCode("COMMERCIAL_RENT").orElseThrow(()->bad("Chưa cấu hình loại dịch vụ tiền thuê mặt bằng."));
        bill.addItem(rent,"Tiền thuê mặt bằng "+contract.getCommercialSpace().getCode()+" - HĐ "+contract.getContractNumber(),contract.getLeasedAreaM2(),contract.getUnitPricePerM2(),start,end);
        if(contract.getVatAmount().signum()>0)bill.addItem(rent,"Thuế GTGT tiền thuê ("+contract.getVatRate().stripTrailingZeros().toPlainString()+"%)",BigDecimal.ONE,contract.getVatAmount(),start,end);
        ServiceType electric=serviceTypes.findByCode("COMMERCIAL_ELECTRIC").orElse(null);
        if(electric!=null)for(ElectricityReading reading:electricityReadings.findForBilling(contract.getCommercialSpace().getId(),periodCode)){
            if(reading.getConsumption().signum()>0&&reading.getUnitPrice().signum()>0)bill.addItem(electric,"Tiền điện công tơ "+reading.getMeter().getMeterCode()+" ("+reading.getPreviousIndex()+" → "+reading.getCurrentIndex()+")",reading.getConsumption(),reading.getUnitPrice(),start,end);
        }
    }

    @GetMapping("/receivables") @Transactional(readOnly=true)
    public List<ReceivableResponse> list(@RequestParam(required=false) Long periodId){return receivables.search(periodId).stream().map(ReceivableResponse::from).toList();}

    @PutMapping("/receivables/{id}/refresh") @Transactional
    public ReceivableResponse refreshReceivable(@PathVariable Long id){
        Receivable bill=receivables.findById(id).orElseThrow(()->notFound("Không tìm thấy khoản phải thu."));
        if(bill.getPaidAmount().signum()>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Khoản phải thu đã có thanh toán nên không thể tính lại.");
        BillingPeriod period=bill.getBillingPeriod();
        bill.clearItems();
        if(bill.isCommercial())populateCommercialItems(bill,bill.getLeaseContract(),period.getPeriodStart(),period.getPeriodEnd(),YearMonth.from(period.getPeriodStart()).toString());
        else populateItems(bill,bill.getUnit(),period.getPeriodStart(),period.getPeriodEnd());
        if(bill.getTotalAmount().signum()<=0)throw bad("Đối tượng không còn dịch vụ do Ban quản lý thu trong kỳ này.");
        return ReceivableResponse.from(receivables.save(bill));
    }

    @PostMapping("/receivables/{id}/payments") @Transactional
    public ReceivableResponse pay(@PathVariable Long id,@Valid @RequestBody PaymentRequest request){
        Receivable bill=receivables.findById(id).orElseThrow(()->notFound("Không tìm thấy khoản phải thu."));
        Map<Long,ReceivableItem> itemMap=new HashMap<>();for(ReceivableItem item:bill.getItems())itemMap.put(item.getId(),item);
        Set<Long> seen=new HashSet<>();BigDecimal total=BigDecimal.ZERO;
        for(PaymentLineRequest line:request.lines()){
            if(!seen.add(line.itemId()))throw bad("Một dịch vụ không được chọn nhiều lần trong cùng phiếu thu.");
            ReceivableItem item=itemMap.get(line.itemId());if(item==null)throw bad("Dịch vụ không thuộc khoản phải thu này.");
            if(line.amount().compareTo(item.outstanding())>0)throw bad("Số tiền thu vượt quá số còn lại của dịch vụ "+item.getDescription()+".");
            validateMethod(line.paymentMethod());total=total.add(line.amount());
        }
        if(total.compareTo(bill.outstanding())>0)throw bad("Tổng số tiền thu vượt quá số tiền còn phải thu.");
        if(request.grouped()){
            String method=request.lines().getFirst().paymentMethod().trim().toUpperCase();
            if(request.lines().stream().anyMatch(line->!method.equals(line.paymentMethod().trim().toUpperCase())))throw bad("Phiếu thu gộp phải sử dụng cùng một phương thức thanh toán.");
            Payment payment=new Payment(bill,total,LocalDateTime.now(),method,trim(request.transactionReference()),trim(request.note()));
            for(PaymentLineRequest line:request.lines()){ReceivableItem item=itemMap.get(line.itemId());item.recordPayment(line.amount());payment.addAllocation(item,line.amount());}
            payments.save(payment);
        }else{
            for(PaymentLineRequest line:request.lines()){
                ReceivableItem item=itemMap.get(line.itemId());item.recordPayment(line.amount());
                Payment payment=new Payment(bill,line.amount(),LocalDateTime.now(),line.paymentMethod().trim().toUpperCase(),trim(request.transactionReference()),trim(request.note()));payment.addAllocation(item,line.amount());payments.save(payment);
            }
        }
        bill.recordPayment(total);
        return ReceivableResponse.from(bill);
    }

    private void validateMethod(String method){String value=method.trim().toUpperCase();if(!Set.of("CASH","QR","BANK_TRANSFER").contains(value))throw bad("Phương thức thanh toán không hợp lệ.");}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}private ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}
    public record GenerateRequest(@NotBlank String month,@NotNull LocalDate dueDate,Long buildingId,boolean refreshExisting){}
    public record PaymentLineRequest(@NotNull Long itemId,@NotNull @DecimalMin("0.01") BigDecimal amount,@NotBlank String paymentMethod){}
    public record PaymentRequest(boolean grouped,@NotEmpty List<@Valid PaymentLineRequest> lines,String transactionReference,String note){}
    public record GenerationResponse(Long periodId,String periodCode,int created,int updated,int skipped,int locked){}
    public record PeriodResponse(Long id,String code,LocalDate periodStart,LocalDate periodEnd,LocalDate dueDate,String status){static PeriodResponse from(BillingPeriod p){return new PeriodResponse(p.getId(),p.getCode(),p.getPeriodStart(),p.getPeriodEnd(),p.getDueDate(),p.getStatus());}}
    public record ItemResponse(Long id,String description,BigDecimal quantity,BigDecimal unitPrice,BigDecimal amount,BigDecimal paidAmount,BigDecimal outstandingAmount){static ItemResponse from(ReceivableItem i){return new ItemResponse(i.getId(),i.getDescription(),i.getQuantity(),i.getUnitPrice(),i.getAmount(),i.getPaidAmount(),i.outstanding());}}
    public record ReceivableResponse(Long id,String sourceType,String referenceCode,String periodCode,Long buildingId,String buildingName,Long unitId,String unitCode,Long customerId,String customerName,String phone,String zaloPhone,String telegramContact,String zaloUserId,String telegramChatId,LocalDate dueDate,BigDecimal totalAmount,BigDecimal paidAmount,BigDecimal outstandingAmount,String status,List<ItemResponse> items){static ReceivableResponse from(Receivable r){BigDecimal outstanding=r.outstanding();String status=r.getStatus();if(!"PAID".equals(status)&&r.getDueDate().isBefore(LocalDate.now()))status="OVERDUE";if(r.isCommercial()){CommercialSpace s=r.getCommercialSpace();CommercialTenant t=r.getCommercialTenant();return new ReceivableResponse(r.getId(),"COMMERCIAL",r.getReferenceCode(),r.getBillingPeriod().getCode(),s.getBuilding().getId(),s.getBuilding().getName(),s.getId(),s.getCode(),t.getId(),t.getName(),t.getPhone(),null,null,null,null,r.getDueDate(),r.getTotalAmount(),r.getPaidAmount(),outstanding,status,r.getItems().stream().map(ItemResponse::from).toList());}Unit u=r.getUnit();Customer c=r.getCustomer();return new ReceivableResponse(r.getId(),"RESIDENTIAL",r.getReferenceCode(),r.getBillingPeriod().getCode(),u.getBuilding().getId(),u.getBuilding().getName(),u.getId(),u.getCode(),c.getId(),c.getFullName(),c.getPhone(),c.getZaloPhone(),c.getTelegramContact(),c.getZaloUserId(),c.getTelegramChatId(),r.getDueDate(),r.getTotalAmount(),r.getPaidAmount(),outstanding,status,r.getItems().stream().map(ItemResponse::from).toList());}}
}
