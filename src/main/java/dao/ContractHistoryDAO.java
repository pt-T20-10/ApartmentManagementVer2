package dao;

import model.ContractHistory;
import connection.Db_connection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO class for ContractHistory operations UPDATED: Added JOIN with users table
 * to get creator name
 */
public class ContractHistoryDAO {

    // --- HELPER: Map ResultSet to ContractHistory ---
    private ContractHistory mapResultSetToHistory(ResultSet rs) throws SQLException {
        ContractHistory history = new ContractHistory();
        history.setId(rs.getLong("id"));
        history.setContractId(rs.getLong("contract_id"));
        history.setAction(rs.getString("action"));
        history.setOldValue(rs.getString("old_value"));
        history.setNewValue(rs.getString("new_value"));

        java.sql.Date oldEndDate = rs.getDate("old_end_date");
        if (oldEndDate != null) {
            history.setOldEndDate(new java.util.Date(oldEndDate.getTime()));
        }

        java.sql.Date newEndDate = rs.getDate("new_end_date");
        if (newEndDate != null) {
            history.setNewEndDate(new java.util.Date(newEndDate.getTime()));
        }

        history.setReason(rs.getString("reason"));

        // Handle created_by (may be NULL)
        long createdBy = rs.getLong("created_by");
        if (!rs.wasNull()) {
            history.setCreatedBy(createdBy);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            history.setCreatedAt(new java.util.Date(createdAt.getTime()));
        }

        return history;
    }

    // --- GET HISTORY BY CONTRACT (Original method - kept for compatibility) ---
    public List<ContractHistory> getHistoryByContract(Long contractId) {
        List<ContractHistory> histories = new ArrayList<>();
        String sql = "SELECT * FROM contract_history "
                + "WHERE contract_id = ? "
                + "ORDER BY created_at DESC";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, contractId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    histories.add(mapResultSetToHistory(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return histories;
    }

    // --- GET HISTORY BY CONTRACT WITH USER INFO (NEW - JOIN users table) ---
    public List<ContractHistory> getHistoryByContractWithUser(Long contractId) {
        List<ContractHistory> histories = new ArrayList<>();
        String sql = "SELECT ch.*, u.full_name AS created_by_name "
                + "FROM contract_history ch "
                + "LEFT JOIN users u ON ch.created_by = u.id "
                + "WHERE ch.contract_id = ? "
                + "ORDER BY ch.created_at DESC";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, contractId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ContractHistory history = mapResultSetToHistory(rs);

                    // Set user name from JOIN
                    String createdByName = rs.getString("created_by_name");
                    history.setCreatedByName(createdByName != null ? createdByName : "Hệ thống");

                    histories.add(history);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return histories;
    }

    // --- INSERT HISTORY ---
    public boolean insert(Connection conn, ContractHistory history) throws SQLException {
        String sql = "INSERT INTO contract_history "
                + "(contract_id, action, old_value, new_value, old_end_date, new_end_date, "
                + "reason, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, history.getContractId());
            pstmt.setString(2, history.getAction());
            pstmt.setString(3, history.getOldValue());
            pstmt.setString(4, history.getNewValue());

            if (history.getOldEndDate() != null) {
                pstmt.setDate(5, new java.sql.Date(history.getOldEndDate().getTime()));
            } else {
                pstmt.setNull(5, Types.DATE);
            }

            if (history.getNewEndDate() != null) {
                pstmt.setDate(6, new java.sql.Date(history.getNewEndDate().getTime()));
            } else {
                pstmt.setNull(6, Types.DATE);
            }

            pstmt.setString(7, history.getReason());

            if (history.getCreatedBy() != null) {
                pstmt.setLong(8, history.getCreatedBy());
            } else {
                pstmt.setNull(8, Types.BIGINT);
            }

            int affected = pstmt.executeUpdate();

            if (affected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        history.setId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }

            return false;
        }
    }

    public boolean insert(ContractHistory history) {
        String sql = "INSERT INTO contract_history "
                + "(contract_id, action, old_value, new_value, old_end_date, new_end_date, "
                + "reason, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, history.getContractId());
            pstmt.setString(2, history.getAction());
            pstmt.setString(3, history.getOldValue());
            pstmt.setString(4, history.getNewValue());

            if (history.getOldEndDate() != null) {
                pstmt.setDate(5, new java.sql.Date(history.getOldEndDate().getTime()));
            } else {
                pstmt.setNull(5, Types.DATE);
            }

            if (history.getNewEndDate() != null) {
                pstmt.setDate(6, new java.sql.Date(history.getNewEndDate().getTime()));
            } else {
                pstmt.setNull(6, Types.DATE);
            }

            pstmt.setString(7, history.getReason());

            if (history.getCreatedBy() != null) {
                pstmt.setLong(8, history.getCreatedBy());
            } else {
                pstmt.setNull(8, Types.BIGINT);
            }

            int affected = pstmt.executeUpdate();

            if (affected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        history.setId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- COUNT HISTORY BY CONTRACT ---
    public int countByContract(Long contractId) {
        String sql = "SELECT COUNT(*) FROM contract_history WHERE contract_id = ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, contractId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // --- GET RECENT HISTORY (LIMIT N) ---
    public List<ContractHistory> getRecentHistory(int limit) {
        List<ContractHistory> histories = new ArrayList<>();
        String sql = "SELECT ch.*, u.full_name AS created_by_name "
                + "FROM contract_history ch "
                + "LEFT JOIN users u ON ch.created_by = u.id "
                + "ORDER BY ch.created_at DESC "
                + "LIMIT ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ContractHistory history = mapResultSetToHistory(rs);
                    String createdByName = rs.getString("created_by_name");
                    history.setCreatedByName(createdByName != null ? createdByName : "Hệ thống");
                    histories.add(history);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return histories;
    }

    // --- COUNT RECENT TERMINATIONS (within N days) ---
    public int countRecentTerminations(int days) {
        String sql = "SELECT COUNT(*) FROM contract_history "
                + "WHERE action = 'TERMINATED' "
                + "AND created_at >= DATE_SUB(NOW(), INTERVAL ? DAY)";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, days);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // --- COUNT ACTIONS BY TYPE (for statistics) ---
    public int countByAction(String action) {
        String sql = "SELECT COUNT(*) FROM contract_history WHERE action = ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, action);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
