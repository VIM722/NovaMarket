package pe.novamarket.discount;

import java.math.BigDecimal;

public interface DiscountPolicy {
    Category category();
    BigDecimal discount(BigDecimal amount);

    record Category(String code, String label, String rule) {}
}
