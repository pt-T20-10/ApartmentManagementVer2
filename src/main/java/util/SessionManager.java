package util;

import model.User;

/**
 * Session Manager Manages current logged-in user session
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    // Private constructor for Singleton pattern
    private SessionManager() {
    }

    /**
     * Get SessionManager instance (Singleton)
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Set current logged-in user
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Get current logged-in user
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Check if current user is admin
     */
    public boolean isAdmin() {
        return isLoggedIn() && currentUser.isAdmin();
    }

    /**
     * Get current username
     */
    public String getCurrentUsername() {
        return isLoggedIn() ? currentUser.getUsername() : "Guest";
    }

    /**
     * Get current full name
     */
    public String getCurrentFullName() {
        return isLoggedIn() ? currentUser.getFullName() : "Guest";
    }

    /**
     * Logout current user
     */
    public void logout() {
        this.currentUser = null;
    }

    /**
     * Clear session (for cleanup)
     */
    public void clearSession() {
        this.currentUser = null;
    }
}
