package ke.shulehub.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.function.Function;

/** Kenya shilling amounts are BigDecimal with two decimal places. */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    private Money() {
    }

    public static BigDecimal of(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(String amount) {
        return of(new BigDecimal(amount.trim()));
    }

    public static <T> BigDecimal sum(Collection<T> items, Function<T, BigDecimal> amount) {
        return items.stream().map(amount).reduce(ZERO, BigDecimal::add);
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    public static boolean isWholeShillings(BigDecimal amount) {
        return amount.stripTrailingZeros().scale() <= 0;
    }
}
