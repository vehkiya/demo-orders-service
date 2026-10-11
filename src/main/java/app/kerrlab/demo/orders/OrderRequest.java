package app.kerrlab.demo.orders;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

/**
 * {@code express} is optional in the JSON (Jackson 3 rejects a missing primitive), so it is a {@link Boolean} that
 * defaults to {@code false}.
 */
public record OrderRequest(@NotEmpty List<@Valid Item> items, Boolean express) {

    public OrderRequest {
        express = Boolean.TRUE.equals(express);
    }

    public record Item(@NotBlank String sku, @Positive @Max(1000) int quantity) {
    }
}
