package vn.com.apartment.resident;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.com.apartment.building.*;

@RestController @RequestMapping("/api/residents")
public class ResidentController {
    private final CustomerRepository customers; private final UnitOccupancyRepository occupancies; private final UnitRepository units;
    public ResidentController(CustomerRepository customers,UnitOccupancyRepository occupancies,UnitRepository units){this.customers=customers;this.occupancies=occupancies;this.units=units;}

    @GetMapping @Transactional(readOnly=true)
    public List<ResidentResponse> list(@RequestParam(required=false) Long buildingId,@RequestParam(required=false) String keyword){
        String q=keyword==null||keyword.isBlank()?null:keyword.trim(); List<Customer> found=customers.search(buildingId,q);
        if(found.isEmpty()) return List.of();
        Map<Long,List<OccupancyResponse>> byCustomer=new HashMap<>();
        occupancies.findDetailedByCustomerIds(found.stream().map(Customer::getId).toList()).forEach(o->byCustomer.computeIfAbsent(o.getCustomer().getId(),k->new ArrayList<>()).add(OccupancyResponse.from(o)));
        return found.stream().map(c->ResidentResponse.from(c,byCustomer.getOrDefault(c.getId(),List.of()))).toList();
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    public ResidentResponse create(@Valid @RequestBody ResidentRequest r){
        String code=normalize(r.customerCode());if(customers.existsByCustomerCodeIgnoreCase(code)) conflict("Mã khách hàng đã tồn tại.");
        String nationalId=trim(r.nationalId());validateNationalId(nationalId,null);
        Customer c=customers.save(new Customer(code,r.customerType(),r.fullName().trim(),trim(r.phone()),trim(r.email()),trim(r.taxCode()),r.birthYear(),r.gender(),nationalId));
        return ResidentResponse.from(c,List.of());
    }

    @PutMapping("/{id}") @Transactional
    public ResidentResponse update(@PathVariable Long id,@Valid @RequestBody ResidentRequest r){
        Customer c=customer(id);String code=normalize(r.customerCode());
        if(customers.existsByCustomerCodeIgnoreCaseAndIdNot(code,id)) conflict("Mã khách hàng đã tồn tại.");
        String nationalId=trim(r.nationalId());validateNationalId(nationalId,id);
        c.update(code,r.customerType(),r.fullName().trim(),trim(r.phone()),trim(r.email()),trim(r.taxCode()),r.birthYear(),r.gender(),nationalId,r.active());
        List<OccupancyResponse> links=occupancies.findDetailedByCustomerIds(List.of(id)).stream().map(OccupancyResponse::from).toList();
        return ResidentResponse.from(c,links);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void deactivate(@PathVariable Long id){customer(id).deactivate();}

    @PostMapping("/{customerId}/occupancies") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public OccupancyResponse assign(@PathVariable Long customerId,@Valid @RequestBody OccupancyRequest r){
        Customer c=customer(customerId);Unit u=units.findById(r.unitId()).filter(x->x.getStatus()==UnitStatus.ACTIVE).orElseThrow(()->notFound("Không tìm thấy căn hộ đang hoạt động."));
        if(r.occupancyRole()==OccupancyRole.OWNER) validateOwnerProfile(c);
        if(r.endDate()!=null&&r.endDate().isBefore(r.startDate())) bad("Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        if(occupancies.existsByUnitIdAndCustomerIdAndEndDateIsNull(u.getId(),c.getId())) conflict("Khách hàng đang được liên kết với căn hộ này.");
        if(r.primary()) occupancies.findActivePrimaryByUnitId(u.getId()).forEach(UnitOccupancy::removePrimary);
        return OccupancyResponse.from(occupancies.save(new UnitOccupancy(u,c,r.occupancyRole(),r.startDate(),r.endDate(),r.primary())));
    }

    @DeleteMapping("/occupancies/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void endOccupancy(@PathVariable Long id,@RequestParam(required=false) LocalDate endDate){
        UnitOccupancy o=occupancies.findById(id).orElseThrow(()->notFound("Không tìm thấy liên kết căn hộ."));
        LocalDate date=endDate==null?LocalDate.now():endDate;if(date.isBefore(o.getStartDate())) bad("Ngày kết thúc không được trước ngày bắt đầu.");o.end(date);
    }

    private Customer customer(Long id){return customers.findById(id).orElseThrow(()->notFound("Không tìm thấy khách hàng."));}
    private void validateNationalId(String nationalId,Long currentId){
        if(nationalId==null)return;
        if(!nationalId.matches("\\d{12}"))bad("Căn cước công dân phải gồm đúng 12 chữ số.");
        boolean exists=currentId==null?customers.existsByNationalId(nationalId):customers.existsByNationalIdAndIdNot(nationalId,currentId);
        if(exists)conflict("Căn cước công dân đã được sử dụng cho khách hàng khác.");
    }
    private void validateOwnerProfile(Customer c){
        if(c.getPhone()==null||c.getPhone().isBlank())bad("Chủ sở hữu bắt buộc phải có số điện thoại.");
        if(c.getCustomerType()==CustomerType.INDIVIDUAL){
            if(c.getBirthYear()==null)bad("Chủ hộ bắt buộc phải có năm sinh.");
            if(c.getGender()==null)bad("Chủ hộ bắt buộc phải có giới tính.");
            if(c.getNationalId()==null||c.getNationalId().isBlank())bad("Chủ hộ bắt buộc phải có căn cước công dân.");
        }else if(c.getTaxCode()==null||c.getTaxCode().isBlank())bad("Tổ chức sở hữu căn hộ bắt buộc phải có mã số thuế.");
    }
    private String normalize(String v){return v.trim().toUpperCase();} private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);} private void conflict(String m){throw new ResponseStatusException(HttpStatus.CONFLICT,m);} private void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}

    public record ResidentRequest(@NotBlank @Size(max=30) String customerCode,@NotNull CustomerType customerType,@NotBlank @Size(max=200) String fullName,
        @Size(max=30) String phone,@Email @Size(max=150) String email,@Size(max=30) String taxCode,
        @Min(1900) @Max(2100) Integer birthYear,Gender gender,@Size(max=20) String nationalId,boolean active){}
    public record OccupancyRequest(@NotNull Long unitId,@NotNull OccupancyRole occupancyRole,@NotNull LocalDate startDate,LocalDate endDate,boolean primary){}
    public record OccupancyResponse(Long id,Long unitId,String unitCode,Long buildingId,String buildingName,OccupancyRole occupancyRole,LocalDate startDate,LocalDate endDate,boolean primary){
        static OccupancyResponse from(UnitOccupancy o){Unit u=o.getUnit();return new OccupancyResponse(o.getId(),u.getId(),u.getCode(),u.getBuilding().getId(),u.getBuilding().getName(),o.getOccupancyRole(),o.getStartDate(),o.getEndDate(),o.isPrimary());}}
    public record ResidentResponse(Long id,String customerCode,CustomerType customerType,String fullName,String phone,String email,String taxCode,Integer birthYear,Gender gender,String nationalId,boolean active,List<OccupancyResponse> occupancies){
        static ResidentResponse from(Customer c,List<OccupancyResponse> links){return new ResidentResponse(c.getId(),c.getCustomerCode(),c.getCustomerType(),c.getFullName(),c.getPhone(),c.getEmail(),c.getTaxCode(),c.getBirthYear(),c.getGender(),c.getNationalId(),c.isActive(),links);}}
}
