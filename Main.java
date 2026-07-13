import java.util.UUID;

import exception.DuplicateTransactionException;
import model.Account;
import model.Transaction;
import services.Bank;

public class Main {
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
}}