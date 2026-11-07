package pe.novamarket.discount;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class UnidentifiedCustomerDiscount implements DiscountPolicy {
    private static final Category CATEGORY = new Category("NO_IDENTIFICADO", "Cliente no identificado", "Sin descuento");

    @Override
    public Category category() {
        return CATEGORY;
    }

    @Override
    public BigDecimal discount(BigDecimal amount) {
        return BigDecimal.ZERO;
    }
}
