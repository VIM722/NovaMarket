package pe.novamarket.discount;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class DiscountCatalog {
    private final Map<String, DiscountPolicy> policies;

    public DiscountCatalog(List<DiscountPolicy> policies) {
        this.policies = policies.stream().collect(Collectors.toUnmodifiableMap(
                policy -> policy.category().code(), Function.identity()));
    }

    public List<DiscountPolicy.Category> categories() {
        return policies.values().stream().map(DiscountPolicy::category)
                .sorted(Comparator.comparing(DiscountPolicy.Category::label)).toList();
    }

    public DiscountPolicy find(String code) {
        return code == null ? null : policies.get(code);
    }
}
