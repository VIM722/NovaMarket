package pe.novamarket.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import pe.novamarket.discount.DiscountCatalog;
import pe.novamarket.discount.DiscountPolicy;
import pe.novamarket.discount.FrequentCustomerDiscount;
import pe.novamarket.discount.NewCustomerDiscount;
import pe.novamarket.discount.UnidentifiedCustomerDiscount;
import pe.novamarket.event.ConfirmationPublisher;
import pe.novamarket.event.OrderConfirmed;
import pe.novamarket.event.OrderObserver;

class OrderServiceTest {
    private final NewCustomerDiscount newDiscount = new NewCustomerDiscount();
    private final FrequentCustomerDiscount frequentDiscount = new FrequentCustomerDiscount();
    private final UnidentifiedCustomerDiscount unidentifiedDiscount = new UnidentifiedCustomerDiscount();

    private OrderService service(List<OrderObserver> observers) {
        return new OrderService(new DiscountCatalog(List.of(newDiscount, frequentDiscount, unidentifiedDiscount)),
                new ConfirmationPublisher(observers));
    }

    @ParameterizedTest
    @CsvSource({
        "NUEVO,200,10.00,190.00",
        "FRECUENTE,200,20.00,180.00",
        "NO_IDENTIFICADO,200,0.00,200.00",
        "NUEVO,0.10,0.01,0.09"
    })
    void calculatesByCategory(String category, String amount, String discount, String total) {
        Order order = service(List.of()).register("Ana", category, new BigDecimal(amount));

        assertEquals(new BigDecimal(discount), order.discount());
        assertEquals(new BigDecimal(total), order.total());
        assertEquals(Order.Status.REGISTRADO, order.status());
        assertTrue(order.reactions().isEmpty());
    }

    @Test
    void acceptsAnAdditionalPolicyWithoutChangingExistingPolicies() {
        DiscountPolicy premium = new DiscountPolicy() {
            @Override
            public Category category() {
                return new Category("PREMIUM", "Cliente prémium", "15 %");
            }

            @Override
            public BigDecimal discount(BigDecimal amount) {
                return amount.multiply(new BigDecimal("0.15"));
            }
        };
        DiscountCatalog catalog = new DiscountCatalog(List.of(newDiscount, frequentDiscount, unidentifiedDiscount, premium));
        OrderService orders = new OrderService(catalog, new ConfirmationPublisher(List.of()));

        assertEquals(new BigDecimal("170.00"), orders.register("Ana", "PREMIUM", new BigDecimal("200")).total());
        assertEquals(new BigDecimal("190.00"), orders.register("Ana", "NUEVO", new BigDecimal("200")).total());
        assertTrue(catalog.categories().stream().anyMatch(category -> category.code().equals("PREMIUM")));
    }

    @Test
    void validatesInputAndMissingOrders() {
        OrderService orders = service(List.of());
        BigDecimal amount = new BigDecimal("200");
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> orders.register(" ", "NUEVO", amount)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> orders.register("Ana", "NO_EXISTE", amount)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> orders.register("Ana", "NUEVO", new BigDecimal("1.005"))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> orders.register("Ana", "NUEVO", BigDecimal.ZERO)).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> orders.confirm(999)).getStatusCode());
        assertTrue(orders.list().isEmpty());
    }

    @Test
    void confirmsOnlyOnceAcrossConcurrentRequests() throws Exception {
        AtomicInteger deliveries = new AtomicInteger();
        OrderObserver observer = new OrderObserver() {
            @Override
            public String area() {
                return "Auditoría";
            }

            @Override
            public String onConfirmed(OrderConfirmed event) {
                deliveries.incrementAndGet();
                return "Evento recibido.";
            }
        };
        OrderService orders = service(List.of(observer));
        long id = orders.register("Ana", "NUEVO", new BigDecimal("200")).id();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            Future<Order> first = pool.submit(() -> orders.confirm(id));
            Future<Order> second = pool.submit(() -> orders.confirm(id));
            assertSame(first.get(), second.get());
            assertEquals(1, deliveries.get());
            assertEquals(Order.Status.CONFIRMADO, orders.list().get(0).status());
            assertEquals("Auditoría", orders.list().get(0).reactions().get(0).area());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void recordsAFailingObserverAndContinuesDelivery() {
        OrderObserver failing = new OrderObserver() {
            @Override
            public String area() {
                return "Módulo con error";
            }

            @Override
            public String onConfirmed(OrderConfirmed event) {
                throw new IllegalStateException("Fallo simulado");
            }
        };
        OrderObserver working = new OrderObserver() {
            @Override
            public String area() {
                return "Auditoría";
            }

            @Override
            public String onConfirmed(OrderConfirmed event) {
                return "Evento recibido.";
            }
        };
        OrderService orders = service(List.of(failing, working));
        long id = orders.register("Ana", "NUEVO", new BigDecimal("200")).id();

        Order confirmed = orders.confirm(id);

        assertEquals(Order.Status.CONFIRMADO, confirmed.status());
        assertFalse(confirmed.reactions().get(0).successful());
        assertTrue(confirmed.reactions().get(1).successful());
        assertSame(confirmed, orders.confirm(id));
    }
}
