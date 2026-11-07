package pe.novamarket.event;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class AdminTracking implements OrderObserver {
    @Override
    public String area() {
        return "Administración";
    }

    @Override
    public String onConfirmed(OrderConfirmed event) {
        return "Seguimiento registrado para el pedido #%d por S/ %s.".formatted(event.orderId(), event.total());
    }
}
