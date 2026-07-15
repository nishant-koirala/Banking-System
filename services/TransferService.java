package services;

import model.Account;

/**
 * The only place actual balance mutation happens. Everything else (immediate
 * transfers, queued transfers) routes through here.
 */
public class TransferService {

    private final AccountRegistry accountRegistry;

    public TransferService(AccountRegistry accountRegistry) {
        this.accountRegistry = accountRegistry;
    }

    public void deposit(String accountId, double amount) {
        Account account = accountRegistry.getAccount(accountId);
        synchronized (account) {
            account.deposit(amount);
        }
    }

    public void withdraw(String accountId, double amount) {
        Account account = accountRegistry.getAccount(accountId);
        synchronized (account) {
            account.withdraw(amount);
        }
    }

    // Executes an IMMEDIATE transfer — no queueing, no priority.
    public void transfer(String fromAccountId, String toAccountId, double amount) {
        Account fromAccount = accountRegistry.getAccount(fromAccountId);
        Account toAccount = accountRegistry.getAccount(toAccountId);

        // Always lock accounts in consistent alphabetical ID order.
        // This prevents deadlock when two threads transfer in opposite directions
        // simultaneously (Thread A: ACC001->ACC002, Thread B: ACC002->ACC001) —
        // with consistent ordering both threads always try ACC001 first, one wins,
        // one waits safely.
        Account firstLock = fromAccountId.compareTo(toAccountId) < 0 ? fromAccount : toAccount;
        Account secondLock = fromAccountId.compareTo(toAccountId) < 0 ? toAccount : fromAccount;

        synchronized (firstLock) {
            synchronized (secondLock) {
                fromAccount.withdraw(amount, toAccountId);
                toAccount.deposit(amount, fromAccountId);
            }
        }
    }

    public double getBalance(String accountId) {
        return accountRegistry.getAccount(accountId).getBalance();
    }
}