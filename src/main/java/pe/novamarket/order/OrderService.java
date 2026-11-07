package pe.novamarket.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import pe.novamarket.discount.DiscountCatalog;
import pe.novamarket.discount.DiscountPolicy;
import pe.novamarket.event.ConfirmationPublisher;
import pe.novamarket.event.OrderConfirmed;

@Service
public class OrderService {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999.99");
    private final DiscountCatalog catalog;
    private final ConfirmationPublisher publisher;
    private final Map<Long, Order> orders = new LinkedHashMap<>();
    private long nextId = 1;

    public OrderService(DiscountCatalog catalog, ConfirmationPublisher publisher) {
        this.catalog = catalog;
        this.publisher = publisher;
    }

    public synchronized Order register(String customer, String category, BigDecimal amount) {
        String name = customer == null ? "" : customer.strip();
        if (name.isBlank() || name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente debe tener entre 1 y 100 caracteres.");
        }
        if (amount == null || amount.signum() <= 0 || amount.compareTo(MAX_AMOUNT) > 0
                || amount.stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ingresa un importe de S/ 0.01 a S/ 999 999.99 con hasta dos decimales.");
        }
        DiscountPolicy policy = catalog.find(category);
        if (policy == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una categoría de cliente válida.");
        }

        BigDecimal base = amount.setScale(2);
        BigDecimal discount = policy.discount(base).setScale(2, RoundingMode.HALF_UP);
        if (discount.signum() < 0 || discount.compareTo(base) > 0) {
            throw new IllegalStateException("La política produjo un descuento fuera del importe del pedido.");
        }
        Order order = new Order(nextId++, name, policy.category(), base, discount, base.subtract(discount),
                Instant.now(), Order.Status.REGISTRADO, null, List.of());
        orders.put(order.id(), order);
        return order;
    }

    public synchronized List<Order> list() {
        List<Order> newestFirst = new ArrayList<>(orders.values());
        Collections.reverse(newestFirst);
        return List.copyOf(newestFirst);
    }

    public synchronized Order confirm(long id) {
        Order order = orders.get(id);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el pedido solicitado.");
        }
        if (order.status() == Order.Status.CONFIRMADO) {
            return order;
        }
        OrderConfirmed event = new OrderConfirmed(UUID.randomUUID(), id, order.customer(), order.total(), Instant.now());
        Order confirmed = order.confirmed(event, publisher.publish(event));
        orders.put(id, confirmed);
        return confirmed;
    }
}
