package pe.novamarket.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderConfirmed(UUID id, long orderId, String customer, BigDecimal total, Instant occurredAt) {}
