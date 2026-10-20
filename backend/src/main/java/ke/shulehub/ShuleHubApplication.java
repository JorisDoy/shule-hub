package ke.shulehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Shule Hub: a modular monolith. Each top-level package below {@code ke.shulehub} is a
 * Spring Modulith application module; {@code ModularityTests} verifies their boundaries.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class ShuleHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShuleHubApplication.class, args);
    }
}
