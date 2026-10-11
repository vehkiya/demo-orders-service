package app.kerrlab.demo.orders;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import app.kerrlab.demo.orders.catalog.CatalogGateway;
import app.kerrlab.demo.orders.catalog.CatalogProduct;
import app.kerrlab.demo.orders.payment.PaymentResult;
import app.kerrlab.demo.orders.payment.SimulatedPaymentGateway;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrdersProperties properties;

    private final CatalogGateway catalog;

    private final SimulatedPaymentGateway payments;

    private final Clock clock;

    /** In memory and per pod: good enough for the demo. */
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public OrderService(OrdersProperties properties, CatalogGateway catalog, SimulatedPaymentGateway payments,
            Clock clock) {
        this.properties = properties;
        this.catalog = catalog;
        this.payments = payments;
        this.clock = clock;
    }

    public Order place(OrderRequest request) {
        int quantity = request.items().stream().mapToInt(OrderRequest.Item::quantity).sum();
        if (quantity > properties.maxItemsPerOrder()) {
            throw Problems.of(HttpStatus.BAD_REQUEST, "too-many-items", "Too many items",
                    "An order can contain at most %d items, got %d".formatted(properties.maxItemsPerOrder(), quantity));
        }
        if (request.express() && !properties.expressShipping().enabled()) {
            throw Problems.of(HttpStatus.BAD_REQUEST, "express-unavailable", "Express shipping unavailable",
                    "Express shipping is not available");
        }
        List<CatalogProduct> products = request.items().stream().map(item -> catalog.product(item.sku())).toList();
        List<Order.Line> lines = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            lines.add(line(products.get(i), request.items().get(i).quantity()));
        }
        BigDecimal total = lines.stream().map(Order.Line::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = products.getFirst().currency();
        PaymentResult payment = payments.charge(total, currency);
        String id = "ord-" + UUID.randomUUID().toString().substring(0, 8);
        Order order = new Order(id, List.copyOf(lines), total, currency, request.express(),
                estimatedDelivery(request.express()), payment, Instant.now(clock));
        orders.put(order.id(), order);
        return order;
    }

    public Optional<Order> find(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    public Collection<Order> all() {
        return orders.values();
    }

    private LocalDate estimatedDelivery(boolean express) {
        OrdersProperties.Delivery delivery = properties.delivery();
        return LocalDate.now(clock).plusDays(express ? delivery.expressDays() : delivery.standardDays());
    }

    private static Order.Line line(CatalogProduct product, int quantity) {
        BigDecimal unitPrice = (product.discountedPrice() != null) ? product.discountedPrice() : product.price();
        return new Order.Line(product.sku(), product.name(), quantity, unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }
}
