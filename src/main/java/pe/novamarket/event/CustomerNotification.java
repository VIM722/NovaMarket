package pe.novamarket.event;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class CustomerNotification implements OrderObserver {
    @Override
    public String area() {
        return "Cliente";
    }

    @Override
    public String onConfirmed(OrderConfirmed event) {
        return "Confirmación para %s del pedido #%d.".formatted(event.customer(), event.orderId());
    }
}
