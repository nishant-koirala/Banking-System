package services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import exception.AccountNotFoundException;
import model.Account;

/**
 * Owns the account map and ID generation. Nothing here moves money — this is
 * purely "does this account exist, and how do I create/find one."
 */
public class AccountRegistry {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>(); // thread-safe access
    private int nextAccountNumber = 1;

    // Auto-generates an ID like "ACC001", "ACC002", ... and registers the account.
    public Account createAccount(String ownerName, double initialBalance) {
        String accountId = "ACC" + String.format("%03d", nextAccountNumber++);
        Account newAccount = new Account(accountId, ownerName, initialBalance);
        accounts.put(accountId, newAccount);
        return newAccount;
    }

    // Simple lookup, wrapped so callers get a clear exception instead of a null pointer.
    public Account getAccount(String accountId) {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException("No account found with ID: " + accountId);
        }
        return account;
    }
}