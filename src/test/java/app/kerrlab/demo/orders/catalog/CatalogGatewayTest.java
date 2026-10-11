package app.kerrlab.demo.orders.catalog;

import java.net.URI;
import java.time.Duration;

import app.kerrlab.demo.orders.OrdersProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatalogGatewayTest {

    private static final String PRODUCT_URL = "http://catalog:8080/api/products/BOOK-001";

    private MockRestServiceServer server;

    private CatalogGateway gateway;

    @BeforeEach
    void setUp() {
        OrdersProperties.Catalog catalog = new OrdersProperties.Catalog(URI.create("http://catalog:8080"),
                Duration.ofSeconds(1), 2);
        RestClient.Builder builder = CatalogClientConfig.restClientBuilder(catalog);
        server = MockRestServiceServer.bindTo(builder).build();
        OrdersProperties properties = new OrdersProperties(10, catalog, new OrdersProperties.ExpressShipping(false),
                new OrdersProperties.Delivery(5, 1),
                new OrdersProperties.Payment(OrdersProperties.Payment.Mode.SIMULATED, null, null));
        gateway = new CatalogGateway(CatalogClientConfig.client(builder.build()), properties);
    }

    @Test
    void returnsProduct() {
        server.expect(requestTo(PRODUCT_URL))
            .andRespond(withSuccess("""
                    {"sku":"BOOK-001","name":"Release It!","price":40.00,"discountedPrice":30.00,"currency":"EUR"}
                    """, MediaType.APPLICATION_JSON));

        assertThat(gateway.product("BOOK-001").discountedPrice()).isEqualByComparingTo("30.00");
    }

    @Test
    void notFoundIsUnknownSkuAndNotRetried() {
        server.expect(ExpectedCount.once(), requestTo(PRODUCT_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatExceptionOfType(ErrorResponseException.class).isThrownBy(() -> gateway.product("BOOK-001"))
            .satisfies(ex -> assertThat(ex.getBody().getType()).hasToString("/problems/unknown-sku"));
        server.verify();
    }

    @Test
    void serverErrorsAreRetriedThenCatalogUnavailable() {
        // max-retries = 2 → 3 attempts in total
        server.expect(ExpectedCount.times(3), requestTo(PRODUCT_URL)).andRespond(withServerError());

        assertThatExceptionOfType(ErrorResponseException.class).isThrownBy(() -> gateway.product("BOOK-001"))
            .satisfies(ex -> {
                assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(ex.getBody().getType()).hasToString("/problems/catalog-unavailable");
                assertThat(ex.getBody().getDetail()).contains("3 attempt(s)");
            });
        server.verify();
    }
}
