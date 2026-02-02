package dao;

import model.ContractService;
import connection.Db_connection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO class for ContractService operations Manages services applied to
 * contracts Updated: Removed 'is_active' column logic to match database schema.
 */
public class ContractServiceDAO {

    // --- HELPER: Map ResultSet to ContractService ---
    private ContractService mapResultSetToContractService(ResultSet rs) throws SQLException {
        ContractService cs = new ContractService();
        cs.setId(rs.getLong("id"));
        cs.setContractId(rs.getLong("contract_id"));
        cs.setServiceId(rs.getLong("service_id"));

        java.sql.Date appliedDate = rs.getDate("applied_date");
        if (appliedDate != null) {
            cs.setAppliedDate(new java.util.Date(appliedDate.getTime()));
        }

        cs.setUnitPrice(rs.getBigDecimal("unit_price"));
        // Mặc định là active vì không còn cột status trong DB
        cs.setActive(true);

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            cs.setCreatedAt(new java.util.Date(createdAt.getTime()));
        }

        return cs;
    }

    // --- HELPER: Map ResultSet with Service JOIN ---
    private ContractService mapResultSetWithService(ResultSet rs) throws SQLException {
        ContractService cs = mapResultSetToContractService(rs);

        // Additional fields from JOIN with services table
        try {
            cs.setServiceName(rs.getString("service_name"));
            cs.setUnitType(rs.getString("unit_type"));
        } catch (SQLException e) {
            // Ignore if columns don't exist (not joined)
        }

        return cs;
    }

    // --- GET SERVICES BY CONTRACT ---
    public List<ContractService> getServicesByContract(Long contractId) {
        List<ContractService> services = new ArrayList<>();
        // Đã xóa is_active khỏi ORDER BY
        String sql = "SELECT cs.*, s.service_name, s.unit_type "
                + "FROM contract_services cs "
                + "INNER JOIN services s ON cs.service_id = s.id "
                + "WHERE cs.contract_id = ? "
                + "ORDER BY cs.applied_date DESC";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, contractId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    services.add(mapResultSetWithService(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return services;
    }

    // --- GET ACTIVE SERVICES BY CONTRACT ---
    // Vì không có is_active, hàm này trả về tất cả dịch vụ của hợp đồng
    public List<ContractService> getActiveServicesByContract(Long contractId) {
        List<ContractService> services = new ArrayList<>();
        String sql = "SELECT cs.*, s.service_name, s.unit_type "
                + "FROM contract_services cs "
                + "INNER JOIN services s ON cs.service_id = s.id "
                + "WHERE cs.contract_id = ? "
                + "ORDER BY s.service_name";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, contractId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    services.add(mapResultSetWithService(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return services;
    }

    // --- GET CONTRACT SERVICE BY ID ---
    public ContractService getById(Long id) {
        String sql = "SELECT cs.*, s.service_name, s.unit_type "
                + "FROM contract_services cs "
                + "INNER JOIN services s ON cs.service_id = s.id "
                + "WHERE cs.id = ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetWithService(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- INSERT CONTRACT SERVICE ---
    public boolean insert(ContractService contractService) {
        // Đã xóa is_active khỏi câu lệnh INSERT
        String sql = "INSERT INTO contract_services "
                + "(contract_id, service_id, applied_date, unit_price) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, contractService.getContractId());
            pstmt.setLong(2, contractService.getServiceId());
            pstmt.setDate(3, new java.sql.Date(contractService.getAppliedDate().getTime()));
            pstmt.setBigDecimal(4, contractService.getUnitPrice());

            int affected = pstmt.executeUpdate();

            if (affected > 0) {
                // Get generated ID
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        contractService.setId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- BATCH INSERT SERVICES FOR CONTRACT ---
    public boolean insertServicesForContract(Long contractId, List<Long> serviceIds,
            java.util.Date appliedDate) {
        String sqlGetPrice = "SELECT unit_price FROM services WHERE id = ?";
        // Đã xóa is_active khỏi câu lệnh INSERT
        String sqlInsert = "INSERT INTO contract_services "
                + "(contract_id, service_id, applied_date, unit_price) "
                + "VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = Db_connection.getConnection();
            conn.setAutoCommit(false);

            for (Long serviceId : serviceIds) {
                // Get current price
                java.math.BigDecimal unitPrice = null;
                try (PreparedStatement pstGet = conn.prepareStatement(sqlGetPrice)) {
                    pstGet.setLong(1, serviceId);
                    try (ResultSet rs = pstGet.executeQuery()) {
                        if (rs.next()) {
                            unitPrice = rs.getBigDecimal("unit_price");
                        }
                    }
                }

                if (unitPrice == null) {
                    continue; // Skip if service not found
                }

                // Insert contract service
                try (PreparedStatement pstInsert = conn.prepareStatement(sqlInsert)) {
                    pstInsert.setLong(1, contractId);
                    pstInsert.setLong(2, serviceId);
                    pstInsert.setDate(3, new java.sql.Date(appliedDate.getTime()));
                    pstInsert.setBigDecimal(4, unitPrice);
                    pstInsert.executeUpdate();
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
        }
        return false;
    }

    // --- DELETE ALL SERVICES FOR CONTRACT (Used in Update) ---
    public boolean deleteServicesByContract(Long contractId) {
        String sql = "DELETE FROM contract_services WHERE contract_id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, contractId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- UPDATE CONTRACT SERVICE ---
    public boolean update(ContractService contractService) {
        // Đã xóa is_active khỏi câu lệnh UPDATE
        String sql = "UPDATE contract_services SET "
                + "service_id = ?, applied_date = ?, unit_price = ? "
                + "WHERE id = ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, contractService.getServiceId());
            pstmt.setDate(2, new java.sql.Date(contractService.getAppliedDate().getTime()));
            pstmt.setBigDecimal(3, contractService.getUnitPrice());
            pstmt.setLong(4, contractService.getId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- DELETE CONTRACT SERVICE (HARD DELETE) ---
    public boolean delete(Long id) {
        String sql = "DELETE FROM contract_services WHERE id = ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- CHECK IF SERVICE EXISTS FOR CONTRACT ---
    public boolean hasService(Long contractId, Long serviceId) {
        // Đã xóa check is_active
        String sql = "SELECT COUNT(*) FROM contract_services "
                + "WHERE contract_id = ? AND service_id = ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, contractId);
            pstmt.setLong(2, serviceId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- COUNT ACTIVE SERVICES FOR CONTRACT ---
    public int countActiveServices(Long contractId) {
        // Đếm tất cả dịch vụ (vì không còn is_active)
        String sql = "SELECT COUNT(*) FROM contract_services "
                + "WHERE contract_id = ?";

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
}
