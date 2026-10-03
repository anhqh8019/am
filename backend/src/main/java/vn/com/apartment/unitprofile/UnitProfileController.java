package vn.com.apartment.unitprofile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.com.apartment.building.*;
import vn.com.apartment.resident.*;
import vn.com.apartment.tariff.ServiceTariff;
import vn.com.apartment.tariff.ServiceTariffRepository;

@RestController @RequestMapping("/api/unit-profiles")
public class UnitProfileController {
    private final UnitRepository units; private final CustomerRepository customers; private final UnitOccupancyRepository occupancies; private final UnitServiceAssignmentRepository services; private final ServiceTariffRepository tariffs;
    public UnitProfileController(UnitRepository units,CustomerRepository customers,UnitOccupancyRepository occupancies,UnitServiceAssignmentRepository services,ServiceTariffRepository tariffs){this.units=units;this.customers=customers;this.occupancies=occupancies;this.services=services;this.tariffs=tariffs;}

    @GetMapping("/{unitId}") @Transactional(readOnly=true)
    public ProfileResponse profile(@PathVariable Long unitId){
        Unit u=unit(unitId);List<OccupantResponse> people=occupancies.findActiveByUnitId(unitId).stream().map(OccupantResponse::from).toList();
        List<TariffOptionResponse> availableTariffs=tariffs.findApplicable("SERVICE_FEE",u.getBuilding().getId()).stream().map(TariffOptionResponse::from).toList();
        return new ProfileResponse(u.getId(),u.getCode(),u.getBuilding().getName(),u.getAreaM2(),people,services.findByUnitIdAndActiveTrueOrderByMandatoryDescServiceNameAsc(unitId).stream().map(s->serviceResponse(s,u)).toList(),availableTariffs);
    }

    @PostMapping("/{unitId}/owner") @Transactional
    public ProfileResponse changeOwner(@PathVariable Long unitId,@Valid @RequestBody OwnerRequest r){
        Unit u=unit(unitId);String nationalId=r.nationalId().trim();
        if(!nationalId.matches("\\d{12}"))bad("CCCD phải gồm đúng 12 chữ số.");
        Customer owner=customers.findByNationalId(nationalId).orElseGet(()->{
            if(customers.existsByCustomerCodeIgnoreCase(r.customerCode().trim())) conflict("Mã khách hàng đã tồn tại.");
            return customers.save(new Customer(r.customerCode().trim().toUpperCase(),CustomerType.INDIVIDUAL,r.fullName().trim(),r.phone().trim(),trim(r.email()),null,r.birthYear(),r.gender(),nationalId));
        });
        if(owner.getPhone()==null||owner.getBirthYear()==null||owner.getGender()==null) bad("Chủ hộ phải có số điện thoại, năm sinh và giới tính.");
        for(UnitOccupancy old:occupancies.findActiveOwnersByUnitId(unitId)){
            if(old.getCustomer().getId().equals(owner.getId())) conflict("Khách hàng này đang là chủ hộ hiện tại.");
            LocalDate end=r.effectiveDate().minusDays(1);if(end.isBefore(old.getStartDate()))end=old.getStartDate();old.end(end);
        }
        occupancies.findActivePrimaryByUnitId(unitId).forEach(UnitOccupancy::removePrimary);
        occupancies.save(new UnitOccupancy(u,owner,OccupancyRole.OWNER,r.effectiveDate(),null,true));
        if(!services.existsByUnitIdAndServiceCodeAndActiveTrue(unitId,"SERVICE_FEE")){
            BigDecimal quantity=u.getAreaM2()==null?BigDecimal.ONE:u.getAreaM2();
            services.save(new UnitServiceAssignment(u,"SERVICE_FEE","Phí dịch vụ tòa nhà",null,ChargeMethod.BY_AREA,quantity,BigDecimal.ZERO,CollectionMode.MANAGEMENT,true,r.effectiveDate()));
        }
        return profile(unitId);
    }

    @PostMapping("/{unitId}/services") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public ServiceResponse addService(@PathVariable Long unitId,@Valid @RequestBody ServiceRequest r){
        Unit u=unit(unitId);String code=r.serviceCode().trim().toUpperCase();
        if(services.existsByUnitIdAndServiceCodeAndActiveTrue(unitId,code)&&!code.equals("PARKING_MONTHLY")) conflict("Căn hộ đã được gán dịch vụ này.");
        String plate=normalizePlate(r.licensePlate());
        if(code.equals("PARKING_MONTHLY")&&("Xe máy".equalsIgnoreCase(r.variant())||"Ô tô".equalsIgnoreCase(r.variant()))&&plate==null) bad("Xe máy và ô tô bắt buộc phải có biển số.");
        if(plate!=null&&services.existsByLicensePlateAndActiveTrue(plate)) conflict("Biển số xe đang được đăng ký cho một căn hộ khác.");
        BigDecimal quantity=r.chargeMethod()==ChargeMethod.BY_AREA?(u.getAreaM2()==null?BigDecimal.ONE:u.getAreaM2()):r.quantity();
        return ServiceResponse.from(services.save(new UnitServiceAssignment(u,code,r.serviceName().trim(),trim(r.variant()),r.chargeMethod(),quantity,r.unitPrice(),r.collectionMode(),false,r.startDate(),plate,r.electricVehicle())));
    }

    @DeleteMapping("/services/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void endService(@PathVariable Long id){UnitServiceAssignment s=services.findById(id).orElseThrow(()->notFound("Không tìm thấy dịch vụ."));try{s.end(LocalDate.now());}catch(IllegalStateException e){bad(e.getMessage());}}

    @PutMapping("/{unitId}/service-fee-tariff") @Transactional
    public ProfileResponse assignServiceFeeTariff(@PathVariable Long unitId,@Valid @RequestBody TariffAssignmentRequest r){
        Unit u=unit(unitId);
        UnitServiceAssignment service=services.findByUnitIdAndActiveTrueOrderByMandatoryDescServiceNameAsc(unitId).stream()
            .filter(s->"SERVICE_FEE".equals(s.getServiceCode())).findFirst()
            .orElseThrow(()->notFound("Căn hộ chưa được gán phí dịch vụ tòa nhà."));
        ServiceTariff tariff=null;
        if(r.tariffId()!=null){
            tariff=tariffs.findById(r.tariffId()).orElseThrow(()->notFound("Không tìm thấy biểu phí."));
            if(!"SERVICE_FEE".equals(tariff.getServiceCode()))bad("Biểu phí không thuộc phí dịch vụ tòa nhà.");
            if(tariff.getBuilding()!=null&&!tariff.getBuilding().getId().equals(u.getBuilding().getId()))bad("Biểu phí không áp dụng cho tòa nhà của căn hộ này.");
        }
        service.assignTariff(tariff);
        return profile(unitId);
    }

    private Unit unit(Long id){return units.findById(id).orElseThrow(()->notFound("Không tìm thấy căn hộ."));}
    private ServiceResponse serviceResponse(UnitServiceAssignment service,Unit unit){
        BigDecimal price=service.getUnitPrice();
        BigDecimal quantity=service.getQuantity();
        if("SERVICE_FEE".equals(service.getServiceCode())){
            quantity=unit.getAreaM2()==null?BigDecimal.ZERO:unit.getAreaM2();
            if(service.getTariff()!=null)price=service.getTariff().getUnitPrice();
            else{
                List<ServiceTariff> effective=tariffs.findEffective(service.getServiceCode(),unit.getBuilding().getId(),LocalDate.now());
                if(!effective.isEmpty())price=effective.getFirst().getUnitPrice();
            }
        }
        return ServiceResponse.from(service,quantity,price);
    }
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();} private String normalizePlate(String v){String p=trim(v);return p==null?null:p.toUpperCase().replaceAll("[\\s.-]","");} private ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);} private void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);} private void conflict(String m){throw new ResponseStatusException(HttpStatus.CONFLICT,m);}

    public record OwnerRequest(@NotBlank @Size(max=30) String customerCode,@NotBlank @Size(max=200) String fullName,@NotBlank @Size(max=30) String phone,@Email String email,@Min(1900) @Max(2100) int birthYear,@NotNull Gender gender,@NotBlank String nationalId,@NotNull LocalDate effectiveDate){}
    public record ServiceRequest(@NotBlank String serviceCode,@NotBlank String serviceName,String variant,@NotNull ChargeMethod chargeMethod,@NotNull @DecimalMin("0.001") BigDecimal quantity,@NotNull @DecimalMin("0") BigDecimal unitPrice,@NotNull CollectionMode collectionMode,@NotNull LocalDate startDate,String licensePlate,boolean electricVehicle){}
    public record TariffAssignmentRequest(Long tariffId){}
    public record OccupantResponse(Long id,Long customerId,String name,String phone,Integer birthYear,Gender gender,String nationalId,OccupancyRole role,boolean primary){static OccupantResponse from(UnitOccupancy o){Customer c=o.getCustomer();return new OccupantResponse(o.getId(),c.getId(),c.getFullName(),c.getPhone(),c.getBirthYear(),c.getGender(),c.getNationalId(),o.getOccupancyRole(),o.isPrimary());}}
    public record ServiceResponse(Long id,String serviceCode,String serviceName,String variant,ChargeMethod chargeMethod,BigDecimal quantity,BigDecimal unitPrice,CollectionMode collectionMode,String licensePlate,boolean electricVehicle,boolean mandatory,BigDecimal estimatedAmount,Long tariffId){static ServiceResponse from(UnitServiceAssignment s){return from(s,s.getQuantity(),s.getUnitPrice());}static ServiceResponse from(UnitServiceAssignment s,BigDecimal quantity,BigDecimal price){return new ServiceResponse(s.getId(),s.getServiceCode(),s.getServiceName(),s.getVariant(),s.getChargeMethod(),quantity,price,s.getCollectionMode(),s.getLicensePlate(),s.isElectricVehicle(),s.isMandatory(),quantity.multiply(price),s.getTariff()==null?null:s.getTariff().getId());}}
    public record TariffOptionResponse(Long id,String scopeName,BigDecimal unitPrice,LocalDate effectiveFrom,LocalDate effectiveTo,String description){static TariffOptionResponse from(ServiceTariff t){return new TariffOptionResponse(t.getId(),t.getBuilding()==null?"Toàn chung cư":t.getBuilding().getName(),t.getUnitPrice(),t.getEffectiveFrom(),t.getEffectiveTo(),t.getDescription());}}
    public record ProfileResponse(Long unitId,String unitCode,String buildingName,BigDecimal areaM2,List<OccupantResponse> occupants,List<ServiceResponse> services,List<TariffOptionResponse> availableTariffs){}
}
