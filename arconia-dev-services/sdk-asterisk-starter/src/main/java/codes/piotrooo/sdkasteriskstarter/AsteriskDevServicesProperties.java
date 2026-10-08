package codes.piotrooo.sdkasteriskstarter;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codes.piotrooo.asterisk")
public class AsteriskDevServicesProperties implements BaseDevServicesProperties {
    private boolean enabled = true;

    private String imageName = "andrius/asterisk:22";

    private int port = 0;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public AsteriskDevServicesProperties setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public String getImageName() {
        return imageName;
    }

    public AsteriskDevServicesProperties setImageName(String imageName) {
        this.imageName = imageName;
        return this;
    }

    @Override
    public int getPort() {
        return port;
    }

    public AsteriskDevServicesProperties setPort(int port) {
        this.port = port;
        return this;
    }
}
