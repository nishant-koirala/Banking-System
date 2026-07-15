package services;

import java.time.LocalDateTime;
import java.util.List;

import model.Account;
import model.Transaction;

/**
 * Bank is a thin facade wiring together three collaborators, each with one job:
 *
 *   AccountRegistry    - account storage & lookup
 *   TransferService    - deposit/withdraw/transfer + deadlock-safe locking
 *   TransactionService - idempotency, the priority queue, processing, and the
 *                        time-indexed audit trail
 *
 * Public API is unchanged from the original single-file Bank.
 */
public class Bank {

    private final AccountRegistry accountRegistry = new AccountRegistry();
    private final TransferService transferService = new TransferService(accountRegistry);
    private final TransactionService transactionService = new TransactionService(transferService);

    // Kept for backward compatibility — same constants callers already use,
    // e.g. Bank.PRIORITY_VIP — now sourced from TransactionService.
    public static final int PRIORITY_STANDARD = TransactionService.PRIORITY_STANDARD;
    public static final int PRIORITY_PREMIUM = TransactionService.PRIORITY_PREMIUM;
    public static final int PRIORITY_VIP = TransactionService.PRIORITY_VIP;

    public Account createAccount(String ownerName, double initialBalance) {
        return accountRegistry.createAccount(ownerName, initialBalance);
    }

    public Account getAccount(String accountId) {
        return accountRegistry.getAccount(accountId);
    }

    public void deposit(String accountId, double amount) {
        transferService.deposit(accountId, amount);
    }

    public void withdraw(String accountId, double amount) {
        transferService.withdraw(accountId, amount);
    }

    public void transfer(String fromAccountId, String toAccountId, double amount) {
        transferService.transfer(fromAccountId, toAccountId, amount);
    }

    public double getBalance(String accountId) {
        return transferService.getBalance(accountId);
    }

    // Original "fire immediately" submission path — unchanged behavior: checks
    // for a duplicate ID, then executes the transfer right away.
    public void submitTransaction(String transactionId, String fromAccountId,String toAccountId, double amount) {
        transactionService.reserveTransactionId(transactionId);
        transferService.transfer(fromAccountId, toAccountId, amount);
    }

    public Transaction submitPendingTransaction(String transactionId, String fromAccountId,String toAccountId, double amount, int priority) {
        return transactionService.submitPendingTransaction(
                transactionId, fromAccountId, toAccountId, amount, priority);
    }

    public boolean hasPendingTransactions() {
        return transactionService.hasPendingTransactions();
    }

    public int pendingCount() {
        return transactionService.pendingCount();
    }

    public Transaction processNextPendingTransaction() {
        return transactionService.processNextPendingTransaction();
    }

    public void processAllPendingTransactions() {
        transactionService.processAllPendingTransactions();
    }

    public List<Transaction> getProcessedLog() {
        return transactionService.getProcessedLog();
    }

    public List<Transaction> getTransactionsBetween(LocalDateTime start, LocalDateTime end) {
        return transactionService.getTransactionsBetween(start, end);
    }
}