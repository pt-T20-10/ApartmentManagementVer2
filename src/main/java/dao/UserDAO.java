package dao;

import model.User;
import connection.Db_connection;
import util.PasswordUtil;
import util.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO class for User operations with BCrypt password hashing UPDATED: Soft
 * Delete Logic (is_deleted)
 */
public class UserDAO {

    /**
     * Authenticate user with BCrypt
     */
    public User authenticate(String username, String password) {
        // ✅ THÊM: AND u.is_deleted = 0
        String sql = "SELECT u.*, b.name AS building_name "
                + "FROM users u "
                + "LEFT JOIN buildings b ON u.building_id = b.id "
                + "WHERE u.username = ? AND u.is_active = TRUE AND u.is_deleted = 0";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                boolean isPasswordValid;

                if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
                    isPasswordValid = PasswordUtil.verifyPassword(password, storedPassword);
                    if (isPasswordValid && PasswordUtil.needsRehash(storedPassword)) {
                        updatePasswordHash(rs.getLong("id"), PasswordUtil.hashPassword(password));
                    }
                } else {
                    isPasswordValid = password.equals(storedPassword);
                    if (isPasswordValid) {
                        updatePasswordHash(rs.getLong("id"), PasswordUtil.hashPassword(password));
                    }
                }

                if (isPasswordValid) {
                    User user = mapResultSetToUser(rs);
                    updateLastLogin(user.getId());
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User getUserById(Long id) {
        String sql = "SELECT u.*, b.name AS building_name, "
                + "a.full_name AS assigned_by_name "
                + "FROM users u "
                + "LEFT JOIN buildings b ON u.building_id = b.id "
                + "LEFT JOIN users a ON u.assigned_by = a.id "
                + "WHERE u.id = ? AND u.is_deleted = 0"; // ✅ Check deleted

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT u.*, b.name AS building_name, "
                + "a.full_name AS assigned_by_name "
                + "FROM users u "
                + "LEFT JOIN buildings b ON u.building_id = b.id "
                + "LEFT JOIN users a ON u.assigned_by = a.id "
                + "WHERE u.username = ? AND u.is_deleted = 0"; // ✅ Check deleted

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateLastLogin(Long userId) {
        String sql = "UPDATE users SET last_login = NOW() WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updatePasswordHash(Long userId, String hashedPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<User> getAllUsers() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        return getAllUsers(currentUser);
    }

    /**
     * Get all users with building filter AND deleted filter
     */
    public List<User> getAllUsers(User currentUser) {
        List<User> users = new ArrayList<>();

        String sql = "SELECT u.*, b.name AS building_name, "
                + "a.full_name AS assigned_by_name "
                + "FROM users u "
                + "LEFT JOIN buildings b ON u.building_id = b.id "
                + "LEFT JOIN users a ON u.assigned_by = a.id "
                + "WHERE u.is_deleted = 0 "; // ✅ LỌC BỎ USER ĐÃ XÓA

        if (currentUser != null && currentUser.isManager() && currentUser.getBuildingId() != null) {
            sql += "AND (u.building_id = ? OR u.id = ?) ";
        }

        sql += "ORDER BY u.created_at DESC";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            if (currentUser != null && currentUser.isManager() && currentUser.getBuildingId() != null) {
                ps.setLong(1, currentUser.getBuildingId());
                ps.setLong(2, currentUser.getId());
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public List<User> getUsersByBuilding(Long buildingId) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.*, b.name AS building_name, "
                + "a.full_name AS assigned_by_name "
                + "FROM users u "
                + "LEFT JOIN buildings b ON u.building_id = b.id "
                + "LEFT JOIN users a ON u.assigned_by = a.id "
                + "WHERE u.building_id = ? AND u.is_deleted = 0 "
                + // ✅ Check deleted
                "ORDER BY u.role, u.full_name";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, buildingId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public boolean insertUser(User user) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        return insertUser(user, currentUser);
    }

    public boolean insertUser(User user, User createdBy) {
        // ✅ Thêm is_deleted = 0 mặc định
        String sql = "INSERT INTO users "
                + "(username, password, full_name, role, is_active, "
                + "building_id, assigned_by, assigned_date, is_deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), 0)";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String hashedPassword = PasswordUtil.hashPassword(user.getPassword());
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getRole());
            pstmt.setBoolean(5, user.isActive());

            if (user.getBuildingId() != null) {
                pstmt.setLong(6, user.getBuildingId());
            } else {
                pstmt.setNull(6, Types.BIGINT);
            }

            if (createdBy != null) {
                pstmt.setLong(7, createdBy.getId());
            } else {
                pstmt.setNull(7, Types.BIGINT);
            }

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE users SET "
                + "username = ?, full_name = ?, role = ?, is_active = ?, "
                + "building_id = ? "
                + "WHERE id = ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getFullName());
            pstmt.setString(3, user.getRole());
            pstmt.setBoolean(4, user.isActive());

            if (user.getBuildingId() != null) {
                pstmt.setLong(5, user.getBuildingId());
            } else {
                pstmt.setNull(5, Types.BIGINT);
            }

            pstmt.setLong(6, user.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean assignUserToBuilding(Long userId, Long buildingId, User assignedBy) {
        String sql = "UPDATE users SET building_id = ?, assigned_by = ?, assigned_date = NOW() WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            if (buildingId != null) {
                ps.setLong(1, buildingId);
            } else {
                ps.setNull(1, Types.BIGINT);
            }

            if (assignedBy != null) {
                ps.setLong(2, assignedBy.getId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }

            ps.setLong(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean removeUserFromBuilding(Long userId) {
        String sql = "UPDATE users SET building_id = NULL WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean changePassword(Long userId, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String hashedPassword = PasswordUtil.hashPassword(newPassword);
            pstmt.setString(1, hashedPassword);
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean verifyCurrentPassword(Long userId, String currentPassword) {
        String sql = "SELECT password FROM users WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
                    return PasswordUtil.verifyPassword(currentPassword, storedPassword);
                } else {
                    return currentPassword.equals(storedPassword);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<User> getAllActiveUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.*, b.name AS building_name, "
                + "a.full_name AS assigned_by_name "
                + "FROM users u "
                + "LEFT JOIN buildings b ON u.building_id = b.id "
                + "LEFT JOIN users a ON u.assigned_by = a.id "
                + "WHERE u.is_active = 1 AND u.is_deleted = 0 "
                + // ✅ Check deleted
                "ORDER BY u.full_name ASC";

        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Delete user (SOFT DELETE) ✅ Đã sửa: Chuyển is_deleted = 1 và is_active =
     * 0
     */
    public boolean deleteUser(Long userId) {
        String sql = "UPDATE users SET is_deleted = 1, is_active = 0 WHERE id = ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Permanently delete user (Hard Delete) Cẩn thận khi dùng
     */
    public boolean permanentlyDeleteUser(Long userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean usernameExists(String username, Long excludeUserId) {
        // Có thể cho phép trùng username với tài khoản đã xóa (tùy nghiệp vụ)
        // Ở đây mình vẫn check cả tài khoản đã xóa để tránh rắc rối
        String sql = excludeUserId != null
                ? "SELECT COUNT(*) FROM users WHERE username = ? AND id != ?"
                : "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            if (excludeUserId != null) {
                pstmt.setLong(2, excludeUserId);
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("is_active"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setLastLogin(rs.getTimestamp("last_login"));

        // Map building info
        Long buildingId = rs.getLong("building_id");
        user.setBuildingId(rs.wasNull() ? null : buildingId);

        try {
            user.setBuildingName(rs.getString("building_name"));
        } catch (SQLException e) {
            user.setBuildingName(null);
        }

        Long assignedBy = rs.getLong("assigned_by");
        user.setAssignedBy(rs.wasNull() ? null : assignedBy);

        try {
            user.setAssignedByName(rs.getString("assigned_by_name"));
        } catch (SQLException e) {
            user.setAssignedByName(null);
        }

        user.setAssignedDate(rs.getTimestamp("assigned_date"));

        return user;
    }

    public List<User> getAllManagers() {
        List<User> managers = new ArrayList<>();
        String sql = "SELECT u.*, b.name AS building_name, "
                + "a.full_name AS assigned_by_name "
                + "FROM users u "
                + "LEFT JOIN buildings b ON u.building_id = b.id "
                + "LEFT JOIN users a ON u.assigned_by = a.id "
                + "WHERE u.role = 'MANAGER' AND u.is_active = 1 AND u.is_deleted = 0 "
                + "ORDER BY u.full_name";
        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                managers.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return managers;
    }

    public List<User> getAvailableManagers() {
        List<User> managers = new ArrayList<>();
        String sql = "SELECT u.*, b.name AS building_name, "
                + "a.full_name AS assigned_by_name "
                + "FROM users u "
                + "LEFT JOIN buildings b ON u.building_id = b.id "
                + "LEFT JOIN users a ON u.assigned_by = a.id "
                + "WHERE u.role = 'MANAGER' AND u.is_active = 1 AND u.is_deleted = 0 "
                + "AND u.building_id IS NULL "
                + "ORDER BY u.full_name";
        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                managers.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return managers;
    }

    public List<User> getManagersForBuilding(Long currentManagerId) {
        List<User> managers = new ArrayList<>();
        String sql = "SELECT u.*, b.name AS building_name, "
                + "a.full_name AS assigned_by_name "
                + "FROM users u "
                + "LEFT JOIN buildings b ON u.building_id = b.id "
                + "LEFT JOIN users a ON u.assigned_by = a.id "
                + "WHERE u.role = 'MANAGER' AND u.is_active = 1 AND u.is_deleted = 0 "
                + "AND (u.building_id IS NULL OR u.id = ?) "
                + "ORDER BY u.full_name";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (currentManagerId != null) {
                ps.setLong(1, currentManagerId);
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                managers.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return managers;
    }
}
