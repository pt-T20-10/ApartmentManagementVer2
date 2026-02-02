package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password Utility Class Handles password hashing and verification using BCrypt
 */
public class PasswordUtil {

    // BCrypt work factor (cost factor)
    // 12 is a good balance between security and performance
    private static final int WORK_FACTOR = 12;

    /**
     * Hash a plain text password using BCrypt
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    /**
     * Verify a plain text password against a hashed password
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Check if a password needs rehashing
     */
    public static boolean needsRehash(String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            return true;
        }
        try {
            String[] parts = hashedPassword.split("\\$");
            if (parts.length < 4) {
                return true;
            }
            int currentWorkFactor = Integer.parseInt(parts[2]);
            return currentWorkFactor < WORK_FACTOR;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Validate password strength
     */
    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        return hasUpperCase && hasLowerCase && hasDigit;
    }

    /**
     * Get password strength description
     */
    public static String getPasswordStrengthDescription(String password) {
        if (password == null || password.isEmpty()) {
            return "Mật khẩu không được để trống";
        }
        if (password.length() < 6) {
            return "Mật khẩu quá ngắn (tối thiểu 6 ký tự)";
        }
        if (password.length() < 8) {
            return "Mật khẩu yếu";
        }

        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        int strength = 0;
        if (hasUpperCase) {
            strength++;
        }
        if (hasLowerCase) {
            strength++;
        }
        if (hasDigit) {
            strength++;
        }
        if (hasSpecial) {
            strength++;
        }

        if (strength >= 4 && password.length() >= 12) {
            return "Mật khẩu rất mạnh";
        } else if (strength >= 3) {
            return "Mật khẩu mạnh";
        } else if (strength >= 2) {
            return "Mật khẩu trung bình";
        } else {
            return "Mật khẩu yếu - nên thêm chữ hoa, số hoặc ký tự đặc biệt";
        }
    }
}
