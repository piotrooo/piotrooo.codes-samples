package codes.piotrooo.azurefunctions.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.function.Function;

import static org.springframework.util.StringUtils.hasText;

@Component
public class UppercaseFunction implements Function<String, String> {
    private static final String DEFAULT_MESSAGE = "default";

    @Override
    public String apply(String message) {
        String payload = hasText(message) ? message : DEFAULT_MESSAGE;
        return payload.toUpperCase(Locale.ROOT);
    }
}
