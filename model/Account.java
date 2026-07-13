    package model;
    import java.time.LocalDateTime;
    import java.util.ArrayList;
    import java.util.Collections;
    import java.util.List;
    import java.util.UUID;
    import exception.InsufficientBalanceException;

    public class Account {
        private String accountId;
        private String ownerName;
        private double balance;
        private List <Transaction> transactionsHistory;

        public Account(String accountId, String ownerName, double balance) {
            this.accountId = accountId;
            this.ownerName = ownerName;
            this.balance = balance;
            this.transactionsHistory = new ArrayList<>();
        }

        public String getAccountId() { return accountId; }
        public String getOwnerName() { return ownerName; }
        public double getBalance() { return balance; }
        public List<Transaction> getTransactionHistory() {
            return Collections.unmodifiableList(transactionsHistory);
            }      


        // Version 1 — plain deposit from outside
        public void deposit(double amount) {
            deposit(amount, "EXTERNAL");
        }

        // Version 2 — deposit that's part of a transfer, knows the source
        public void deposit(double amount, String fromAccountId) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Deposit amount must be positive.");
            }
            balance += amount;

            Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                fromAccountId,    // correctly records where the money actually came from
                this.accountId,
                amount,
                LocalDateTime.now(),
                0
            );
            transaction.setStatus(Transaction.Status.COMPLETED);
            transactionsHistory.add(transaction);
        }
    
        // Version 1 — plain withdrawal
        public void withdraw(double amount) {
            withdraw(amount, "EXTERNAL"); // just calls the more detailed version with "EXTERNAL" as default
        }

        // Version 2 — the real implementation, used for both cases
        public void withdraw(double amount, String toAccountId) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Withdrawal amount must be positive.");
            }
            if (amount > balance) {
                throw new InsufficientBalanceException("Insufficient funds for withdrawal.");
            }
            balance -= amount;

            Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                this.accountId,
                toAccountId,      // now correctly records whoever actually received the money
                amount,
                LocalDateTime.now(),
                0
            );
            transaction.setStatus(Transaction.Status.COMPLETED);  
            transactionsHistory.add(transaction); 
        }

 }

    