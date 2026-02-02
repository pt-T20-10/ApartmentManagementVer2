package dao;

import model.User;
import connection.Db_connection;
import util.PasswordUtil;
import util.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO - UPDATED for Many-to-Many Buildings
 * Supports user_buildings junction table
 */
public class UserDAO {

    // ==================== AUTHENTICATION ====================

    public User authenticate(String username, String password) {
        String sql = "SELECT u.*, a.full_name AS assigned_by_name "
                + "FROM users u "
                + "LEFT JOIN users a ON u.assigned_by = a.id "
                + "WHERE u.username = ? AND u.is_active = TRUE AND u.is_deleted = 0";

        try (Connection conn = Db_connection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

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
                    loadUserBuildings(user); // ✅ NEW: Load buildings from junction table
                    updateLastLogin(user.getId());
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ==================== CRUD ====================

    public User getUserById(Long id) {
        String sql = "SELECT u.*, a.full_name AS assigned_by_name FROM users u LEFT JOIN users a ON u.assigned_by = a.id WHERE u.id = ? AND u.is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = mapResultSetToUser(rs);
                loadUserBuildings(user);
                return user;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT u.*, a.full_name AS assigned_by_name "
                + "FROM users u "
                + "LEFT JOIN users a ON u.assigned_by = a.id "
                + "WHERE u.username = ? AND u.is_deleted = 0";

        try (Connection conn = Db_connection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = mapResultSetToUser(rs);
                loadUserBuildings(user); // ✅ NEW
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getAllUsers() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        return getAllUsers(currentUser);
    }

    public List<User> getAllUsers(User currentUser) {
        List<User> users = new ArrayList<>();
        // Nếu là Manager thì chỉ hiện nhân viên thuộc tòa nhà mình quản lý
        String sql = "SELECT u.*, a.full_name AS assigned_by_name FROM users u LEFT JOIN users a ON u.assigned_by = a.id WHERE u.is_deleted = 0 ";
        
        if (currentUser != null && currentUser.isManager() && currentUser.hasBuilding()) {
            sql += "AND u.id IN (SELECT user_id FROM user_buildings WHERE building_id IN (" + buildingIdsToString(currentUser.getBuildingIds()) + ")) ";
        }
        sql += "ORDER BY u.created_at DESC";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                loadUserBuildings(user);
                users.add(user);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return users;
    }

    public List<User> getUsersByBuilding(Long buildingId) {
        List<User> users = new ArrayList<>();
        
        // ✅ NEW: Query từ user_buildings
        String sql = "SELECT u.*, a.full_name AS assigned_by_name "
                + "FROM users u "
                + "JOIN user_buildings ub ON u.id = ub.user_id "
                + "LEFT JOIN users a ON u.assigned_by = a.id "
                + "WHERE ub.building_id = ? AND u.is_deleted = 0 "
                + "ORDER BY u.role, u.full_name";

        try (Connection conn = Db_connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, buildingId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                loadUserBuildings(user); // ✅ NEW
                users.add(user);
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
        String sql = "INSERT INTO users "
                + "(username, password, full_name, role, is_active, assigned_by, is_deleted) "
                + "VALUES (?, ?, ?, ?, ?, ?, 0)";

        Connection conn = null;
        try {
            conn = Db_connection.getConnection();
            conn.setAutoCommit(false);

            // 1. Insert user
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            String hashedPassword = PasswordUtil.hashPassword(user.getPassword());
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getRole());
            pstmt.setBoolean(5, user.isActive());

            if (createdBy != null) {
                pstmt.setLong(6, createdBy.getId());
            } else {
                pstmt.setNull(6, Types.BIGINT);
            }

            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                conn.rollback();
                return false;
            }

            // 2. Get generated user ID
            ResultSet keys = pstmt.getGeneratedKeys();
            if (!keys.next()) {
                conn.rollback();
                return false;
            }
            Long userId = keys.getLong(1);
            user.setId(userId);

            // 3. ✅ NEW: Insert into user_buildings nếu có buildingIds
            if (user.getBuildingIds() != null && !user.getBuildingIds().isEmpty()) {
                if (!assignUserToBuildings(conn, userId, user.getBuildingIds(), 
                        createdBy != null ? createdBy.getId() : null)) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE users SET username = ?, full_name = ?, role = ?, is_active = ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = Db_connection.getConnection();
            conn.setAutoCommit(false);

            // 1. Update bảng users
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getFullName());
            pstmt.setString(3, user.getRole());
            pstmt.setBoolean(4, user.isActive());
            pstmt.setLong(5, user.getId());

            if (pstmt.executeUpdate() == 0) {
                conn.rollback();
                return false;
            }

            // 2. ✅ QUAN TRỌNG: Cập nhật bảng user_buildings
            // Nếu danh sách buildingIds không null -> Có thay đổi phân công
            if (user.getBuildingIds() != null) {
                // Xóa phân công cũ
                PreparedStatement delStmt = conn.prepareStatement("DELETE FROM user_buildings WHERE user_id = ?");
                delStmt.setLong(1, user.getId());
                delStmt.executeUpdate();

                // Thêm phân công mới
                if (!user.getBuildingIds().isEmpty()) {
                    User currentUser = SessionManager.getInstance().getCurrentUser();
                    Long assignedBy = currentUser != null ? currentUser.getId() : null;
                    
                    if (!assignUserToBuildings(conn, user.getId(), user.getBuildingIds(), assignedBy)) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public boolean deleteUser(Long userId) {
        String sql = "UPDATE users SET is_deleted = 1, is_active = 0 WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // ==================== BUILDING ASSIGNMENT (NEW) ====================

    private boolean assignUserToBuildings(Connection conn, Long userId, List<Long> buildingIds, Long assignedBy) 
            throws SQLException {
        String sql = "INSERT INTO user_buildings (user_id, building_id, assigned_by, assigned_date) VALUES (?, ?, ?, NOW())";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Long buildingId : buildingIds) {
                pstmt.setLong(1, userId);
                pstmt.setLong(2, buildingId);
                if (assignedBy != null) {
                    pstmt.setLong(3, assignedBy);
                } else {
                    pstmt.setNull(3, Types.BIGINT);
                }
                pstmt.addBatch();
            }
            
            int[] results = pstmt.executeBatch();
            for (int result : results) {
                if (result == Statement.EXECUTE_FAILED) return false;
            }
            return true;
        }
    }
    /**
     * Update buildings cho user (public method cho UI gọi)
     */
    public boolean updateUserBuildings(Long userId, List<Long> buildingIds, Long assignedBy) {
        Connection conn = null;
        try {
            conn = Db_connection.getConnection();
            conn.setAutoCommit(false);

            // Delete old
            PreparedStatement delStmt = conn.prepareStatement("DELETE FROM user_buildings WHERE user_id = ?");
            delStmt.setLong(1, userId);
            delStmt.executeUpdate();

            // Insert new
            if (buildingIds != null && !buildingIds.isEmpty()) {
                if (!assignUserToBuildings(conn, userId, buildingIds, assignedBy)) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    
    private void loadUserBuildings(User user) {
        if (user == null || user.getId() == null) return;
        if (user.isAdmin()) return;

        List<Long> buildingIds = new ArrayList<>();
        List<String> buildingNames = new ArrayList<>();

        // Lấy từ bảng user_buildings VÀ cả bảng buildings (nếu là manager cũ)
        String sql = "SELECT DISTINCT b.id, b.name " +
                     "FROM buildings b " +
                     "LEFT JOIN user_buildings ub ON b.id = ub.building_id " +
                     "WHERE (ub.user_id = ? OR b.manager_user_id = ?) " +
                     "AND b.is_deleted = 0 ORDER BY b.name";

        try (Connection conn = Db_connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, user.getId());
            ps.setLong(2, user.getId());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                buildingIds.add(rs.getLong("id"));
                buildingNames.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        user.setBuildingIds(buildingIds);
        user.setBuildingNames(buildingNames);
    }

    // ==================== HELPERS ====================

    private void updateLastLogin(Long userId) {
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET last_login = NOW() WHERE id = ?")) {
            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updatePasswordHash(Long userId, String hash) {
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET password = ? WHERE id = ?")) {
            pstmt.setString(1, hash);
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public boolean changePassword(Long userId, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, PasswordUtil.hashPassword(newPassword));
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public boolean verifyCurrentPassword(Long userId, String currentPassword) {
        String sql = "SELECT password FROM users WHERE id = ?";
        try (Connection conn = Db_connection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        String sql = "SELECT u.*, a.full_name AS assigned_by_name "
                + "FROM users u "
                + "LEFT JOIN users a ON u.assigned_by = a.id "
                + "WHERE u.is_active = 1 AND u.is_deleted = 0 "
                + "ORDER BY u.full_name ASC";

        try (Connection conn = Db_connection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                loadUserBuildings(user);
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public List<User> getAllManagers() {
        List<User> managers = new ArrayList<>();
        String sql = "SELECT u.*, a.full_name AS assigned_by_name "
                + "FROM users u "
                + "LEFT JOIN users a ON u.assigned_by = a.id "
                + "WHERE u.role = 'MANAGER' AND u.is_active = 1 AND u.is_deleted = 0 "
                + "ORDER BY u.full_name";
        try (Connection conn = Db_connection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                loadUserBuildings(user);
                managers.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return managers;
    }

    public boolean usernameExists(String username, Long excludeUserId) {
        String sql = excludeUserId != null
                ? "SELECT COUNT(*) FROM users WHERE username = ? AND id != ?"
                : "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = Db_connection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        user.setAssignedBy(rs.getLong("assigned_by"));
        if (rs.wasNull()) user.setAssignedBy(null);
        try { user.setAssignedByName(rs.getString("assigned_by_name")); } catch (SQLException e) { user.setAssignedByName(null); }
        return user;
    }

    private String buildingIdsToString(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return "0";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(ids.get(i));
        }
        return sb.toString();
    }
    
    /**
     * ✅ Xóa vĩnh viễn user khỏi database
     * Dùng cho tính năng dọn dẹp dữ liệu trong UserManagementPanel
     */
    public boolean permanentlyDeleteUser(Long userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = Db_connection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}