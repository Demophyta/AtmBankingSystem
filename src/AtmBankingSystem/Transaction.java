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
        do {
            System.out.println("1. Withdraw Money");
            System.out.println("2. Deposit Money");
            System.out.println("3. Transfer Money");
            System.out.println("4. Check Balance");
            System.out.println("5. Log Out");
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine(); // Consume leftover newline
            handleAccountOperation(connect, choice, email); // Process the user's choice
        } while (choice != 5);
        System.out.println("You have been logged out.");
    }

    /**
     * Handles the user's account operations based on their menu choice.
     */
    private static void handleAccountOperation(Connection connect, int choice, String email) {
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
    private static void withdraw(Connection connect, String email) {
        System.out.print("Enter amount to withdraw: ");
        double amount = scanner.nextDouble();

        // Validate withdrawal amount
        if (amount <= 0 || (amount % 500 != 0 && amount % 1000 != 0)) {
            System.out.println("Invalid amount. Withdraw amount must be in multiples of 500 or 1000.");
            return;
        }

        // SQL queries for checking balance and updating account
        String checkBalanceQuery = "SELECT account_balance FROM account WHERE email = ?";
        String updateBalanceQuery = "UPDATE account SET account_balance = account_balance - ? WHERE email = ? AND account_balance >= ?";

        try (PreparedStatement checkStmt = connect.prepareStatement(checkBalanceQuery);
             PreparedStatement updateStmt = connect.prepareStatement(updateBalanceQuery)) {

            checkStmt.setString(1, email);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    double currentBalance = rs.getDouble("account_balance");
                    if (currentBalance >= amount) {
                        // Deduct amount and update balance
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
    private static void deposit(Connection connect, String email) {
        System.out.print("Enter amount to deposit: ");
        double amount = scanner.nextDouble();

        // Validate deposit amount
        if (amount <= 0 || (amount % 500 != 0 && amount % 1000 != 0)) {
            System.out.println("Invalid amount. Transfer amount must be in multiples of 500 or 1000.");
            return;
        }

        String query = "UPDATE account SET account_balance = account_balance + ? WHERE email = ?";
        try (PreparedStatement stmt = connect.prepareStatement(query)) {
            stmt.setDouble(1, amount); // Set deposit amount
            stmt.setString(2, email); // Set user's email
            stmt.executeUpdate(); // Execute update query
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
    private static void transfer(Connection connect, String email) {
        System.out.print("Enter recipient email: ");
        String recipientEmail = scanner.next();
        System.out.print("Enter amount to transfer (must be in multiples of 500 or 1000): ");
        double amount = scanner.nextDouble();

        // Validate transfer amount
        if (amount <= 0 || (amount % 500 != 0 && amount % 1000 != 0)) {
            System.out.println("Invalid amount. Transfer amount must be in multiples of 500 or 1000.");
            return;
        }

        String balanceQuery = "SELECT account_balance FROM account WHERE email = ?";
        String deductQuery = "UPDATE account SET account_balance = account_balance - ? WHERE email = ? AND account_balance >= ?";
        String transactionQuery = "INSERT INTO transactions (sender_email, recipient_email, amount, transaction_date, status) VALUES (?, ?, ?, ?, ?)";

        try {
            connect.setAutoCommit(false);

            // Check sender's balance
            double currentBalance = 0;
            try (PreparedStatement balanceStmt = connect.prepareStatement(balanceQuery)) {
                balanceStmt.setString(1, email);
                ResultSet rs = balanceStmt.executeQuery();
                if (rs.next()) {
                    currentBalance = rs.getDouble(1);
                    if (currentBalance < amount) {
                        System.out.println("Insufficient funds.");
                        logTransaction(connect, email, recipientEmail, amount, "FAILED");
                        connect.rollback();
                        return;
                    }
                } else {
                    System.out.println("Sender account not found.");
                    logTransaction(connect, email, recipientEmail, amount, "FAILED");
                    connect.rollback();
                    return;
                }
            }

            // Deduct amount from sender
            try (PreparedStatement deductStmt = connect.prepareStatement(deductQuery)) {
                deductStmt.setDouble(1, amount);
                deductStmt.setString(2, email);
                deductStmt.setDouble(3, amount);
                if (deductStmt.executeUpdate() == 0) {
                    System.out.println("Failed to deduct amount.");
                    logTransaction(connect, email, recipientEmail, amount, "FAILED");
                    connect.rollback();
                    return;
                }
            }

            // Log the successful transaction
            logTransaction(connect, email, recipientEmail, amount, "SUCCESS");

            connect.commit(); // Commit transaction
            System.out.println("Transfer successful.");
        } catch (SQLException e) {
            try {
                connect.rollback(); // Rollback on error
            } catch (SQLException rollbackEx) {
                System.out.println("Error during rollback: " + rollbackEx.getMessage());
            }
            System.out.println("Error during transfer: " + e.getMessage());
        } finally {
            try {
                connect.setAutoCommit(true); // Restore auto-commit
            } catch (SQLException ex) {
                System.out.println("Error resetting auto-commit: " + ex.getMessage());
            }
        }
    }

    /**
     * Logs a transaction in the transactions table.
     */
    private static void logTransaction(Connection connect, String senderEmail, String recipientEmail, double amount, String status) throws SQLException {
        String transactionQuery = "INSERT INTO transactions (sender_email, recipient_email, amount, transaction_date, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement transactionStmt = connect.prepareStatement(transactionQuery)) {
            transactionStmt.setString(1, senderEmail);
            transactionStmt.setString(2, recipientEmail);
            transactionStmt.setDouble(3, amount);
            transactionStmt.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis())); // Current date and time
            transactionStmt.setString(5, status);
            transactionStmt.executeUpdate();
        }
    }

    /**
     * Checks and displays the user's account balance.
     *
     * @param connect the database connection
     * @param email   the user's email
     */
    private static void checkBalance(Connection connect, String email) {
        String query = "SELECT account_balance FROM account WHERE email = ?";
        try (PreparedStatement stmt = connect.prepareStatement(query)) {
            stmt.setString(1, email); // Set email parameter
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double balance = rs.getDouble("account_balance");
                    System.out.println("Your current balance is: " + balance);
                } else {
                    System.out.println("Account not found.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking balance: " + e.getMessage());
        }
    }
}
