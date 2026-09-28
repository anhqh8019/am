package vn.com.apartment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ApartmentManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApartmentManagementApplication.class, args);
    }
}
