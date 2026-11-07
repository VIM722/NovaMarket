package pe.novamarket.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import pe.novamarket.discount.DiscountPolicy;
import pe.novamarket.event.ConfirmationPublisher.Reaction;
import pe.novamarket.event.OrderConfirmed;

public record Order(long id, String customer, DiscountPolicy.Category category, BigDecimal amount,
                    BigDecimal discount, BigDecimal total, Instant createdAt, Status status,
                    OrderConfirmed confirmation, List<Reaction> reactions) {

    public enum Status { REGISTRADO, CONFIRMADO }

    public Order confirmed(OrderConfirmed event, List<Reaction> reactions) {
        return new Order(id, customer, category, amount, discount, total, createdAt, Status.CONFIRMADO,
                event, reactions);
    }
}
