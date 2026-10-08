package codes.piotrooo.sdkasteriskstarter;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled(name = "asterisk", prefix = "codes.piotrooo")
@EnableConfigurationProperties(AsteriskDevServicesProperties.class)
@Import(AsteriskDevServiceAutoConfiguration.AsteriskDevServiceRegistrar.class)
public class AsteriskDevServiceAutoConfiguration {
    static class AsteriskDevServiceRegistrar extends DevServicesRegistrar {
        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            AsteriskDevServicesProperties properties = bindProperties("codes.piotrooo.asterisk", AsteriskDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("asterisk")
                    .description("Asterisk Dev Service")
                    .container(container -> container
                            .type(AsteriskContainer.class)
                            .supplier(() -> new AsteriskContainer(properties))
                            .serviceConnectionName(null)
                    ));
        }

    }
}
