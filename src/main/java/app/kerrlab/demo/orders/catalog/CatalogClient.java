package app.kerrlab.demo.orders.catalog;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

/** Declarative HTTP client for demo-catalog-service. */
public interface CatalogClient {

    @GetExchange("/api/products/{sku}")
    CatalogProduct product(@PathVariable String sku);
}
