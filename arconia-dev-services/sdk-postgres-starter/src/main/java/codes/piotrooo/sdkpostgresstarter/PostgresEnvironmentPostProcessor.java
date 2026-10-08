package codes.piotrooo.sdkpostgresstarter;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class PostgresEnvironmentPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> conf = Map.of(
                "arconia.dev.services.postgresql.image-name", "postgres:17"
        );
        environment.getPropertySources().addLast(new MapPropertySource("postgresDevServicesSdk", conf));
    }
}
