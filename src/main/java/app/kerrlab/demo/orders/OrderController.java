package app.kerrlab.demo.orders;

import java.util.Collection;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Order place(@Valid @RequestBody OrderRequest request) {
        return orderService.place(request);
    }

    @GetMapping
    Collection<Order> all() {
        return orderService.all();
    }

    @GetMapping("/{id}")
    Order find(@PathVariable String id) {
        return orderService.find(id)
            .orElseThrow(() -> Problems.of(HttpStatus.NOT_FOUND, "unknown-order", "Unknown order",
                    "No order with id " + id + " on this instance"));
    }
}
