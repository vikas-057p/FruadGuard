package fraudguard.service;

import fraudguard.db.DBConnection;
import fraudguard.detection.FraudDetector;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TransactionService {
    private final FraudDetector detector = new FraudDetector();

    public long createTransaction(int senderId, int receiverId, BigDecimal amount)
            throws SQLException {
        if (senderId == receiverId) {
            throw new IllegalArgumentException(
                    "Sender and receiver must be different.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                BigDecimal balance = getBalance(connection, senderId);
                getBalance(connection, receiverId);

                if (balance.compareTo(amount) < 0) {
                    throw new IllegalArgumentException(
                            "Insufficient sender balance.");
                }

                int score = detector.calculateScore(connection, senderId, amount);
                String level = detector.riskLevel(score);
                String reason = detector.explain(connection, senderId, amount);
                String status = score >= 60 ? "REVIEW" : "APPROVED";

                String sql = "INSERT INTO transactions " +
                        "(sender_account_id, receiver_account_id, amount, " +
                        "risk_score, risk_level, status, reason) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

                long transactionId;

                try (PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, senderId);
                    statement.setInt(2, receiverId);
                    statement.setBigDecimal(3, amount);
                    statement.setInt(4, score);
                    statement.setString(5, level);
                    statement.setString(6, status);
                    statement.setString(7, reason);

                    statement.executeUpdate();

                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Could not obtain transaction ID.");
                        }

                        transactionId = keys.getLong(1);
                    }
                }

                if ("APPROVED".equals(status)) {
                    transferBalance(connection, senderId, receiverId, amount);
                }

                connection.commit();
                return transactionId;

            } catch (Exception exception) {
                connection.rollback();

                if (exception instanceof SQLException) {
                    throw (SQLException) exception;
                }

                if (exception instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) exception;
                }

                throw new SQLException(
                        "Transaction could not be completed.", exception);

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private BigDecimal getBalance(Connection connection, int accountId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance FROM accounts WHERE account_id = ? FOR UPDATE")) {
            statement.setInt(1, accountId);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(
                            "Account ID " + accountId + " does not exist.");
                }

                return result.getBigDecimal("balance");
            }
        }
    }

    private void transferBalance(Connection connection, int senderId,
                                 int receiverId, BigDecimal amount)
            throws SQLException {
        try (PreparedStatement debit = connection.prepareStatement(
                "UPDATE accounts SET balance = balance - ? WHERE account_id = ?");
             PreparedStatement credit = connection.prepareStatement(
                     "UPDATE accounts SET balance = balance + ? WHERE account_id = ?")) {

            debit.setBigDecimal(1, amount);
            debit.setInt(2, senderId);
            debit.executeUpdate();

            credit.setBigDecimal(1, amount);
            credit.setInt(2, receiverId);
            credit.executeUpdate();
        }
    }

    public void reviewTransaction(long transactionId, boolean approve)
            throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                int senderId;
                int receiverId;
                BigDecimal amount;
                String status;

                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT sender_account_id, receiver_account_id, " +
                                "amount, status FROM transactions " +
                                "WHERE transaction_id = ? FOR UPDATE")) {

                    statement.setLong(1, transactionId);

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException(
                                    "Transaction not found.");
                        }

                        senderId = result.getInt("sender_account_id");
                        receiverId = result.getInt("receiver_account_id");
                        amount = result.getBigDecimal("amount");
                        status = result.getString("status");
                    }
                }

                if (!"REVIEW".equals(status)) {
                    throw new IllegalArgumentException(
                            "Only transactions awaiting review can be reviewed.");
                }

                if (approve) {
                    BigDecimal balance = getBalance(connection, senderId);

                    if (balance.compareTo(amount) < 0) {
                        throw new IllegalArgumentException(
                                "Insufficient balance at approval time.");
                    }

                    transferBalance(connection, senderId, receiverId, amount);
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE transactions SET status = ? " +
                                "WHERE transaction_id = ?")) {

                    statement.setString(1, approve ? "APPROVED" : "REJECTED");
                    statement.setLong(2, transactionId);
                    statement.executeUpdate();
                }

                connection.commit();

            } catch (Exception exception) {
                connection.rollback();

                if (exception instanceof SQLException) {
                    throw (SQLException) exception;
                }

                if (exception instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) exception;
                }

                throw new SQLException(
                        "Review could not be saved.", exception);

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}