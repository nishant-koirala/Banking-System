package services;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import exception.AccountNotFoundException;
import exception.DuplicateTransactionException;
import model.Account;

public class Bank {
    private Map<String, Account> accounts;        // HashMap — fast lookup by account ID
    private int nextAccountNumber;
    private Set<String> processedTransactionIds = new HashSet<>();


    public Bank() {
        this.accounts = new HashMap<>();
        this.nextAccountNumber = 1;
    }

    public Account createAccount(String ownerName, double initialBalance) {
        String accountId = "ACC" + String.format("%03d", nextAccountNumber++);
        Account newAccount = new Account(accountId, ownerName, initialBalance);
        accounts.put(accountId, newAccount);   // store the new account in the HashMap for future retrieval
        return newAccount;
    }

    public Account getAccount(String accountId)  {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException("No account found with ID: " + accountId);

        }
        return account;
    }


    public void transfer(String fromAccountId, String toAccountId, double amount) {
        Account fromAccount = getAccount(fromAccountId); // throws AccountNotFoundException if missing
        Account toAccount = getAccount(toAccountId);

        fromAccount.withdraw(amount, toAccountId); // version 2 — records ACC002 as destination
        toAccount.deposit(amount, fromAccountId);   // version 2 — records ACC001 as source
    }
    public void submitTransaction(String transactionId, String fromAccountId, String toAccountId, double amount) {
        if (processedTransactionIds.contains(transactionId)) {
            throw new DuplicateTransactionException("Transaction " + transactionId + " has already been processed");
        }
        processedTransactionIds.add(transactionId);
        transfer(fromAccountId, toAccountId, amount);
    }
}
