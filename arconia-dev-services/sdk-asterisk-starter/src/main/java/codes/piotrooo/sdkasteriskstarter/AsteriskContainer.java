package codes.piotrooo.sdkasteriskstarter;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class AsteriskContainer extends GenericContainer<AsteriskContainer> {
    public static final int SIP_PORT = 5060;

    public AsteriskContainer(AsteriskDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()));

        waitingFor(Wait.forLogMessage(".*Asterisk Ready\\..*\\n", 1));

        ContainerConfigurer.base(this, properties);

        addFixedExposedPort(properties.getPort(), SIP_PORT, InternetProtocol.UDP);
    }
}
