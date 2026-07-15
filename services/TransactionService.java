package services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import exception.DuplicateTransactionException;
import model.Transaction;

/**
 * Everything to do with "a transaction, as opposed to raw money movement":
 *   - idempotency (reject duplicate transaction IDs)
 *   - the priority queue transactions wait in before they're executed
 *   - actually popping and executing pending transactions (via TransferService)
 *   - the time-indexed audit trail of everything that's been processed
 *
 * These pieces always change together in practice, so they live in one class
 * rather than being split into separate files that would only ever be
 * constructed and used as a unit.
 */
public class TransactionService {

    // Named priority levels so callers don't hardcode magic numbers like "3"
    // everywhere. These map directly onto Transaction's existing int priority field.
    public static final int PRIORITY_STANDARD = 1;
    public static final int PRIORITY_PREMIUM = 2;
    public static final int PRIORITY_VIP = 3;

    // PriorityQueue is a MIN-heap by default, so comparingInt(priority) alone would
    // pop the LOWEST priority number first — the opposite of what we want. .reversed()
    // flips that so the HIGHEST priority number (e.g. VIP = 3) comes out first.
    // .thenComparing(timestamp) only matters when two transactions tie on priority —
    // it breaks the tie by earliest submission time first (FIFO within the same tier).
    // This second key is NOT affected by the .reversed() above, since reversed() only
    // reverses the comparator it's chained directly onto.
    private static final Comparator<Transaction> PRIORITY_ORDER =
            Comparator.comparingInt(Transaction::getPriority).reversed()
                      .thenComparing(Transaction::getTimestamp);

    private final TransferService transferService;

    // Idempotency guard — remembers every txn ID we've accepted, whether
    // executed immediately or queued for later. Synchronized access since
    // HashSet itself isn't thread-safe.
    private final Set<String> processedTransactionIds = new HashSet<>();

    // The heap. Transactions live here between submission and execution —
    // nothing in this queue has actually moved any money yet.
    private final PriorityQueue<Transaction> pendingQueue = new PriorityQueue<>(PRIORITY_ORDER);

    // Time-indexed view of every transaction that's come OUT of the queue
    // (whether it succeeded or failed), for range queries.
    private final TreeMap<LocalDateTime, List<Transaction>> transactionsByTime = new TreeMap<>();

    // Flat, ordered record of every transaction that's come out of the queue,
    // in the order it was processed. This is a Bank-wide audit trail, separate
    // from each Account's own transaction history.
    private final List<Transaction> processedLog = new ArrayList<>();

    public TransactionService(TransferService transferService) {
        this.transferService = transferService;
    }

    // Throws if the ID was already seen; otherwise reserves it immediately so a
    // transaction can't be submitted/queued twice while the first copy is still
    // in flight. Used by both the immediate-execution path (Bank.submitTransaction)
    // and the queued path (submitPendingTransaction) below.
    public synchronized void reserveTransactionId(String transactionId) {
        if (processedTransactionIds.contains(transactionId)) {
            throw new DuplicateTransactionException(
                    "Transaction " + transactionId + " has already been processed");
        }
        processedTransactionIds.add(transactionId);
    }

    // Doesn't execute immediately — just records intent (PENDING) and enqueues it.
    // Nothing moves until processNextPendingTransaction() / processAllPendingTransactions() runs.
    public Transaction submitPendingTransaction(String transactionId, String fromAccountId, String toAccountId, double amount, int priority) {
        reserveTransactionId(transactionId);

        // Transaction's constructor already sets status = PENDING by default.
        Transaction pending = new Transaction(
                transactionId, fromAccountId, toAccountId, amount, LocalDateTime.now(), priority);

        pendingQueue.offer(pending);
        return pending;
    }

    public boolean hasPendingTransactions() {
        return !pendingQueue.isEmpty();
    }

    public int pendingCount() {
        return pendingQueue.size();
    }

    /**
     * Pops the single highest-priority pending transaction and actually executes
     * it via TransferService. On failure (bad account, insufficient funds), the
     * pending Transaction is marked FAILED instead of throwing — the queue keeps
     * moving.
     */
    public Transaction processNextPendingTransaction() {
        Transaction next = pendingQueue.poll();
        if (next == null) return null; // nothing pending — nothing to do

        try {
            transferService.transfer(next.getFromAccountId(), next.getToAccountId(), next.getAmount());
            next.setStatus(Transaction.Status.COMPLETED);
        } catch (RuntimeException e) {
            // Catches AccountNotFoundException, InsufficientBalanceException, etc.
            // A single bad transaction shouldn't halt the rest of the queue, so we
            // mark it FAILED and move on instead of letting the exception propagate.
            next.setStatus(Transaction.Status.FAILED);
        }

        indexTransaction(next);
        processedLog.add(next);
        return next;
    }

    // Drains the entire queue, processing one transaction at a time in priority
    // order, until nothing is left pending.
    public void processAllPendingTransactions() {
        while (hasPendingTransactions()) {
            processNextPendingTransaction();
        }
    }

    public List<Transaction> getProcessedLog() {
        return processedLog;
    }

    private void indexTransaction(Transaction t) {
        transactionsByTime
                .computeIfAbsent(t.getTimestamp(), k -> new ArrayList<>())
                .add(t);
    }

    public List<Transaction> getTransactionsBetween(LocalDateTime start, LocalDateTime end) {
        List<Transaction> result = new ArrayList<>();
        transactionsByTime
                .subMap(start, true, end, true)
                .values()
                .forEach(result::addAll);
        return result;
    }
}