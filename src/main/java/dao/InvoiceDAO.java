package dao;

import model.Invoice;
import model.InvoiceDetail;
import connection.Db_connection;
import util.SessionManager;
import model.User;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InvoiceDAO {

    // =============================================================
    // PHẦN 1: CÁC HÀM CRUD CƠ BẢN
    // =============================================================

    public List<Invoice> getAllInvoices() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        List<Invoice> invoices = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder("SELECT i.* FROM invoices i "
                + "JOIN contracts c ON i.contract_id = c.id "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE i.is_deleted = 0 ");

        boolean needsFilter = currentUser != null && !currentUser.isAdmin() && currentUser.hasBuilding();
        List<Long> bIds = needsFilter ? currentUser.getBuildingIds() : new ArrayList<>();

        if (needsFilter) {
            String placeholders = bIds.stream().map(id -> "?").collect(Collectors.joining(","));
            sql.append("AND f.building_id IN (").append(placeholders).append(") ");
        }

        sql.append("ORDER BY i.year DESC, i.month DESC, i.created_at DESC");

        try (Connection conn = Db_connection.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (needsFilter) {
                for (int i = 0; i < bIds.size(); i++) {
                    ps.setLong(i + 1, bIds.get(i));
                }
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                invoices.add(mapRowToInvoice(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return invoices;
    }

    public Invoice getInvoiceById(Long id) {
        String sql = "SELECT * FROM invoices WHERE id = ? AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToInvoice(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Invoice> getInvoicesByMonth(int month, int year) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        List<Invoice> invoices = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder("SELECT i.* FROM invoices i "
                + "JOIN contracts c ON i.contract_id = c.id "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE i.month = ? AND i.year = ? AND i.is_deleted = 0 ");

        boolean needsFilter = currentUser != null && !currentUser.isAdmin() && currentUser.hasBuilding();
        List<Long> bIds = needsFilter ? currentUser.getBuildingIds() : new ArrayList<>();

        if (needsFilter) {
            String placeholders = bIds.stream().map(id -> "?").collect(Collectors.joining(","));
            sql.append("AND f.building_id IN (").append(placeholders).append(") ");
        }

        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            
            if (needsFilter) {
                for (int i = 0; i < bIds.size(); i++) {
                    ps.setLong(i + 3, bIds.get(i));
                }
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                invoices.add(mapRowToInvoice(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return invoices;
    }
    
    public List<Invoice> getInvoicesByMonth(Integer month, Integer year) {
        if (month == null || year == null) return getAllInvoices();
        return getInvoicesByMonth(month.intValue(), year.intValue());
    }

    public Invoice getLatestInvoiceByApartmentId(Long apartmentId) {
        String sql = "SELECT i.* FROM invoices i "
                + "JOIN contracts c ON i.contract_id = c.id "
                + "WHERE c.apartment_id = ? AND i.is_deleted = 0 "
                + "ORDER BY i.year DESC, i.month DESC LIMIT 1";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, apartmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToInvoice(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insertInvoice(Invoice invoice) {
        return insertInvoiceAndReturnId(invoice) != null;
    }

    public Long insertInvoiceAndReturnId(Invoice invoice) {
        String sql = "INSERT INTO invoices (contract_id, month, year, total_amount, status, created_at, is_deleted) "
                + "VALUES (?, ?, ?, ?, ?, NOW(), 0)";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, invoice.getContractId());
            ps.setInt(2, invoice.getMonth());
            ps.setInt(3, invoice.getYear());
            ps.setBigDecimal(4, invoice.getTotalAmount());
            ps.setString(5, invoice.getStatus());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateInvoice(Invoice invoice) {
        String sql = "UPDATE invoices SET total_amount = ?, status = ?, payment_date = ? WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, invoice.getTotalAmount());
            ps.setString(2, invoice.getStatus());
            
            if (invoice.getPaymentDate() != null) {
                ps.setTimestamp(3, new Timestamp(invoice.getPaymentDate().getTime()));
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }
            
            ps.setLong(4, invoice.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteInvoice(Long id) {
        String sql = "UPDATE invoices SET is_deleted = 1 WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =============================================================
    // PHẦN 2: INVOICE DETAILS
    // =============================================================

    public boolean insertInvoiceDetails(Long invoiceId, List<InvoiceDetail> details) {
        String sql = "INSERT INTO invoice_details (invoice_id, service_name, amount) VALUES (?, ?, ?)";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (InvoiceDetail detail : details) {
                ps.setLong(1, invoiceId);
                ps.setString(2, detail.getServiceName());
                ps.setBigDecimal(3, detail.getAmount());
                ps.addBatch();
            }
            ps.executeBatch();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<InvoiceDetail> getInvoiceDetails(Long invoiceId) {
        List<InvoiceDetail> details = new ArrayList<>();
        String sql = "SELECT * FROM invoice_details WHERE invoice_id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InvoiceDetail detail = new InvoiceDetail();
                    detail.setId(rs.getLong("id"));
                    detail.setInvoiceId(rs.getLong("invoice_id"));
                    detail.setServiceName(rs.getString("service_name"));
                    detail.setAmount(rs.getBigDecimal("amount"));
                    details.add(detail);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return details;
    }
    
    public void deleteInvoiceDetails(Long invoiceId) {
        String sql = "DELETE FROM invoice_details WHERE invoice_id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =============================================================
    // PHẦN 3: DASHBOARD STATISTICS (Đã Fix NullPointerException)
    // =============================================================

    public BigDecimal getTotalRevenue() {
        String sql = "SELECT SUM(total_amount) FROM invoices WHERE status = 'PAID' AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getBigDecimal(1) != null ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        } catch (SQLException e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    public int countUnpaidInvoices() {
        String sql = "SELECT COUNT(*) FROM invoices WHERE status = 'UNPAID' AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ✅ FIXED: Check null để tránh NPE
    public BigDecimal getTotalRevenueByBuilding(Long buildingId) {
        if (buildingId == null) return getTotalRevenue(); // Fallback nếu null
        
        String sql = "SELECT SUM(i.total_amount) FROM invoices i "
                + "JOIN contracts c ON i.contract_id = c.id "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE i.status = 'PAID' AND i.is_deleted = 0 AND f.building_id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, buildingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1) != null ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    // ✅ FIXED: Check null để tránh NPE
    public int countUnpaidInvoicesByBuilding(Long buildingId) {
        if (buildingId == null) return countUnpaidInvoices(); // Fallback nếu null

        String sql = "SELECT COUNT(*) FROM invoices i "
                + "JOIN contracts c ON i.contract_id = c.id "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE i.status = 'UNPAID' AND i.is_deleted = 0 AND f.building_id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, buildingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ✅ FIXED: Check null để tránh NPE
    public int countPaidInvoicesByBuilding(Long buildingId) {
        if (buildingId == null) {
            // Xem tất cả (Logic cho Admin hoặc Tổng hợp)
            try (Connection conn = Db_connection.getConnection(); Statement st = conn.createStatement()) {
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM invoices WHERE status='PAID' AND is_deleted=0");
                return rs.next() ? rs.getInt(1) : 0;
            } catch (SQLException e) { return 0; }
        }
        String sql = "SELECT COUNT(*) FROM invoices i "
                + "JOIN contracts c ON i.contract_id = c.id "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE i.status = 'PAID' AND i.is_deleted = 0 AND f.building_id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, buildingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public BigDecimal getMonthlyRevenue(int month, int year, Long buildingId) {
        String sql = "SELECT SUM(i.total_amount) FROM invoices i "
                + "JOIN contracts c ON i.contract_id = c.id "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE i.status = 'PAID' AND i.is_deleted = 0 "
                + "AND MONTH(i.payment_date) = ? AND YEAR(i.payment_date) = ?";

        if (buildingId != null) sql += " AND f.building_id = ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            if (buildingId != null) ps.setLong(3, buildingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1) != null ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }
    
    // Overload for backward compatibility
    public BigDecimal getMonthlyRevenue(int month, int year) { return getMonthlyRevenue(month, year, null); }

    // =============================================================
    // HELPER
    // =============================================================

    private Invoice mapRowToInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getLong("id"));
        invoice.setContractId(rs.getLong("contract_id"));
        invoice.setMonth(rs.getInt("month"));
        invoice.setYear(rs.getInt("year"));
        invoice.setTotalAmount(rs.getBigDecimal("total_amount"));
        invoice.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            invoice.setCreatedAt(new java.util.Date(createdAt.getTime()));
        }

        Timestamp paymentDate = rs.getTimestamp("payment_date");
        if (paymentDate != null) {
            invoice.setPaymentDate(new java.util.Date(paymentDate.getTime()));
        }
        return invoice;
    }
}