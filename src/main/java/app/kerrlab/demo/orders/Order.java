package app.kerrlab.demo.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import app.kerrlab.demo.orders.payment.PaymentResult;

public record Order(String id, List<Line> items, BigDecimal total, String currency, boolean express,
        LocalDate estimatedDelivery, PaymentResult payment, Instant createdAt) {

    public record Line(String sku, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }
}
