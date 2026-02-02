package util;

import connection.Db_connection;
import java.sql.*;

/**
 * Database Password Migration Script Migrates all plain text passwords to
 * BCrypt hashes
 */
public class PasswordMigration {

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  PASSWORD MIGRATION TO BCRYPT");
        System.out.println("==============================================");
        System.out.println();

        int totalUsers = 0;
        int migratedUsers = 0;
        int skippedUsers = 0;
        int failedUsers = 0;

        String selectSql = "SELECT id, username, password FROM users";
        String updateSql = "UPDATE users SET password = ? WHERE id = ?";

        try (Connection conn = Db_connection.getConnection(); Statement selectStmt = conn.createStatement(); PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            ResultSet rs = selectStmt.executeQuery(selectSql);

            while (rs.next()) {
                totalUsers++;
                Long userId = rs.getLong("id");
                String username = rs.getString("username");
                String currentPassword = rs.getString("password");

                // Check if already hashed
                if (currentPassword.startsWith("$2a$")
                        || currentPassword.startsWith("$2b$")
                        || currentPassword.startsWith("$2y$")) {
                    System.out.println("⊘ SKIPPED: " + username + " (already hashed)");
                    skippedUsers++;
                    continue;
                }

                // Hash the plain text password
                try {
                    String hashedPassword = PasswordUtil.hashPassword(currentPassword);
                    updateStmt.setString(1, hashedPassword);
                    updateStmt.setLong(2, userId);
                    int updated = updateStmt.executeUpdate();

                    if (updated > 0) {
                        System.out.println("✓ MIGRATED: " + username);
                        migratedUsers++;
                    } else {
                        System.out.println("✗ FAILED: " + username + " (update failed)");
                        failedUsers++;
                    }
                } catch (Exception e) {
                    System.out.println("✗ FAILED: " + username + " (" + e.getMessage() + ")");
                    failedUsers++;
                }
            }

            System.out.println();
            System.out.println("==============================================");
            System.out.println("  MIGRATION SUMMARY");
            System.out.println("==============================================");
            System.out.println("Total users:    " + totalUsers);
            System.out.println("Migrated:       " + migratedUsers);
            System.out.println("Skipped:        " + skippedUsers);
            System.out.println("Failed:         " + failedUsers);
            System.out.println("==============================================");

            if (failedUsers > 0) {
                System.out.println();
                System.out.println("⚠ WARNING: Some migrations failed!");
            } else if (migratedUsers > 0) {
                System.out.println();
                System.out.println("✓ SUCCESS: All passwords migrated!");
            } else {
                System.out.println();
                System.out.println("ℹ INFO: No passwords needed migration.");
            }

        } catch (SQLException e) {
            System.err.println("✗ DATABASE ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
