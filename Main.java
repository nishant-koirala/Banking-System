import services.Bank;

/**
 * Entry point for the banking system application.
 * Creates the business logic layer (Bank) and UI layer (UserInterface),
 * then starts the UI loop.
 */
public class Main {
    public static void main(String[] args) {
        Bank bank = new Bank();
        UserInterface ui = new UserInterface(bank);
        ui.start();
    }
}