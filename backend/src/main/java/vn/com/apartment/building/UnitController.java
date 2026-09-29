package vn.com.apartment.building;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/units")
public class UnitController {
    private final UnitRepository unitRepository;
    private final BuildingRepository buildingRepository;

    UnitController(UnitRepository unitRepository, BuildingRepository buildingRepository) {
        this.unitRepository = unitRepository;
        this.buildingRepository = buildingRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    List<UnitResponse> list(@RequestParam(required = false) Long buildingId,
                            @RequestParam(required = false) String keyword) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return unitRepository.search(buildingId, normalizedKeyword).stream().map(UnitResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    UnitResponse create(@Valid @RequestBody UnitRequest request) {
        String code = normalizeCode(request.code());
        Building building = findBuilding(request.buildingId());
        if (unitRepository.existsByBuildingIdAndCodeIgnoreCase(building.getId(), code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã căn hộ đã tồn tại trong tòa nhà.");
        }
        Unit unit = new Unit(building, code, UnitType.APARTMENT, request.areaM2());
        return UnitResponse.from(unitRepository.save(unit));
    }

    @PutMapping("/{id}")
    @Transactional
    UnitResponse update(@PathVariable Long id, @Valid @RequestBody UnitRequest request) {
        Unit unit = findUnit(id);
        Building building = findBuilding(request.buildingId());
        String code = normalizeCode(request.code());
        if (unitRepository.existsByBuildingIdAndCodeIgnoreCaseAndIdNot(building.getId(), code, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã căn hộ đã tồn tại trong tòa nhà.");
        }
        unit.update(building, code, UnitType.APARTMENT, request.areaM2(), request.status());
        return UnitResponse.from(unit);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void deactivate(@PathVariable Long id) { findUnit(id).deactivate(); }

    private Building findBuilding(Long id) {
        return buildingRepository.findById(id)
            .filter(Building::isActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tòa nhà."));
    }

    private Unit findUnit(Long id) {
        return unitRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy căn hộ."));
    }

    private String normalizeCode(String code) { return code.trim().toUpperCase(); }

    record UnitRequest(
        @NotNull Long buildingId,
        @NotBlank @Size(max = 30) String code,
        @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal areaM2,
        @NotNull UnitStatus status
    ) {}

    record UnitResponse(Long id, Long buildingId, String buildingCode, String buildingName,
                        String code, UnitType unitType, BigDecimal areaM2, UnitStatus status) {
        static UnitResponse from(Unit unit) {
            Building building = unit.getBuilding();
            return new UnitResponse(unit.getId(), building.getId(), building.getCode(), building.getName(),
                unit.getCode(), unit.getUnitType(), unit.getAreaM2(), unit.getStatus());
        }
    }
}
