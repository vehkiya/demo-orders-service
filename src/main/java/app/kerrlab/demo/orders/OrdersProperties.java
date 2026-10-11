package app.kerrlab.demo.orders;

import java.net.URI;
import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * All orders settings. Defaults live in {@code application.yaml}; per-environment overrides live in
 * {@code demo-gitops/envs/<env>/orders/application.yaml}; the payment API key comes from the secret store.
 * <p>
 * {@code ignoreUnknownFields = false} makes a misspelled key under {@code orders.*} fail startup instead of being
 * silently ignored.
 */
@Validated
@ConfigurationProperties(prefix = "orders", ignoreUnknownFields = false)
public record OrdersProperties(
        @Min(1) @Max(100) int maxItemsPerOrder,
        @Valid @NotNull Catalog catalog,
        @Valid @NotNull ExpressShipping expressShipping,
        @Valid @NotNull Payment payment) {

    public record Catalog(
            @NotNull URI baseUrl,
            @NotNull @DurationMin(millis = 100) @DurationMax(seconds = 10) Duration timeout,
            @Min(0) @Max(5) int maxRetries) {
    }

    public record ExpressShipping(boolean enabled) {
    }

    public record Payment(@NotNull Mode mode, URI providerUrl, String apiKey) {

        public enum Mode {
            /** In-process fake, needs no URL or key: the local default. */
            SIMULATED, SANDBOX, LIVE
        }

        @AssertTrue(message = "orders.payment.provider-url and orders.payment.api-key are required unless mode is simulated")
        public boolean isConfiguredForMode() {
            return mode != Mode.SANDBOX && mode != Mode.LIVE
                    || providerUrl != null && apiKey != null && !apiKey.isBlank();
        }
    }
}
