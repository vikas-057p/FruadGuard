package fraudguard.detection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FraudDetector {
    private final List<DetectionRule> rules = new ArrayList<>();

    public FraudDetector() {
        rules.add(new LargeAmountRule());
        rules.add(new RapidActivityRule());
    }

    public FraudDetector(DetectionRule rule) {
        rules.add(rule);
    }

    public int calculateScore(Connection connection, int senderAccountId, BigDecimal amount)
            throws SQLException {
        int total = 0;

        for (DetectionRule rule : rules) {
            total += rule.score(connection, senderAccountId, amount);
        }

        return Math.min(total, 100);
    }

    public String explain(Connection connection, int senderAccountId, BigDecimal amount)
            throws SQLException {
        List<String> reasons = new ArrayList<>();

        for (DetectionRule rule : rules) {
            if (rule.score(connection, senderAccountId, amount) > 0) {
                reasons.add(rule.getName());
            }
        }

        return reasons.isEmpty()
                ? "No demo rules triggered"
                : String.join("; ", reasons);
    }

    public String riskLevel(int score) {
        if (score >= 60) return "HIGH";
        if (score >= 30) return "MEDIUM";
        return "LOW";
    }
}