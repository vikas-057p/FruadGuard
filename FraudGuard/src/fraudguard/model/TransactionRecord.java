package fraudguard.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class TransactionRecord {
    private final long transactionId;
    private final int senderAccountId;
    private final int receiverAccountId;
    private final BigDecimal amount;
    private final LocalDateTime createdAt;
    private final int riskScore;
    private final String riskLevel;
    private final String status;
    private final String reason;

    public TransactionRecord(long transactionId, int senderAccountId, int receiverAccountId,
                             BigDecimal amount, LocalDateTime createdAt, int riskScore,
                             String riskLevel, String status, String reason) {
        this.transactionId = transactionId;
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.amount = amount;
        this.createdAt = createdAt;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.status = status;
        this.reason = reason;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public int getSenderAccountId() {
        return senderAccountId;
    }

    public int getReceiverAccountId() {
        return receiverAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "Transaction #" + transactionId + " (" + riskLevel + ", " + status + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TransactionRecord)) return false;
        TransactionRecord that = (TransactionRecord) other;
        return transactionId == that.transactionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
}