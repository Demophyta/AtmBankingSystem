package AtmBankingSystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Transaction {
    private static final Scanner scanner = new Scanner(System.in);

    /**
     * Displays the account menu and allows the user to perform various operations.
     *
     * @param connect the database connection
     * @param email   the user's email
     */
    public static void showAccountMenu(Connection connect, String email) {
        int choice;
        Transaction transaction = new Transaction(); // Create an instance of Transaction
        do {
            System.out.println("1. Withdraw Money");
            System.out.println("2. Deposit Money");
            System.out.println("3. Transfer Money");
            System.out.println("4. Check Balance");
            System.out.println("5. Log Out");
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine(); // Consume leftover newline

            // Process the user's choice using the instance
            transaction.handleAccountOperation(connect, choice, email);
        } while (choice != 5);
        System.out.println("You have been logged out.");
    }

    /**
     * Handles the user's account operations based on their menu choice.
     *
     * @param connect the database connection
     * @param choice  the user's choice
     * @param email   the user's email
     */
    private void handleAccountOperation(Connection connect, int choice, String email) {
        switch (choice) {
            case 1:
                withdraw(connect, email); // Perform withdrawal
                break;
            case 2:
                deposit(connect, email); // Perform deposit
                break;
            case 3:
                transfer(connect, email); // Perform transfer
                break;
            case 4:
                checkBalance(connect, email); // Check account balance
                break;
            case 5:
                break; // Log out
            default:
                System.out.println("Enter a valid choice!");
                break;
        }
    }

    /**
     * Handles withdrawal of money from the user's account.
     *
     * @param connect the database connection
     * @param email   the user's email
     */
    private void withdraw(Connection connect, String email) {
        System.out.print("Enter amount to withdraw: ");
        double amount = scanner.nextDouble();

        // Validate withdrawal amount
        if (amount <= 0 || (amount % 500 != 0 && amount % 1000 != 0)) {
            System.out.println("Invalid amount. Withdraw amount must be in multiples of 500 or 1000.");
            return;
        }

        String checkBalanceQuery = "SELECT account_balance FROM account WHERE email = ?";
        String updateBalanceQuery = "UPDATE account SET account_balance = account_balance - ? WHERE email = ? AND account_balance >= ?";

        try (PreparedStatement checkStmt = connect.prepareStatement(checkBalanceQuery);
             PreparedStatement updateStmt = connect.prepareStatement(updateBalanceQuery)) {

            checkStmt.setString(1, email);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    double currentBalance = rs.getDouble("account_balance");
                    if (currentBalance >= amount) {
                        updateStmt.setDouble(1, amount);
                        updateStmt.setString(2, email);
                        updateStmt.setDouble(3, amount);
                        updateStmt.executeUpdate();
                        System.out.println("Withdrawal successful. New balance: " + (currentBalance - amount));
                    } else {
                        System.out.println("Insufficient funds.");
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Error during withdrawal: " + e.getMessage());
        }
    }

    /**
     * Handles deposit of money into the user's account.
     *
     * @param connect the database connection
     * @param email   the user's email
     */
    private void deposit(Connection connect, String email) {
        System.out.print("Enter amount to deposit: ");
        double amount = scanner.nextDouble();

        if (amount <= 0 || (amount % 500 != 0 && amount % 1000 != 0)) {
            System.out.println("Invalid amount. Deposit amount must be in multiples of 500 or 1000.");
            return;
        }

        String query = "UPDATE account SET account_balance = account_balance + ? WHERE email = ?";
        try (PreparedStatement stmt = connect.prepareStatement(query)) {
            stmt.setDouble(1, amount);
            stmt.setString(2, email);
            stmt.executeUpdate();
            System.out.println("Deposit successful.");
        } catch (SQLException e) {
            System.out.println("Error during deposit: " + e.getMessage());
        }
    }

    /**
     * Handles transfer of money between accounts.
     *
     * @param connect the database connection
     * @param email   the sender's email
     */
    private void transfer(Connection connect, String email) {
        System.out.print("Enter recipient email: ");
        String recipientEmail = scanner.next();
        System.out.print("Enter amount to transfer: ");
        double amount = scanner.nextDouble();

        if (amount <= 0 || (amount % 500 != 0 && amount % 1000 != 0)) {
            System.out.println("Invalid amount. Transfer amount must be in multiples of 500 or 1000.");
            return;
        }

        String balanceQuery = "SELECT account_balance FROM account WHERE email = ?";
        String deductQuery = "UPDATE account SET account_balance = account_balance - ? WHERE email = ? AND account_balance >= ?";
        String addQuery = "UPDATE account SET account_balance = account_balance + ? WHERE email = ?";
        try {
            connect.setAutoCommit(false);

            try (PreparedStatement balanceStmt = connect.prepareStatement(balanceQuery);
                 PreparedStatement deductStmt = connect.prepareStatement(deductQuery);
                 PreparedStatement addStmt = connect.prepareStatement(addQuery)) {

                balanceStmt.setString(1, email);
                ResultSet rs = balanceStmt.executeQuery();
                if (rs.next()) {
                    double currentBalance = rs.getDouble("account_balance");
                    if (currentBalance < amount) {
                        System.out.println("Insufficient funds.");
                        connect.rollback();
                        return;
                    }
                }

                deductStmt.setDouble(1, amount);
                deductStmt.setString(2, email);
                deductStmt.setDouble(3, amount);
                deductStmt.executeUpdate();

                addStmt.setDouble(1, amount);
                addStmt.setString(2, recipientEmail);
                addStmt.executeUpdate();

                connect.commit();
                System.out.println("Transfer successful.");
            }
        } catch (SQLException e) {
            try {
                connect.rollback();
            } catch (SQLException ex) {
                System.out.println("Error during rollback: " + ex.getMessage());
            }
            System.out.println("Error during transfer: " + e.getMessage());
        } finally {
            try {
                connect.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Error resetting auto-commit: " + e.getMessage());
            }
        }
    }

    /**
     * Checks and displays the user's account balance.
     *
     * @param connect the database connection
     * @param email   the user's email
     */
    private void checkBalance(Connection connect, String email) {
        String query = "SELECT account_balance FROM account WHERE email = ?";
        try (PreparedStatement stmt = connect.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("account_balance");
                System.out.println("Your current balance is: " + balance);
            } else {
                System.out.println("Account not found.");
            }
        } catch (SQLException e) {
            System.out.println("Error checking balance: " + e.getMessage());
        }
    }
}
