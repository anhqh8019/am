package vn.com.apartment.tariff;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.com.apartment.building.Building;
import vn.com.apartment.building.BuildingRepository;
import vn.com.apartment.unitprofile.ChargeMethod;

@RestController
@RequestMapping("/api/service-tariffs")
public class ServiceTariffController {
    private static final String SERVICE_FEE = "SERVICE_FEE";
    private final ServiceTariffRepository tariffs;
    private final BuildingRepository buildings;

    public ServiceTariffController(ServiceTariffRepository tariffs, BuildingRepository buildings) {
        this.tariffs = tariffs;
        this.buildings = buildings;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<TariffResponse> list() {
        return tariffs.findAllWithBuilding().stream().map(TariffResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public TariffResponse create(@Valid @RequestBody TariffRequest request) {
        Building building = request.buildingId() == null ? null : buildings.findById(request.buildingId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tòa nhà."));

        Long buildingId = building == null ? null : building.getId();
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Ngày kết thúc không được trước ngày bắt đầu.");
        }

        for (ServiceTariff current : tariffs.findForScope(SERVICE_FEE, buildingId)) {
            if (!overlaps(current.getEffectiveFrom(), current.getEffectiveTo(),
                          request.effectiveFrom(), request.effectiveTo())) continue;
            if (current.getEffectiveTo() == null && current.getEffectiveFrom().isBefore(request.effectiveFrom())) {
                current.close(request.effectiveFrom().minusDays(1));
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khoảng thời gian này bị trùng với một biểu phí đã có trong cùng phạm vi.");
            }
        }

        ServiceTariff tariff = new ServiceTariff(
            SERVICE_FEE, building, request.unitPrice(), ChargeMethod.BY_AREA,
            request.effectiveFrom(), request.effectiveTo(), trim(request.description()));
        return TariffResponse.from(tariffs.save(tariff));
    }

    private boolean overlaps(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return (end1 == null || !end1.isBefore(start2)) && (end2 == null || !end2.isBefore(start1));
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record TariffRequest(Long buildingId,
                                @NotNull @DecimalMin("0") BigDecimal unitPrice,
                                @NotNull LocalDate effectiveFrom,
                                LocalDate effectiveTo,
                                @Size(max = 500) String description) {}

    public record TariffResponse(Long id, String serviceCode, Long buildingId, String buildingName,
                                 BigDecimal unitPrice, ChargeMethod calculationType,
                                 LocalDate effectiveFrom, LocalDate effectiveTo,
                                 String description, boolean active) {
        static TariffResponse from(ServiceTariff tariff) {
            Building building = tariff.getBuilding();
            return new TariffResponse(tariff.getId(), tariff.getServiceCode(),
                building == null ? null : building.getId(),
                building == null ? "Toàn chung cư" : building.getName(),
                tariff.getUnitPrice(), tariff.getCalculationType(), tariff.getEffectiveFrom(),
                tariff.getEffectiveTo(), tariff.getDescription(), tariff.isActive());
        }
    }
}
