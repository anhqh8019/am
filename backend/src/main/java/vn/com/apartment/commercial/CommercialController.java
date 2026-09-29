package vn.com.apartment.commercial;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.com.apartment.building.Building;
import vn.com.apartment.building.BuildingRepository;

@RestController
@RequestMapping("/api/commercial")
public class CommercialController {
    private final CommercialSpaceRepository spaces; private final CommercialTenantRepository tenants;
    private final LeaseContractRepository contracts; private final ElectricityMeterRepository meters;
    private final ElectricityReadingRepository readings; private final BuildingRepository buildings;
    public CommercialController(CommercialSpaceRepository spaces,CommercialTenantRepository tenants,LeaseContractRepository contracts,
            ElectricityMeterRepository meters,ElectricityReadingRepository readings,BuildingRepository buildings){
        this.spaces=spaces;this.tenants=tenants;this.contracts=contracts;this.meters=meters;this.readings=readings;this.buildings=buildings;
    }

    @GetMapping("/spaces") @Transactional(readOnly=true)
    public List<SpaceResponse> spaces(@RequestParam(required=false) Long buildingId,@RequestParam(required=false) String keyword){
        String q=keyword==null||keyword.isBlank()?null:keyword.trim(); return spaces.search(buildingId,q).stream().map(SpaceResponse::from).toList();
    }
    @PostMapping("/spaces") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public SpaceResponse createSpace(@Valid @RequestBody SpaceRequest r){
        Building b=building(r.buildingId());String code=code(r.code());
        if(spaces.existsByBuildingIdAndCodeIgnoreCase(b.getId(),code)) conflict("Mã mặt bằng đã tồn tại trong tòa nhà.");
        return SpaceResponse.from(spaces.save(new CommercialSpace(b,code,r.location().trim(),trim(r.description()),r.totalAreaM2())));
    }
    @PutMapping("/spaces/{id}") @Transactional
    public SpaceResponse updateSpace(@PathVariable Long id,@Valid @RequestBody SpaceRequest r){
        CommercialSpace s=space(id);Building b=building(r.buildingId());String code=code(r.code());
        if(spaces.existsByBuildingIdAndCodeIgnoreCaseAndIdNot(b.getId(),code,id)) conflict("Mã mặt bằng đã tồn tại trong tòa nhà.");
        s.update(b,code,r.location().trim(),trim(r.description()),r.totalAreaM2(),r.status()); return SpaceResponse.from(s);
    }
    @DeleteMapping("/spaces/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void deactivateSpace(@PathVariable Long id){space(id).deactivate();}

    @GetMapping("/tenants") @Transactional(readOnly=true) public List<TenantResponse> tenants(){return tenants.findAllByActiveTrueOrderByNameAsc().stream().map(TenantResponse::from).toList();}
    @PostMapping("/tenants") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public TenantResponse createTenant(@Valid @RequestBody TenantRequest r){
        String code=code(r.code());if(tenants.existsByCodeIgnoreCase(code)) conflict("Mã khách thuê đã tồn tại.");
        return TenantResponse.from(tenants.save(new CommercialTenant(code,r.name().trim(),trim(r.taxCode()),trim(r.representative()),trim(r.phone()),trim(r.email()),trim(r.billingAddress()))));
    }

    @GetMapping("/contracts") @Transactional(readOnly=true) public List<ContractResponse> contracts(){return contracts.findAllDetailed().stream().map(ContractResponse::from).toList();}
    @PostMapping("/contracts") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public ContractResponse createContract(@Valid @RequestBody ContractRequest r){
        String number=code(r.contractNumber());if(contracts.existsByContractNumberIgnoreCase(number)) conflict("Số hợp đồng đã tồn tại.");
        CommercialSpace s=space(r.spaceId());CommercialTenant t=tenant(r.tenantId());
        if(r.endDate().isBefore(r.startDate())) bad("Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        if(r.leasedAreaM2().compareTo(s.getTotalAreaM2())>0) bad("Diện tích tính tiền không được lớn hơn tổng diện tích mặt bằng.");
        LeaseContract c=new LeaseContract(number,s,t,r.leasedAreaM2(),r.unitPricePerM2(),r.vatRate(),r.depositAmount(),r.startDate(),r.endDate(),r.paymentDay(),r.status(),trim(r.notes()));
        if(r.status()==LeaseStatus.ACTIVE) s.update(s.getBuilding(),s.getCode(),s.getLocation(),s.getDescription(),s.getTotalAreaM2(),CommercialSpaceStatus.LEASED);
        return ContractResponse.from(contracts.save(c));
    }

    @GetMapping("/meters") @Transactional(readOnly=true) public List<MeterResponse> meters(){return meters.findAllDetailed().stream().map(MeterResponse::from).toList();}
    @PostMapping("/meters") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public MeterResponse createMeter(@Valid @RequestBody MeterRequest r){
        String meterCode=code(r.meterCode());if(meters.existsByMeterCodeIgnoreCase(meterCode)) conflict("Mã công tơ đã tồn tại.");
        return MeterResponse.from(meters.save(new ElectricityMeter(space(r.spaceId()),meterCode,trim(r.description()),r.defaultUnitPrice())));
    }
    @GetMapping("/readings") @Transactional(readOnly=true) public List<ReadingResponse> readings(){return readings.findAllDetailed().stream().map(ReadingResponse::from).toList();}
    @PostMapping("/readings") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public ReadingResponse createReading(@Valid @RequestBody ReadingRequest r){
        ElectricityMeter m=meters.findById(r.meterId()).orElseThrow(()->notFound("Không tìm thấy công tơ."));
        String period=r.periodCode().trim().toUpperCase();if(readings.existsByMeterIdAndPeriodCode(m.getId(),period)) conflict("Công tơ đã có chỉ số trong kỳ này.");
        if(r.currentIndex().compareTo(r.previousIndex())<0) bad("Chỉ số cuối phải lớn hơn hoặc bằng chỉ số đầu.");
        return ReadingResponse.from(readings.save(new ElectricityReading(m,period,r.previousIndex(),r.currentIndex(),r.unitPrice(),r.readingDate(),trim(r.imageUrl()),trim(r.note()))));
    }

    private Building building(Long id){return buildings.findById(id).filter(Building::isActive).orElseThrow(()->notFound("Không tìm thấy tòa nhà."));}
    private CommercialSpace space(Long id){return spaces.findById(id).orElseThrow(()->notFound("Không tìm thấy mặt bằng."));}
    private CommercialTenant tenant(Long id){return tenants.findById(id).filter(CommercialTenant::isActive).orElseThrow(()->notFound("Không tìm thấy khách thuê."));}
    private String code(String v){return v.trim().toUpperCase();} private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);} private void conflict(String m){throw new ResponseStatusException(HttpStatus.CONFLICT,m);} private void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}

    public record SpaceRequest(@NotNull Long buildingId,@NotBlank @Size(max=30) String code,@NotBlank @Size(max=200) String location,@Size(max=500) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal totalAreaM2,@NotNull CommercialSpaceStatus status){}
    public record SpaceResponse(Long id,Long buildingId,String buildingName,String code,String location,String description,BigDecimal totalAreaM2,CommercialSpaceStatus status){
        static SpaceResponse from(CommercialSpace s){return new SpaceResponse(s.getId(),s.getBuilding().getId(),s.getBuilding().getName(),s.getCode(),s.getLocation(),s.getDescription(),s.getTotalAreaM2(),s.getStatus());}}
    public record TenantRequest(@NotBlank @Size(max=30) String code,@NotBlank @Size(max=200) String name,@Size(max=30) String taxCode,@Size(max=150) String representative,@Size(max=30) String phone,@Email @Size(max=150) String email,@Size(max=300) String billingAddress){}
    public record TenantResponse(Long id,String code,String name,String taxCode,String representative,String phone,String email,String billingAddress){static TenantResponse from(CommercialTenant t){return new TenantResponse(t.getId(),t.getCode(),t.getName(),t.getTaxCode(),t.getRepresentative(),t.getPhone(),t.getEmail(),t.getBillingAddress());}}
    public record ContractRequest(@NotBlank @Size(max=50) String contractNumber,@NotNull Long spaceId,@NotNull Long tenantId,@NotNull @DecimalMin("0.01") BigDecimal leasedAreaM2,
        @NotNull @DecimalMin("0") BigDecimal unitPricePerM2,@NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal vatRate,@NotNull @DecimalMin("0") BigDecimal depositAmount,
        @NotNull LocalDate startDate,@NotNull LocalDate endDate,@Min(1) @Max(28) int paymentDay,@NotNull LeaseStatus status,@Size(max=500) String notes){}
    public record ContractResponse(Long id,String contractNumber,Long spaceId,String spaceCode,String location,Long tenantId,String tenantName,BigDecimal leasedAreaM2,BigDecimal unitPricePerM2,BigDecimal vatRate,BigDecimal subtotal,BigDecimal vatAmount,BigDecimal monthlyTotal,LocalDate startDate,LocalDate endDate,int paymentDay,LeaseStatus status){
        static ContractResponse from(LeaseContract c){return new ContractResponse(c.getId(),c.getContractNumber(),c.getCommercialSpace().getId(),c.getCommercialSpace().getCode(),c.getCommercialSpace().getLocation(),c.getTenant().getId(),c.getTenant().getName(),c.getLeasedAreaM2(),c.getUnitPricePerM2(),c.getVatRate(),c.getSubtotal(),c.getVatAmount(),c.getMonthlyTotal(),c.getStartDate(),c.getEndDate(),c.getPaymentDay(),c.getStatus());}}
    public record MeterRequest(@NotNull Long spaceId,@NotBlank @Size(max=50) String meterCode,@Size(max=200) String description,@NotNull @DecimalMin("0") BigDecimal defaultUnitPrice){}
    public record MeterResponse(Long id,Long spaceId,String spaceCode,String meterCode,String description,BigDecimal defaultUnitPrice,boolean active){static MeterResponse from(ElectricityMeter m){return new MeterResponse(m.getId(),m.getCommercialSpace().getId(),m.getCommercialSpace().getCode(),m.getMeterCode(),m.getDescription(),m.getDefaultUnitPrice(),m.isActive());}}
    public record ReadingRequest(@NotNull Long meterId,@NotBlank @Pattern(regexp="\\d{4}-(0[1-9]|1[0-2])") String periodCode,@NotNull @DecimalMin("0") BigDecimal previousIndex,@NotNull @DecimalMin("0") BigDecimal currentIndex,@NotNull @DecimalMin("0") BigDecimal unitPrice,@NotNull LocalDate readingDate,@Size(max=500) String imageUrl,@Size(max=300) String note){}
    public record ReadingResponse(Long id,Long meterId,String meterCode,String spaceCode,String periodCode,BigDecimal previousIndex,BigDecimal currentIndex,BigDecimal consumption,BigDecimal unitPrice,BigDecimal amount,LocalDate readingDate){static ReadingResponse from(ElectricityReading r){return new ReadingResponse(r.getId(),r.getMeter().getId(),r.getMeter().getMeterCode(),r.getMeter().getCommercialSpace().getCode(),r.getPeriodCode(),r.getPreviousIndex(),r.getCurrentIndex(),r.getConsumption(),r.getUnitPrice(),r.getAmount(),r.getReadingDate());}}
}
