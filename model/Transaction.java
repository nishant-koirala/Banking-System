package model;

import java.time.LocalDateTime;

public class Transaction {
    private String transactionId;
    private String fromAccountId;
    private String toAccountId;
    private double amount;
    private LocalDateTime timestamp;
    private int priority;
    private Status status;

    public enum Status {
        PENDING,
        COMPLETED,
        FAILED
    }

    public Transaction(String transactionId, String fromAccountId, String toAccountId, double amount, LocalDateTime timestamp, int priority) {
        this.transactionId = transactionId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.priority = priority;
        this.status = Status.PENDING;
    }

    public String getTransactionId() { return transactionId; }
    public String getFromAccountId() { return fromAccountId; }
    public String getToAccountId() { return toAccountId; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public int getPriority() { return priority; }
    public Status getStatus() { return status; }

    public void setStatus(Status status) { this.status = status; }

    @Override
    public String toString() {
        return "Transaction{" +
            "id='" + transactionId + '\'' +
            ", from='" + fromAccountId + '\'' +
            ", to='" + toAccountId + '\'' +
            ", amount=" + amount +
            ", status=" + status +
            ", timestamp=" + timestamp +
            ", priority=" + priority +
            '}';
        }
}