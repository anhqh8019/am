package vn.com.apartment.health;

import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping
    Map<String, Object> health() {
        return Map.of("status", "UP", "service", "apartment-management-api", "time", OffsetDateTime.now());
    }
}
