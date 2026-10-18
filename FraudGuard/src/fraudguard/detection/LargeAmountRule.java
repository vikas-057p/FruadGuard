package fraudguard.detection;

import java.math.BigDecimal;
import java.sql.Connection;

public class LargeAmountRule implements DetectionRule {
    @Override
    public int score(Connection connection, int senderAccountId, BigDecimal amount) {
        int points = 0;

        if (amount.compareTo(new BigDecimal("50000")) > 0) {
            points += 40;
        }

        if (amount.compareTo(new BigDecimal("100000")) > 0) {
            points += 30;
        }

        return points;
    }

    @Override
    public String getName() {
        return "Large transaction amount";
    }
}