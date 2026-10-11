package app.kerrlab.demo.orders.catalog;

import java.time.Duration;

import app.kerrlab.demo.orders.OrdersProperties;
import app.kerrlab.demo.orders.Problems;

import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Wraps {@link CatalogClient} with retries (Spring Framework 7 core retry) and maps failures to problem responses.
 * Only timeouts / connection errors and 5xx are retried; 4xx never are.
 */
@Component
public class CatalogGateway {

    private final CatalogClient client;

    private final RetryTemplate retryTemplate;

    public CatalogGateway(CatalogClient client, OrdersProperties properties) {
        this.client = client;
        this.retryTemplate = new RetryTemplate(RetryPolicy.builder()
            .maxRetries(properties.catalog().maxRetries())
            .delay(Duration.ofMillis(200))
            .predicate(CatalogGateway::isRetryable)
            .build());
    }

    public CatalogProduct product(String sku) {
        try {
            return retryTemplate.execute(() -> client.product(sku));
        }
        catch (RetryException ex) {
            if (ex.getLastException() instanceof HttpClientErrorException.NotFound) {
                throw Problems.of(HttpStatus.NOT_FOUND, "unknown-sku", "Unknown SKU", "No product with SKU " + sku);
            }
            throw Problems.of(HttpStatus.SERVICE_UNAVAILABLE, "catalog-unavailable", "Catalog unavailable",
                    "Catalog did not answer after %d attempt(s)".formatted(ex.getRetryCount() + 1),
                    ex.getLastException());
        }
    }

    private static boolean isRetryable(Throwable ex) {
        return ex instanceof ResourceAccessException || ex instanceof HttpServerErrorException;
    }
}
