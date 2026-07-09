import exception.AccountNotFoundException;
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
}}