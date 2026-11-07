package pe.novamarket.event;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class WarehouseNotification implements OrderObserver {
    @Override
    public String area() {
        return "Almacén";
    }

    @Override
    public String onConfirmed(OrderConfirmed event) {
        return "Pedido #%d recibido para preparar su atención.".formatted(event.orderId());
    }
}
