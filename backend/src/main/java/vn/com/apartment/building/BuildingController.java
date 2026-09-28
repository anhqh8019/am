package vn.com.apartment.building;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {
    private final BuildingRepository repository;
    BuildingController(BuildingRepository repository) { this.repository = repository; }

    @GetMapping
    List<BuildingResponse> list() {
        return repository.findByActiveTrueOrderByCode().stream()
            .map(b -> new BuildingResponse(b.getId(), b.getCode(), b.getName()))
            .toList();
    }

    record BuildingResponse(Long id, String code, String name) {}
}
