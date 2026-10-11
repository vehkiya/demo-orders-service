package app.kerrlab.demo.orders.catalog;

import java.math.BigDecimal;

public record CatalogProduct(String sku, String name, BigDecimal price, BigDecimal discountedPrice, String currency) {
}
