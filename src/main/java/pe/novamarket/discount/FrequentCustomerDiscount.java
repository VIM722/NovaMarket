package pe.novamarket.discount;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class FrequentCustomerDiscount implements DiscountPolicy {
    private static final Category CATEGORY = new Category("FRECUENTE", "Cliente frecuente", "10 %");
    private static final BigDecimal RATE = new BigDecimal("0.10");

    @Override
    public Category category() {
        return CATEGORY;
    }

    @Override
    public BigDecimal discount(BigDecimal amount) {
        return amount.multiply(RATE);
    }
}
