package pe.novamarket.event;

public interface OrderObserver {
    String area();
    String onConfirmed(OrderConfirmed event);
}
