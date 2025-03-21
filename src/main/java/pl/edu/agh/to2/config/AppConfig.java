package pl.edu.agh.to2.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.time.Clock;

@Configuration
@ComponentScan("pl.edu.agh.to2")
@PropertySource("classpath:application.properties")
@PropertySource("classpath:./.env")
public class AppConfig {
    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure().ignoreIfMissing().load();
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
