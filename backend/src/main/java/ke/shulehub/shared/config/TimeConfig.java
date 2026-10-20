package ke.shulehub.shared.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** School dates (terms, due dates, "today") follow East Africa Time. */
@Configuration
public class TimeConfig {

    public static final ZoneId NAIROBI = ZoneId.of("Africa/Nairobi");

    @Bean
    Clock clock() {
        return Clock.system(NAIROBI);
    }
}
