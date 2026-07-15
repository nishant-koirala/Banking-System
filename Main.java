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

public class Main {

    public static void main(String[] args) {

        Bank bank = new Bank();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            printMainMenu();
            int choice = scanner.nextInt();
            System.out.println("\n-----------------------------------------------------");

            switch (choice) {
                case 1 -> handleCreateAccount(bank, scanner);
                case 2 -> handleDeposit(bank, scanner);
                case 3 -> handleWithdraw(bank, scanner);
                case 4 -> handleTransfer(bank, scanner);
                case 5 -> handlePendingTransactionMenu(bank, scanner);
                case 6 -> handleCheckBalance(bank, scanner);
                case 7 -> handleTransactionHistory(bank, scanner);
                case 8 -> handleExit(scanner);
                default -> handleInvalidChoice();
            }
        }
    }

    // ===================== MAIN MENU =====================

    private static void printMainMenu() {
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

    private static void handleCreateAccount(Bank bank, Scanner scanner) {
        System.out.print("Enter account holder name: ");
        String name = scanner.next();
        System.out.print("Enter initial balance: ");
        double balance = scanner.nextDouble();

        Account newAccount = bank.createAccount(name, balance);
        System.out.println("Account created successfully! Account ID: " + newAccount.getAccountId());
        System.out.println("\n-----------------------------------------------------");
    }

    private static void handleDeposit(Bank bank, Scanner scanner) {
        System.out.print("Enter the account ID to deposit into: ");
        String depositAccountId = scanner.next();
        System.out.print("Enter the amount to deposit: ");
        double depositAmount = scanner.nextDouble();

        try {
            bank.deposit(depositAccountId, depositAmount);
            System.out.println("Deposit successful!");
        } catch (AccountNotFoundException e) {
            System.out.println("Account not found!");
        }
        System.out.println("\n-----------------------------------------------------");
    }

    private static void handleWithdraw(Bank bank, Scanner scanner) {
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
        }
        System.out.println("\n-----------------------------------------------------");
    }

    private static void handleTransfer(Bank bank, Scanner scanner) {
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
        }
        System.out.println("\n-----------------------------------------------------");
    }

    private static void handleCheckBalance(Bank bank, Scanner scanner) {
        System.out.print("Enter the account ID to check balance: ");
        String balanceAccountId = scanner.next();

        try {
            double balanceAmount = bank.getBalance(balanceAccountId);
            System.out.println("Current balance: " + balanceAmount);
        } catch (AccountNotFoundException e) {
            System.out.println("Account not found!");
        }
    }

    private static void handleTransactionHistory(Bank bank, Scanner scanner) {
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

    private static void handleExit(Scanner scanner) {
        System.out.println("Exiting the banking system. Goodbye!");
        scanner.close();
        System.exit(0);
    }

    private static void handleInvalidChoice() {
        System.out.println("Invalid choice!");
        System.out.println("\n-----------------------------------------------------");
    }

    // ===================== PENDING TRANSACTION MENU =====================

    private static void handlePendingTransactionMenu(Bank bank, Scanner scanner) {
        while (true) {
            printPendingTransactionMenu();
            int choice = scanner.nextInt();
            System.out.println("\n-----------------------------------------------------");

            switch (choice) {
                case 1 -> handleSubmitPendingTransfer(bank, scanner);
                case 2 -> handleProcessNextPending(bank);
                case 3 -> handleProcessAllPending(bank);
                case 4 -> handlePendingCount(bank);
                case 5 -> handleProcessedLog(bank);
                case 6 -> handleRecentTransactions(bank, scanner);
                case 7 -> {
                    return;
                }
                default -> handleInvalidChoice();
            }
        }
    }

    private static void printPendingTransactionMenu() {
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

    private static void handleSubmitPendingTransfer(Bank bank, Scanner scanner) {
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
        }
        System.out.println("\n-----------------------------------------------------");
    }

    private static void handleProcessNextPending(Bank bank) {
        Transaction nextTransaction = bank.processNextPendingTransaction();
        if (nextTransaction == null) {
            System.out.println("No pending transactions to process.");
        } else {
            System.out.println("Processed transaction: " + nextTransaction);
        }
        System.out.println("\n-----------------------------------------------------");
    }

    private static void handleProcessAllPending(Bank bank) {
        bank.processAllPendingTransactions();
        System.out.println("All pending transactions processed.");
        System.out.println("\n-----------------------------------------------------");
    }

    private static void handlePendingCount(Bank bank) {
        System.out.println("Pending transactions in queue: " + bank.pendingCount());
        System.out.println("\n-----------------------------------------------------");
    }

    private static void handleProcessedLog(Bank bank) {
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

    // Only meaningful for transactions that came through the pending queue —
    // time-indexing happens in TransactionService as part of processing, so
    // this belongs with the other pending-menu handlers, not the main menu.
    private static void handleRecentTransactions(Bank bank, Scanner scanner) {
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