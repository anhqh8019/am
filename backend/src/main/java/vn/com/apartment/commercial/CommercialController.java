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
    private final CommercialServiceAssignmentRepository serviceAssignments; private final CommercialParkingVehicleRepository parkingVehicles;
    public CommercialController(CommercialSpaceRepository spaces,CommercialTenantRepository tenants,LeaseContractRepository contracts,
            ElectricityMeterRepository meters,ElectricityReadingRepository readings,BuildingRepository buildings,CommercialServiceAssignmentRepository serviceAssignments,CommercialParkingVehicleRepository parkingVehicles){
        this.spaces=spaces;this.tenants=tenants;this.contracts=contracts;this.meters=meters;this.readings=readings;this.buildings=buildings;
        this.serviceAssignments=serviceAssignments;this.parkingVehicles=parkingVehicles;
    }

    @GetMapping("/spaces") @Transactional(readOnly=true)
    public List<SpaceResponse> spaces(@RequestParam(required=false) Long buildingId,@RequestParam(required=false) String keyword){
        String q=keyword==null||keyword.isBlank()?null:keyword.trim(); return spaces.search(buildingId,q).stream().map(this::spaceResponse).toList();
    }
    @PostMapping("/spaces") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public SpaceResponse createSpace(@Valid @RequestBody SpaceRequest r){
        Building b=building(r.buildingId());String code=code(r.code());
        if(spaces.existsByBuildingIdAndCodeIgnoreCase(b.getId(),code)) conflict("Mã mặt bằng đã tồn tại trong tòa nhà.");
        return spaceResponse(spaces.save(new CommercialSpace(b,code,r.location().trim(),trim(r.description()),r.totalAreaM2())));
    }
    @PutMapping("/spaces/{id}") @Transactional
    public SpaceResponse updateSpace(@PathVariable Long id,@Valid @RequestBody SpaceRequest r){
        CommercialSpace s=space(id);Building b=building(r.buildingId());String code=code(r.code());
        if(spaces.existsByBuildingIdAndCodeIgnoreCaseAndIdNot(b.getId(),code,id)) conflict("Mã mặt bằng đã tồn tại trong tòa nhà.");
        s.update(b,code,r.location().trim(),trim(r.description()),r.totalAreaM2(),r.status()); return spaceResponse(s);
    }
    @DeleteMapping("/spaces/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void deactivateSpace(@PathVariable Long id){
        CommercialSpace s=space(id);
        if(contracts.existsByCommercialSpace_IdAndStatus(id,LeaseStatus.ACTIVE))conflict("Mặt bằng đang có hợp đồng hiệu lực. Hãy kết thúc hợp đồng trước khi xóa.");
        s.deactivate();
    }

    @GetMapping("/tenants") @Transactional(readOnly=true) public List<TenantResponse> tenants(){return tenants.findAllByActiveTrueOrderByNameAsc().stream().map(TenantResponse::from).toList();}
    @PostMapping("/tenants") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public TenantResponse createTenant(@Valid @RequestBody TenantRequest r){
        String code=code(r.code());if(tenants.existsByCodeIgnoreCase(code)) conflict("Mã khách thuê đã tồn tại.");
        return TenantResponse.from(tenants.save(new CommercialTenant(code,r.name().trim(),trim(r.taxCode()),trim(r.representative()),trim(r.phone()),trim(r.email()),trim(r.billingAddress()))));
    }
    @PutMapping("/tenants/{id}") @Transactional
    public TenantResponse updateTenant(@PathVariable Long id,@Valid @RequestBody TenantRequest r){
        CommercialTenant t=tenant(id);String code=code(r.code());
        if(tenants.existsByCodeIgnoreCaseAndIdNot(code,id)) conflict("Mã khách thuê đã tồn tại.");
        t.update(code,r.name().trim(),trim(r.taxCode()),trim(r.representative()),trim(r.phone()),trim(r.email()),trim(r.billingAddress()));
        return TenantResponse.from(t);
    }
    @DeleteMapping("/tenants/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void deactivateTenant(@PathVariable Long id){
        CommercialTenant t=tenant(id);
        if(contracts.existsByTenant_IdAndStatus(id,LeaseStatus.ACTIVE)) conflict("Khách thuê đang có hợp đồng hiệu lực. Hãy kết thúc hợp đồng trước khi xóa.");
        t.deactivate();
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
    @DeleteMapping("/meters/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void deactivateMeter(@PathVariable Long id){
        ElectricityMeter meter=meters.findById(id).orElseThrow(()->notFound("Không tìm thấy công tơ."));
        if(!meter.isActive())return;
        meter.deactivate();
    }
    @GetMapping("/readings") @Transactional(readOnly=true) public List<ReadingResponse> readings(){return readings.findAllDetailed().stream().map(ReadingResponse::from).toList();}
    @PostMapping("/readings") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public ReadingResponse createReading(@Valid @RequestBody ReadingRequest r){
        ElectricityMeter m=meters.findById(r.meterId()).filter(ElectricityMeter::isActive).orElseThrow(()->notFound("Công tơ không tồn tại hoặc đã ngừng sử dụng."));
        String period=r.periodCode().trim().toUpperCase();if(readings.existsByMeterIdAndPeriodCode(m.getId(),period)) conflict("Công tơ đã có chỉ số trong kỳ này.");
        if(r.currentIndex().compareTo(r.previousIndex())<0) bad("Chỉ số cuối phải lớn hơn hoặc bằng chỉ số đầu.");
        return ReadingResponse.from(readings.save(new ElectricityReading(m,period,r.previousIndex(),r.currentIndex(),r.unitPrice(),r.readingDate(),trim(r.imageUrl()),trim(r.note()))));
    }

    @GetMapping("/services") @Transactional(readOnly=true)
    public List<ServiceResponse> services(){return serviceAssignments.findAllDetailed().stream().map(ServiceResponse::from).toList();}
    @PostMapping("/services") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public ServiceResponse createService(@Valid @RequestBody ServiceRequest r){
        String code=code(r.serviceCode()),method=code(r.pricingMethod()),vehicleType=trim(r.vehicleType());
        if(!List.of("BUILDING_SERVICE","PARKING").contains(code))bad("Loại dịch vụ mặt bằng không hợp lệ.");
        if(!List.of("PER_M2","FIXED","PER_VEHICLE").contains(method))bad("Cách tính phí không hợp lệ.");
        if("PER_VEHICLE".equals(method)&&vehicleType==null)bad("Phí theo đầu xe phải chọn loại xe.");
        CommercialServiceAssignment a=new CommercialServiceAssignment(space(r.spaceId()),code,r.serviceName().trim(),method,vehicleType==null?null:vehicleType.toUpperCase(),r.unitPrice());
        return ServiceResponse.from(serviceAssignments.save(a));
    }
    @DeleteMapping("/services/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void removeService(@PathVariable Long id){CommercialServiceAssignment a=serviceAssignments.findById(id).orElseThrow(()->notFound("Không tìm thấy dịch vụ."));a.deactivate();for(CommercialParkingVehicle v:parkingVehicles.findActiveByAssignment(id))v.deactivate();}
    @GetMapping("/vehicles") @Transactional(readOnly=true)
    public List<VehicleResponse> vehicles(){return parkingVehicles.findAllActiveDetailed().stream().map(VehicleResponse::from).toList();}
    @PostMapping("/vehicles") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public VehicleResponse createVehicle(@Valid @RequestBody VehicleRequest r){
        CommercialServiceAssignment a=serviceAssignments.findById(r.assignmentId()).filter(CommercialServiceAssignment::isActive).orElseThrow(()->notFound("Không tìm thấy dịch vụ bãi xe."));
        if(!"PARKING".equals(a.getServiceCode()))bad("Chỉ dịch vụ bãi xe mới được thêm xe.");
        String plate=code(r.plateNumber());if(parkingVehicles.existsByPlateNumberIgnoreCaseAndActiveTrue(plate))conflict("Biển số xe đang được sử dụng.");
        String requestedType=trim(r.vehicleType());
        if("FIXED".equals(a.getPricingMethod())&&requestedType==null)bad("Gói khoán phải chọn loại xe.");
        String vehicleType="PER_VEHICLE".equals(a.getPricingMethod())?a.getVehicleType():requestedType.toUpperCase();
        if(!List.of("MOTORBIKE","CAR").contains(vehicleType))bad("Loại xe không hợp lệ.");
        return VehicleResponse.from(parkingVehicles.save(new CommercialParkingVehicle(a,plate,vehicleType,r.electricVehicle())));
    }
    @DeleteMapping("/vehicles/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void removeVehicle(@PathVariable Long id){parkingVehicles.findById(id).orElseThrow(()->notFound("Không tìm thấy xe.")).deactivate();}

    private Building building(Long id){return buildings.findById(id).filter(Building::isActive).orElseThrow(()->notFound("Không tìm thấy tòa nhà."));}
    private CommercialSpace space(Long id){return spaces.findById(id).orElseThrow(()->notFound("Không tìm thấy mặt bằng."));}
    private CommercialTenant tenant(Long id){return tenants.findById(id).filter(CommercialTenant::isActive).orElseThrow(()->notFound("Không tìm thấy khách thuê."));}
    private SpaceResponse spaceResponse(CommercialSpace s){
        LeaseContract active=contracts.findFirstByCommercialSpace_IdAndStatusOrderByStartDateDesc(s.getId(),LeaseStatus.ACTIVE).orElse(null);
        return new SpaceResponse(s.getId(),s.getBuilding().getId(),s.getBuilding().getName(),s.getCode(),s.getLocation(),s.getDescription(),s.getTotalAreaM2(),s.getStatus(),active==null?null:active.getTenant().getId(),active==null?null:active.getTenant().getName(),active==null?null:active.getContractNumber());
    }
    private String code(String v){return v.trim().toUpperCase();} private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);} private void conflict(String m){throw new ResponseStatusException(HttpStatus.CONFLICT,m);} private void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}

    public record SpaceRequest(@NotNull Long buildingId,@NotBlank @Size(max=30) String code,@NotBlank @Size(max=200) String location,@Size(max=500) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal totalAreaM2,@NotNull CommercialSpaceStatus status){}
    public record SpaceResponse(Long id,Long buildingId,String buildingName,String code,String location,String description,BigDecimal totalAreaM2,CommercialSpaceStatus status,Long tenantId,String tenantName,String contractNumber){}
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
    public record ServiceRequest(@NotNull Long spaceId,@NotBlank String serviceCode,@NotBlank @Size(max=150) String serviceName,@NotBlank String pricingMethod,@Size(max=20) String vehicleType,@NotNull @DecimalMin("0") BigDecimal unitPrice){}
    public record ServiceResponse(Long id,Long spaceId,String spaceCode,String serviceCode,String serviceName,String pricingMethod,String vehicleType,BigDecimal unitPrice,boolean active){static ServiceResponse from(CommercialServiceAssignment a){return new ServiceResponse(a.getId(),a.getSpace().getId(),a.getSpace().getCode(),a.getServiceCode(),a.getServiceName(),a.getPricingMethod(),a.getVehicleType(),a.getUnitPrice(),a.isActive());}}
    public record VehicleRequest(@NotNull Long assignmentId,@NotBlank @Size(max=30) String plateNumber,@Size(max=20) String vehicleType,boolean electricVehicle){}
    public record VehicleResponse(Long id,Long assignmentId,String spaceCode,String plateNumber,String vehicleType,boolean electricVehicle){static VehicleResponse from(CommercialParkingVehicle v){return new VehicleResponse(v.getId(),v.getAssignment().getId(),v.getAssignment().getSpace().getCode(),v.getPlateNumber(),v.getVehicleType(),v.isElectricVehicle());}}
}
