import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import exception.AccountNotFoundException;
import exception.DuplicateTransactionException;
import exception.InsufficientBalanceException;
import model.Account;
import model.Transaction;
import services.Bank;

/**
 * Handles all user interaction: input collection, menu display, and output.
 * Completely separate from business logic (Bank).
 */
public class UserInterface {
    private final Bank bank;
    private final Scanner scanner;

    public UserInterface(Bank bank) {
        this.bank = bank;
        this.scanner = new Scanner(System.in);
    }

    /**
     * Start the main menu loop. This is the only public method callers need.
     */
    public void start() {
        seedDemoData();

        while (true) {
            printMainMenu();
            int choice = scanner.nextInt();
            System.out.println("\n-----------------------------------------------------");

            switch (choice) {
                case 1 -> handleCreateAccount();
                case 2 -> handleDeposit();
                case 3 -> handleWithdraw();
                case 4 -> handleTransfer();
                case 5 -> handlePendingTransactionMenu();
                case 6 -> handleCheckBalance();
                case 7 -> handleTransactionHistory();
                case 8 -> handleExit();
                default -> handleInvalidChoice();
            }
        }
    }

    // ===================== DEMO DATA SEEDING =====================

    private void seedDemoData() {
        System.out.println("Seeding demo data...");

        Account Nishant = bank.createAccount("Nishant", 1000);
        Account Laxmi = bank.createAccount("Laxmi", 500);
        Account Sahara = bank.createAccount("Sahara", 2000);

        bank.deposit(Nishant.getAccountId(), 200);
        bank.withdraw(Laxmi.getAccountId(), 50);
        bank.transfer(Sahara.getAccountId(), Nishant.getAccountId(), 300);

        bank.submitPendingTransaction(UUID.randomUUID().toString(),
                Laxmi.getAccountId(), Sahara.getAccountId(), 100, Bank.PRIORITY_STANDARD);
        bank.submitPendingTransaction(UUID.randomUUID().toString(),
                Nishant.getAccountId(), Laxmi.getAccountId(), 75, Bank.PRIORITY_VIP);
        bank.submitPendingTransaction(UUID.randomUUID().toString(),
                Sahara.getAccountId(), Nishant.getAccountId(), 150, Bank.PRIORITY_PREMIUM);

        bank.processNextPendingTransaction();

        System.out.println("Demo accounts ready:");
        System.out.println(
                "  " + Nishant.getAccountId() + " - Nishant - balance: " + bank.getBalance(Nishant.getAccountId()));
        System.out.println(
                "  " + Laxmi.getAccountId() + " - Laxmi   - balance: " + bank.getBalance(Laxmi.getAccountId()));
        System.out.println(
                "  " + Sahara.getAccountId() + " - Sahara - balance: " + bank.getBalance(Sahara.getAccountId()));
        System.out.println("2 pending transactions still in the queue.");
        System.out.println("-----------------------------------------------------");
    }

    // ===================== MAIN MENU =====================

    private void printMainMenu() {
        System.out.println("-----------------Banking System Menu-----------------");
        System.out.println("1. Create Account");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Transfer");
        System.out.println("5. Pending Transaction Menu");
        System.out.println("6. Check Balance");
        System.out.println("7. Check Transaction History");
        System.out.println("8. Exit");
        System.out.print("Enter your choice: ");
    }

    private void handleCreateAccount() {
        System.out.print("Enter account holder name: ");
        String name = scanner.next();
        System.out.print("Enter initial balance: ");
        double balance = scanner.nextDouble();

        Account newAccount = bank.createAccount(name, balance);
        System.out.println("Account created successfully! Account ID: " + newAccount.getAccountId());
        System.out.println("\n-----------------------------------------------------");
    }

    private void handleDeposit() {
        System.out.print("Enter the account ID to deposit into: ");
        String depositAccountId = scanner.next();
        System.out.print("Enter the amount to deposit: ");
        double depositAmount = scanner.nextDouble();

        try {
            bank.deposit(depositAccountId, depositAmount);
            System.out.println("Deposit successful!");
        } catch (AccountNotFoundException e) {
            System.out.println("Account not found!");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid amount: " + e.getMessage());
        }
        System.out.println("\n-----------------------------------------------------");
    }

    private void handleWithdraw() {
        System.out.print("Enter the account ID to withdraw from: ");
        String withdrawAccountId = scanner.next();
        System.out.print("Enter the amount to withdraw: ");
        double withdrawAmount = scanner.nextDouble();

        try {
            bank.withdraw(withdrawAccountId, withdrawAmount);
            System.out.println("Withdrawal successful!");
        } catch (AccountNotFoundException e) {
            System.out.println("Account not found!");
        } catch (InsufficientBalanceException e) {
            System.out.println("Insufficient balance!");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid amount: " + e.getMessage());
        }
        System.out.println("\n-----------------------------------------------------");
    }

    private void handleTransfer() {
        System.out.print("Enter the source account ID: ");
        String fromAccountId = scanner.next();
        System.out.print("Enter the destination account ID: ");
        String toAccountId = scanner.next();
        System.out.print("Enter the amount to transfer: ");
        double transferAmount = scanner.nextDouble();

        try {
            bank.transfer(fromAccountId, toAccountId, transferAmount);
            System.out.println("Transfer successful!");
        } catch (AccountNotFoundException e) {
            System.out.println("Account not found!");
        } catch (InsufficientBalanceException e) {
            System.out.println("Insufficient balance!");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid amount: " + e.getMessage());
        }
        System.out.println("\n-----------------------------------------------------");
    }

    private void handleCheckBalance() {
        System.out.print("Enter the account ID to check balance: ");
        String balanceAccountId = scanner.next();

        try {
            double balanceAmount = bank.getBalance(balanceAccountId);
            System.out.println("Current balance: " + balanceAmount);
        } catch (AccountNotFoundException e) {
            System.out.println("Account not found!");
        }
    }

    private void handleTransactionHistory() {
        System.out.print("Enter the account ID to check transaction history: ");
        String historyAccountId = scanner.next();

        try {
            List<Transaction> history = bank.getAccount(historyAccountId).getTransactionHistory();
            System.out.println("Transaction History for Account " + historyAccountId + ":");
            for (Transaction transaction : history) {
                System.out.println("- " + transaction);
            }
        } catch (AccountNotFoundException e) {
            System.out.println("Account not found!");
        }
        System.out.println("\n-----------------------------------------------------");
    }

    private void handleExit() {
        System.out.println("Exiting the banking system. Goodbye!");
        scanner.close();
        System.exit(0);
    }

    private void handleInvalidChoice() {
        System.out.println("Invalid choice!");
        System.out.println("\n-----------------------------------------------------");
    }

    // ===================== PENDING TRANSACTION MENU =====================

    private void handlePendingTransactionMenu() {
        while (true) {
            printPendingTransactionMenu();
            int choice = scanner.nextInt();
            System.out.println("\n-----------------------------------------------------");

            switch (choice) {
                case 1 -> handleSubmitPendingTransfer();
                case 2 -> handleProcessNextPending();
                case 3 -> handleProcessAllPending();
                case 4 -> handlePendingCount();
                case 5 -> handleProcessedLog();
                case 6 -> handleRecentTransactions();
                case 7 -> {
                    return;
                }
                default -> handleInvalidChoice();
            }
        }
    }

    private void printPendingTransactionMenu() {
        System.out.println("----- Pending Transaction Menu -----");
        System.out.println("1. Submit pending transfer");
        System.out.println("2. Process next pending transaction");
        System.out.println("3. Process all pending transactions");
        System.out.println("4. Show pending transaction count");
        System.out.println("5. Show processed transaction log");
        System.out.println("6. Show transactions of last X minutes");
        System.out.println("7. Back to main menu");
        System.out.print("Enter your choice: ");
    }

    private void handleSubmitPendingTransfer() {
        System.out.print("Enter the source account ID: ");
        String fromAccountId = scanner.next();
        System.out.print("Enter the destination account ID: ");
        String toAccountId = scanner.next();
        System.out.print("Enter the amount to transfer: ");
        double amount = scanner.nextDouble();
        System.out.print("Enter priority (1=standard, 2=premium, 3=vip): ");
        int priority = scanner.nextInt();
        String transactionId = UUID.randomUUID().toString();

        try {
            bank.submitPendingTransaction(transactionId, fromAccountId, toAccountId, amount, priority);
            System.out.println("Pending transfer submitted with ID: " + transactionId);
        } catch (DuplicateTransactionException e) {
            System.out.println("Duplicate transaction ID!");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid amount: " + e.getMessage());
        }
        System.out.println("\n-----------------------------------------------------");
    }

    private void handleProcessNextPending() {
        Transaction nextTransaction = bank.processNextPendingTransaction();
        if (nextTransaction == null) {
            System.out.println("No pending transactions to process.");
        } else {
            System.out.println("Processed transaction: " + nextTransaction);
        }
        System.out.println("\n-----------------------------------------------------");
    }

    private void handleProcessAllPending() {
        bank.processAllPendingTransactions();
        System.out.println("All pending transactions processed.");
        System.out.println("\n-----------------------------------------------------");
    }

    private void handlePendingCount() {
        System.out.println("Pending transactions in queue: " + bank.pendingCount());
        System.out.println("\n-----------------------------------------------------");
    }

    private void handleProcessedLog() {
        List<Transaction> processedLog = bank.getProcessedLog();
        if (processedLog.isEmpty()) {
            System.out.println("No processed transactions yet.");
        } else {
            System.out.println("Processed Transaction Log:");
            for (Transaction transaction : processedLog) {
                System.out.println("- " + transaction);
            }
        }
        System.out.println("\n-----------------------------------------------------");
    }

    private void handleRecentTransactions() {
        System.out.println("Show Transaction of last X minutes");
        int minutes = scanner.nextInt();
        LocalDateTime start = LocalDateTime.now().minusMinutes(minutes);
        LocalDateTime end = LocalDateTime.now();

        List<Transaction> recentTransactions = bank.getTransactionsBetween(start, end);
        if (recentTransactions.isEmpty()) {
            System.out.println("No transactions found in the last " + minutes + " minutes.");
        } else {
            System.out.println("Transactions in the last " + minutes + " minutes:");
            for (Transaction transaction : recentTransactions) {
                System.out.println("- " + transaction);
            }
        }
    }
}
