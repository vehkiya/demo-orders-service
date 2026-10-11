package app.kerrlab.demo.orders;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import app.kerrlab.demo.orders.catalog.CatalogGateway;
import app.kerrlab.demo.orders.catalog.CatalogProduct;
import app.kerrlab.demo.orders.payment.SimulatedPaymentGateway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class OrderServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-12T09:00:00Z"), ZoneOffset.UTC);

    private final CatalogGateway catalog = mock(CatalogGateway.class);

    @Test
    void standardDeliveryUsesStandardDays() {
        Order order = service(true).place(request(false));

        assertThat(order.estimatedDelivery()).isEqualTo(LocalDate.of(2026, 10, 17));
    }

    @Test
    void expressDeliveryUsesExpressDays() {
        Order order = service(true).place(request(true));

        assertThat(order.estimatedDelivery()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(order.total()).isEqualByComparingTo("60.00");
    }

    private OrderService service(boolean expressEnabled) {
        given(catalog.product(anyString())).willReturn(new CatalogProduct("BOOK-001", "Release It!",
                new BigDecimal("40.00"), new BigDecimal("30.00"), "EUR"));
        OrdersProperties properties = new OrdersProperties(10,
                new OrdersProperties.Catalog(URI.create("http://catalog:8080"), Duration.ofSeconds(1), 0),
                new OrdersProperties.ExpressShipping(expressEnabled), new OrdersProperties.Delivery(5, 1),
                new OrdersProperties.Payment(OrdersProperties.Payment.Mode.SIMULATED, null, null));
        return new OrderService(properties, catalog, new SimulatedPaymentGateway(properties), CLOCK);
    }

    private static OrderRequest request(boolean express) {
        return new OrderRequest(List.of(new OrderRequest.Item("BOOK-001", 2)), express);
    }
}
