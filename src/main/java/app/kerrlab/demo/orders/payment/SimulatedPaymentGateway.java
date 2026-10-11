package app.kerrlab.demo.orders.payment;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import app.kerrlab.demo.orders.OrdersProperties;
import app.kerrlab.demo.orders.config.Secrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * No real HTTP call: logs which provider and which key would be used. That is enough to show that the right URL and
 * the right secret reached the right environment.
 */
@Component
public class SimulatedPaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentGateway.class);

    private final OrdersProperties.Payment payment;

    public SimulatedPaymentGateway(OrdersProperties properties) {
        this.payment = properties.payment();
    }

    public PaymentResult charge(BigDecimal amount, String currency) {
        String mode = payment.mode().name().toLowerCase(Locale.ROOT);
        log.info("Charging {} {} via {} ({}) key={}", amount, currency, mode,
                (payment.providerUrl() != null) ? payment.providerUrl() : "in-process",
                Secrets.fingerprint(payment.apiKey()));
        return new PaymentResult(mode, "PAY-" + UUID.randomUUID().toString().substring(0, 8));
    }
}
