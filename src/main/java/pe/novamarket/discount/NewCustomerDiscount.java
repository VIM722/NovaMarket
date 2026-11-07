package pe.novamarket.discount;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class NewCustomerDiscount implements DiscountPolicy {
    private static final Category CATEGORY = new Category("NUEVO", "Cliente nuevo", "5 %");
    private static final BigDecimal RATE = new BigDecimal("0.05");

    @Override
    public Category category() {
        return CATEGORY;
    }

    @Override
    public BigDecimal discount(BigDecimal amount) {
        return amount.multiply(RATE);
    }
}
