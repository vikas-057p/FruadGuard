package fraudguard.detection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RapidActivityRule implements DetectionRule {
    @Override
    public int score(Connection connection, int senderAccountId, BigDecimal amount)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions " +
                "WHERE sender_account_id = ? " +
                "AND created_at >= NOW() - INTERVAL 5 MINUTE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, senderAccountId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && result.getInt(1) >= 2) {
                    return 35;
                }
            }
        }

        return 0;
    }

    @Override
    public String getName() {
        return "Rapid repeated activity";
    }
}