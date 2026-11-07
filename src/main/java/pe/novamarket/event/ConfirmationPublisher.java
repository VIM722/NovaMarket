package pe.novamarket.event;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConfirmationPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(ConfirmationPublisher.class);
    private final List<OrderObserver> observers;

    public ConfirmationPublisher(List<OrderObserver> observers) {
        this.observers = List.copyOf(observers);
    }

    public List<Reaction> publish(OrderConfirmed event) {
        LOG.info("PEDIDO_CONFIRMADO evento={} pedido={} fecha={}", event.id(), event.orderId(), event.occurredAt());
        List<Reaction> reactions = new ArrayList<>();
        for (OrderObserver observer : observers) {
            String area = observer.area();
            try {
                String message = observer.onConfirmed(event);
                reactions.add(new Reaction(area, message, true));
                LOG.info("evento={} área={} estado=PROCESADO", event.id(), area);
            } catch (RuntimeException ex) {
                reactions.add(new Reaction(area, "No se pudo procesar la reacción.", false));
                LOG.error("Error en la reacción de {} al evento {}", area, event.id(), ex);
            }
        }
        return List.copyOf(reactions);
    }

    public record Reaction(String area, String message, boolean successful) {}
}
