package AtmBankingSystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class User {
    private final Connection connect;
    // A scanner object for reading user inputs from the console
    private static final Scanner scanner = new Scanner(System.in);
    public User(Connection connect) {
        this.connect = connect;
    }

    /**
     * This method handles the login functionality.
     * Prompts the user for email and password, then validates them.
     *
     */
    public void handleLogin() {
        System.out.println("Please login to continue.");

        // Reading the email and password from the user
        String email = readInput("Email: ");
        String inputPassword = readInput("Password: ");

        // Validate the password against the stored password in the database
        if (validatePasswordInput( email, inputPassword)) {
            System.out.println("Login successful!");
            // If login is successful, show the account menu
            Transaction.showAccountMenu( connect, email);
        } else {
            // If login fails, inform the user
            System.out.println("Invalid email or password. Please try again.");
        }
    }

    /**
     * This method validates the email and password input.
     * It queries the database for the stored password associated with the given email.
     *
     * @param email         The email entered by the user
     * @param inputPassword The password entered by the user
     * @return true if the input password matches the stored password, otherwise false
     */
    public boolean validatePasswordInput(String email, String inputPassword) {
        // SQL query to fetch the password associated with the given email
        String query = "SELECT password FROM account WHERE email = ?";
        try (PreparedStatement stmt = connect.prepareStatement(query)) {
            // Set the email in the prepared statement
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                // If there is a result, check if the password matches
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    return storedPassword.equals(inputPassword);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error during password validation: " + e.getMessage());
        }
        // If no result is found or an error occurs, return false
        return false;
    }

    /**
     * This method handles the user registration process.
     * It prompts the user for details like full name, email, password, and phone number,
     * and then stores this information in the database if valid.
     */
    public void handleRegistration() {
        // Prompt the user for registration details
        String fullName = readInput("Full Name: ");
        String email = readInput("Email: ");
        String password = readInput("Password: ");
        String phone_number = readInput("Phone number: ");

        // Check if any input field is empty and show an error message
        if (isInputInvalid(fullName, email, password)) {
            System.out.println("All fields are required.");
            return;
        }

        // SQL query to insert the new user data into the account table
        String query = "INSERT INTO account (full_name, email, password, phone_number) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connect.prepareStatement(query)) {
            // Set the values in the prepared statement
            stmt.setString(1, fullName);
            stmt.setString(2, email);
            stmt.setString(3, password);
            stmt.setString(4, phone_number);

            // Execute the insert statement to add the user to the database
            stmt.executeUpdate();
            System.out.println("Registration successful!");
            // After successful registration, show the account menu
            Transaction.showAccountMenu(connect, email);
        } catch (SQLException e) {
            System.out.println("Error during registration: " + e.getMessage());
        }
    }

    /**
     * This method checks whether any of the required fields (full name, email, or password)
     * are empty. It returns true if any field is invalid, otherwise false.
     *
     * @param fullName The full name entered by the user
     * @param email The email entered by the user
     * @param password The password entered by the user
     * @return true if any field is invalid (empty), otherwise false
     */
    private static boolean isInputInvalid(String fullName, String email, String password) {
        return fullName.isEmpty() || email.isEmpty() || password.isEmpty();
    }

    /**
     * This method reads the user input from the console.
     * It takes a prompt message, displays it, and returns the user's input.
     *
     * @param prompt The prompt message to show to the user
     * @return The input provided by the user
     */
    private static String readInput(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine();
        // Return the input after trimming leading/trailing spaces to ensure clean data
        return input == null ? "" : input.trim();
    }
}
