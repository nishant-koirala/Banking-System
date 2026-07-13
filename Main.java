import java.util.UUID;

import exception.DuplicateTransactionException;
import model.Account;
import model.Transaction;
import services.Bank;

public class Main {

    // Small helper so submissions land at visibly different timestamps instead of all
    // landing within the same millisecond. Wraps Thread.sleep's checked InterruptedException
    // so call sites don't need their own try/catch.
    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore the interrupt flag, don't swallow it
        }
    }

    public static void main(String[] args) {
        
Bank bank = new Bank();
Account acc1 = bank.createAccount("Nishanta", 5000.0);
Account acc2 = bank.createAccount("Ram", 3000.0);

System.out.println("Before transfer:");
System.out.println("ACC001 balance: " + acc1.getBalance()); // 5000.0
System.out.println("ACC002 balance: " + acc2.getBalance()); // 3000.0

bank.transfer(acc1.getAccountId(), acc2.getAccountId(), 1000.0);

System.out.println("\nAfter transfer:");
System.out.println("ACC001 balance: " + acc1.getBalance()); // 4000.0
System.out.println("ACC002 balance: " + acc2.getBalance()); // 4000.0

System.out.println("\nACC001 transaction history:");
for (Transaction t : acc1.getTransactionHistory()) {
    System.out.println(t);
}

System.out.println("\nACC002 transaction history:");
for (Transaction t : acc2.getTransactionHistory()) {
    System.out.println(t);
}

System.out.println("\n--- Idempotency Test ---");
System.out.println("ACC001 balance before: " + acc1.getBalance());
System.out.println("ACC002 balance before: " + acc2.getBalance());

String txnId = UUID.randomUUID().toString();

// First submission — should succeed
bank.submitTransaction(txnId, acc1.getAccountId(), acc2.getAccountId(), 500.0);
System.out.println("First submission processed successfully");
System.out.println("ACC001 balance after first: " + acc1.getBalance());
System.out.println("ACC002 balance after first: " + acc2.getBalance());

// Second submission — same ID, should be rejected
try {
    bank.submitTransaction(txnId, acc1.getAccountId(), acc2.getAccountId(), 500.0);
} catch (DuplicateTransactionException e) {
    System.out.println("Duplicate caught: " + e.getMessage());
}
System.out.println("ACC001 balance after duplicate attempt: " + acc1.getBalance());
System.out.println("ACC002 balance after duplicate attempt: " + acc2.getBalance());

// New legitimate transaction — different ID, should succeed
String newTxnId = UUID.randomUUID().toString();
bank.submitTransaction(newTxnId, acc1.getAccountId(), acc2.getAccountId(), 200.0);
System.out.println("New transaction processed successfully");
System.out.println("ACC001 final balance: " + acc1.getBalance());
System.out.println("ACC002 final balance: " + acc2.getBalance());

// ---------------------------------------------------------------------
// --- Priority Queue Test ---
// Submit several pending transactions out of priority order, then process
// the whole queue and watch them come out VIP -> PREMIUM -> STANDARD,
// FIFO within the same tier.
// ---------------------------------------------------------------------
System.out.println("\n--- Priority Queue Test ---");
System.out.println("ACC001 balance before: " + acc1.getBalance());
System.out.println("ACC002 balance before: " + acc2.getBalance());

bank.submitPendingTransaction(UUID.randomUUID().toString(),
        acc1.getAccountId(), acc2.getAccountId(), 50.0, Bank.PRIORITY_STANDARD);
pause(50);
bank.submitPendingTransaction(UUID.randomUUID().toString(),
        acc2.getAccountId(), acc1.getAccountId(), 200.0, Bank.PRIORITY_VIP);
pause(50);
bank.submitPendingTransaction(UUID.randomUUID().toString(),
        acc1.getAccountId(), acc2.getAccountId(), 75.0, Bank.PRIORITY_PREMIUM);
pause(50);
bank.submitPendingTransaction(UUID.randomUUID().toString(),
        acc2.getAccountId(), acc1.getAccountId(), 20.0, Bank.PRIORITY_STANDARD);
pause(50);
// Deliberately oversized — this account can't cover it, should come out FAILED, not crash the queue.
bank.submitPendingTransaction(UUID.randomUUID().toString(),
        acc1.getAccountId(), acc2.getAccountId(), 999999.0, Bank.PRIORITY_VIP);

System.out.println("Queued " + bank.pendingCount() + " pending transactions.");
System.out.println("Processing in priority order (VIP > PREMIUM > STANDARD, FIFO within tier):");

bank.processAllPendingTransactions();

for (Transaction t : bank.getProcessedLog()) {
    System.out.println(t);
}

System.out.println("\nACC001 balance after queue processed: " + acc1.getBalance());
System.out.println("ACC002 balance after queue processed: " + acc2.getBalance());
}}