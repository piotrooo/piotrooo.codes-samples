package codes.piotrooo.azurefunctions.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class UppercaseFunctionTest {
    private final UppercaseFunction uppercaseFunction = new UppercaseFunction();

    private final Locale originalLocale = Locale.getDefault();

    @AfterEach
    void restoreDefaultLocale() {
        Locale.setDefault(originalLocale);
    }

    @Test
    void shouldUppercaseMessage() {
        // when
        String result = uppercaseFunction.apply("hello");

        // then
        assertThat(result).isEqualTo("HELLO");
    }

    @Test
    void shouldUppercaseDefaultMessageWhenMessageIsBlank() {
        // when
        String result = uppercaseFunction.apply("   ");

        // then
        assertThat(result).isEqualTo("DEFAULT");
    }

    @Test
    void shouldUppercaseIndependentlyOfDefaultLocale() {
        // given
        // in Turkish locale 'i' uppercases to 'İ' (U+0130) instead of 'I'
        Locale.setDefault(Locale.forLanguageTag("tr"));

        // when
        String result = uppercaseFunction.apply("istanbul");

        // then
        assertThat(result).isEqualTo("ISTANBUL");
    }
}
