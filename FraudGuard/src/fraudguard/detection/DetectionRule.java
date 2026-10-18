package fraudguard.detection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public interface DetectionRule {
    int score(Connection connection, int senderAccountId, BigDecimal amount) throws SQLException;

    String getName();
}