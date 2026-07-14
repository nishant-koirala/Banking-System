package services;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import exception.AccountNotFoundException;
import exception.DuplicateTransactionException;
import model.Account;
import model.Transaction;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class Bank {
    private Map<String, Account> accounts;        // HashMap — fast lookup by account ID
    private int nextAccountNumber;
    private Set<String> processedTransactionIds = new HashSet<>(); // HashSet — idempotency guard, remembers every txn ID we've accepted
    private TreeMap<LocalDateTime, List<Transaction>> transactionsByTime = new TreeMap<>();
    // ===================== PENDING TRANSACTION PROCESSING (PriorityQueue) =====================

    // Defines the processing order for the heap below.
    // PriorityQueue is a MIN-heap by default, so comparingInt(priority) alone would pop the
    // LOWEST priority number first — the opposite of what we want. .reversed() flips that so
    // the HIGHEST priority number (e.g. VIP = 3) comes out first.
    // .thenComparing(timestamp) only matters when two transactions tie on priority — it breaks
    // the tie by earliest submission time first (FIFO within the same tier). This second key is
    // NOT affected by the .reversed() above, since reversed() only reverses the comparator it's
    // chained directly onto.
    private static final Comparator<Transaction> PRIORITY_ORDER =
            Comparator.comparingInt(Transaction::getPriority).reversed()
                      .thenComparing(Transaction::getTimestamp);

    // The heap itself. Transactions live here between submission and execution — nothing in
    // this queue has actually moved any money yet.
    private PriorityQueue<Transaction> pendingQueue = new PriorityQueue<>(PRIORITY_ORDER);

    // Flat, ordered record of every transaction that has come OUT of the queue (whether it
    // succeeded or failed), in the order it was processed. Separate from each Account's own
    // transactionsHistory list — this is a Bank-wide audit trail, not a per-account ledger.
    private List<Transaction> processedLog = new ArrayList<>();

    // Named priority levels so callers don't hardcode magic numbers like "3" everywhere.
    // These map directly onto Transaction's existing int priority field — no model changes needed.
    public static final int PRIORITY_STANDARD = 1;
    public static final int PRIORITY_PREMIUM = 2;
    public static final int PRIORITY_VIP = 3;

    // ============================================================================================


    public Bank() {
        // this.accounts = new HashMap<>();     // Original — not thread-safe, can throw ConcurrentModificationException if multiple threads access it simultaneously
        this.accounts = new ConcurrentHashMap<>(); // Use ConcurrentHashMap for thread-safe access
        this.nextAccountNumber = 1;
    }

    // Auto-generates an ID like "ACC001", "ACC002", ... and registers the account in the HashMap.
    public Account createAccount(String ownerName, double initialBalance) {
        String accountId = "ACC" + String.format("%03d", nextAccountNumber++);
        Account newAccount = new Account(accountId, ownerName, initialBalance);
        accounts.put(accountId, newAccount);   // store the new account in the HashMap for future retrieval
        return newAccount;
    }

    // Simple HashMap lookup, wrapped so callers get a clear exception instead of a null pointer.
    public Account getAccount(String accountId)  {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException("No account found with ID: " + accountId);

        }
        return account;
    }
    public void deposit(String accountId, double amount) {
    Account account = getAccount(accountId);
    synchronized (account) {
        account.deposit(amount);
    }
}

    public void withdraw(String accountId, double amount) {
        Account account = getAccount(accountId);
        synchronized (account) {
            account.withdraw(amount);
        }
    }



    // Executes an IMMEDIATE transfer — no queueing, no priority. Used directly by submitTransaction()
    // below, and reused internally by processNextPendingTransaction() once a queued transaction
    // reaches the front of the line. This is the only place actual balance mutation happens.
public void transfer(String fromAccountId, String toAccountId, double amount) {
    Account fromAccount = getAccount(fromAccountId);
    Account toAccount = getAccount(toAccountId);

    // Always lock accounts in consistent alphabetical ID order
    // This prevents deadlock when two threads transfer in opposite directions simultaneously
    // e.g. Thread A: ACC001→ACC002, Thread B: ACC002→ACC001
    // Without consistent ordering, both threads could hold one lock and wait for the other forever
    // With consistent ordering, both threads always try ACC001 first — one wins, one waits safely
    Account firstLock  = fromAccountId.compareTo(toAccountId) < 0 ? fromAccount : toAccount;
    Account secondLock = fromAccountId.compareTo(toAccountId) < 0 ? toAccount : fromAccount;

    synchronized (firstLock) {
        synchronized (secondLock) {
            fromAccount.withdraw(amount, toAccountId);
            toAccount.deposit(amount, fromAccountId);
        }
    }
}

  public double getBalance(String accountId) {
        Account account = getAccount(accountId);
        return account.getBalance();
    }

    // Original "fire immediately" submission path — unchanged. Checks the HashSet for a duplicate
    // ID, then executes the transfer right away. Left as-is so existing idempotency behavior
    // doesn't change; the priority-queue path below is a separate, parallel option.
    public void submitTransaction(String transactionId, String fromAccountId, String toAccountId, double amount) {
        if (processedTransactionIds.contains(transactionId)) {
            throw new DuplicateTransactionException("Transaction " + transactionId + " has already been processed");
        }
        processedTransactionIds.add(transactionId);
        transfer(fromAccountId, toAccountId, amount);
    }

    // ---------------------------------------------------------------------
    // NEW: priority-queued submission. Doesn't execute immediately — it just
    // records intent (PENDING) and enqueues it. Nothing moves until
    // processNextPendingTransaction() / processAllPendingTransactions() runs.
    // ---------------------------------------------------------------------
    public Transaction submitPendingTransaction(String transactionId, String fromAccountId,String toAccountId, double amount, int priority) {
        // Same duplicate check as submitTransaction() — reject if we've already seen this ID.
        if (processedTransactionIds.contains(transactionId)) {
            throw new DuplicateTransactionException("Transaction " + transactionId + " has already been processed");
        }
        // Reserve the ID now (before it's even processed) so the same transaction can't be
        // queued a second time while the first copy is still sitting in pendingQueue.
        processedTransactionIds.add(transactionId);

        // Build the Transaction "intent" object. Its constructor already sets status = PENDING
        // by default, so there's nothing extra to set here.
        Transaction pending = new Transaction(
                transactionId, fromAccountId, toAccountId, amount, LocalDateTime.now(), priority
        );

        // offer() inserts into the heap; PriorityQueue re-sorts internally so the highest-priority
        // (per PRIORITY_ORDER) item is always at the front, ready for poll().
        pendingQueue.offer(pending);
        return pending;
    }

    // Lets callers check the queue before/without draining it.
    public boolean hasPendingTransactions() {
        return !pendingQueue.isEmpty();
    }

    public int pendingCount() {
        return pendingQueue.size();
    }



    private void indexTransaction(Transaction t) {
    transactionsByTime
        .computeIfAbsent(t.getTimestamp(), k -> new ArrayList<>())
        .add(t);
}
    /**
     * Pops the single highest-priority pending transaction and actually executes it
     * via the existing transfer() logic. On failure (bad account, insufficient funds),
     * the pending Transaction is marked FAILED instead of throwing — the queue keeps moving.
     */
    public Transaction processNextPendingTransaction() {
        // poll() removes AND returns the highest-priority element, or null if the queue is
        // empty (unlike remove(), poll() never throws on an empty queue).
        Transaction next = pendingQueue.poll();
        if (next == null) return null; // nothing pending — nothing to do

        try {
            // Reuse the same transfer() used by the immediate-execution path — this is where
            // the money actually moves and where Account creates its own ledger-level
            // Transaction records (separate objects from "next" here).
            transfer(next.getFromAccountId(), next.getToAccountId(), next.getAmount());
            next.setStatus(Transaction.Status.COMPLETED);
        } catch (RuntimeException e) {
            // Catches AccountNotFoundException, InsufficientBalanceException, etc.
            // A single bad transaction shouldn't halt the rest of the queue, so we
            // mark it FAILED and move on instead of letting the exception propagate.
            next.setStatus(Transaction.Status.FAILED);
        }

        // Record the outcome (success or failure) in the Bank-wide audit trail.
        indexTransaction(next);
        processedLog.add(next);
        return next;
    }
    public List<Transaction> getTransactionsBetween(LocalDateTime start, LocalDateTime end) {
    List<Transaction> result = new ArrayList<>();
    transactionsByTime
        .subMap(start, true, end, true)
        .values()
        .forEach(result::addAll);
    return result;
}
    // Drains the entire queue, processing one transaction at a time in priority order,
    // until nothing is left pending.
    public void processAllPendingTransactions() {
        while (hasPendingTransactions()) {
            processNextPendingTransaction();
        }
    }

    // Exposes the audit trail so callers (e.g. Main.java) can print or inspect what happened,
    // in the exact order it was processed.
    public List<Transaction> getProcessedLog() {
        return processedLog;
    }
}