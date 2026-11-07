package pe.novamarket.order;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pe.novamarket.discount.DiscountCatalog;
import pe.novamarket.discount.DiscountPolicy;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orders;
    private final DiscountCatalog catalog;

    public OrderController(OrderService orders, DiscountCatalog catalog) {
        this.orders = orders;
        this.catalog = catalog;
    }

    @GetMapping("/politicas")
    public List<DiscountPolicy.Category> policies() {
        return catalog.categories();
    }

    @GetMapping("/pedidos")
    public List<Order> list() {
        return orders.list();
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public Order register(@RequestBody NewOrder request) {
        return orders.register(request.customer(), request.category(), request.amount());
    }

    @PostMapping("/pedidos/{id}/confirmar")
    public Order confirm(@PathVariable long id) {
        return orders.confirm(id);
    }

    public record NewOrder(String customer, String category, BigDecimal amount) {}
}
