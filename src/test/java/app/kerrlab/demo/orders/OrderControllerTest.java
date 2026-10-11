package app.kerrlab.demo.orders;

import java.math.BigDecimal;

import app.kerrlab.demo.orders.catalog.CatalogGateway;
import app.kerrlab.demo.orders.catalog.CatalogProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "orders.max-items-per-order=3")
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CatalogGateway catalog;

    @BeforeEach
    void catalogKnowsTwoProducts() {
        given(catalog.product("BOOK-001")).willReturn(new CatalogProduct("BOOK-001", "Release It!",
                new BigDecimal("40.00"), new BigDecimal("30.00"), "EUR"));
        given(catalog.product("MUG-001")).willReturn(new CatalogProduct("MUG-001", "GitOps mug",
                new BigDecimal("12.50"), new BigDecimal("9.38"), "EUR"));
    }

    @Test
    void placesOrderUsingDiscountedPrices() throws Exception {
        order("""
                {"items":[{"sku":"BOOK-001","quantity":2},{"sku":"MUG-001","quantity":1}]}
                """)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.total").value(69.38))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.estimatedDelivery").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}")))
            .andExpect(jsonPath("$.payment.mode").value("simulated"));
    }

    @Test
    void tooManyItemsIsProblemDetailWithLimit() throws Exception {
        order("""
                {"items":[{"sku":"BOOK-001","quantity":4}]}
                """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("/problems/too-many-items"))
            .andExpect(jsonPath("$.detail").value("An order can contain at most 3 items, got 4"));
    }

    @Test
    void expressRejectedWhenFlagDisabled() throws Exception {
        order("""
                {"items":[{"sku":"BOOK-001","quantity":1}],"express":true}
                """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("/problems/express-unavailable"));
    }

    @Test
    void invalidRequestIsRejected() throws Exception {
        order("""
                {"items":[]}
                """)
            .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions order(String json) throws Exception {
        return mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
