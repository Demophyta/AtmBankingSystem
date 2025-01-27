package AtmBankingSystem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * This is the main class for the ATM banking system.
 * It handles the main menu and establishes the database connection.
 */
public class Main{
    // Scanner for user input
    private static final Scanner scanner = new Scanner(System.in);

    // Entry point of the application
    public static void main(String[] args) {
        // Database connection details
        String url = "jdbc:mysql://localhost:3306/accounts"; // Database URL
        String username = "root";
        String password = "3310";

        // Attempt to establish a connection to the database
        try (Connection connect = DriverManager.getConnection(url, username, password)) {
            System.out.println("Welcome to our ATM banking system!");
            // Create a User instance and pass the database connection
            User user = new User(connect);

            // Main application loop
            while (true) {
                // Display the main menu to the user
                System.out.println("Main Menu:");
                System.out.println("1. Create an account");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Enter your choice: ");

                // Read user choice and handle invalid input
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline character to avoid input issues


                // Handle user choice using a switch statement
                switch (choice) {
                    case 1:
                        // Call the method to handle user registration
                        user.handleRegistration();
                        break;
                    case 2:
                        // Call the method to handle user login
                       user.handleLogin();
                        break;
                    case 3:
                        // Exit the application
                        System.out.println("Thank you for using our banking ATM system! Goodbye!");
                        System.exit(0);
                    default:
                        // Handle invalid menu options
                        System.out.println("Enter a valid choice!");
                }
            }
        } catch (SQLException e) {
            // Handle exceptions related to database connection
            System.out.println("Failed to connect to the database: " + e.getMessage());
        }
    }
}
