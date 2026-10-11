package app.kerrlab.demo.orders;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the safety net: a bad override in demo-gitops fails startup, so the pod never becomes Ready and the previous
 * version keeps serving.
 */
class OrdersPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer()) // loads application.yaml
        .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
        .withUserConfiguration(PropertiesConfig.class);

    @Test
    void defaultsFromApplicationYamlBind() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            OrdersProperties properties = context.getBean(OrdersProperties.class);
            assertThat(properties.maxItemsPerOrder()).isEqualTo(10);
            assertThat(properties.catalog().baseUrl()).isEqualTo(URI.create("http://localhost:8090"));
            assertThat(properties.catalog().timeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(properties.payment().mode()).isEqualTo(OrdersProperties.Payment.Mode.SIMULATED);
            assertThat(properties.delivery()).isEqualTo(new OrdersProperties.Delivery(5, 1));
        });
    }

    @Test
    void unrealisticDeliveryEstimateFailsStartup() {
        runner.withPropertyValues("orders.delivery.standard-days=0")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void kubernetesDocumentSwitchesCatalogUrl() {
        runner.withPropertyValues("spring.main.cloud-platform=kubernetes")
            .run(context -> assertThat(context.getBean(OrdersProperties.class).catalog().baseUrl())
                .isEqualTo(URI.create("http://catalog:8080")));
    }

    @Test
    void misspelledKeyFailsStartup() {
        runner.withPropertyValues("orders.max-item-per-order=5")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void negativeTimeoutFailsStartup() {
        runner.withPropertyValues("orders.catalog.timeout=-1s")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void liveModeWithoutApiKeyFailsStartup() {
        runner.withPropertyValues("orders.payment.mode=live", "orders.payment.provider-url=https://api.payments.example")
            .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void liveModeWithUrlAndKeyStarts() {
        runner.withPropertyValues("orders.payment.mode=live", "orders.payment.provider-url=https://api.payments.example",
                "orders.payment.api-key=prod-key-3333")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OrdersProperties.class)
    static class PropertiesConfig {

    }
}
