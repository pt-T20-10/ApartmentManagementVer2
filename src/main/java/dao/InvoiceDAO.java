package dao;

import model.Invoice;
import model.InvoiceDetail;
import connection.Db_connection;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {

    // ================== CORE CRUD METHODS ==================
    public List<Invoice> getAllInvoices() {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices WHERE is_deleted = 0 AND status <> 'CANCELED' ORDER BY year DESC, month DESC";

        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

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
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT * FROM invoices WHERE month = ? AND year = ? AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                invoices.add(mapRowToInvoice(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return invoices;
    }

    public Long insertInvoiceAndReturnId(Invoice invoice) {
        String sql = "INSERT INTO invoices (contract_id, month, year, total_amount, status, created_at, is_deleted) VALUES (?, ?, ?, ?, ?, NOW(), 0)";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, invoice.getContractId());
            ps.setInt(2, invoice.getMonth());
            ps.setInt(3, invoice.getYear());
            ps.setBigDecimal(4, invoice.getTotalAmount());
            ps.setString(5, invoice.getStatus());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
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
            ps.setTimestamp(3, invoice.getPaymentDate() != null ? new Timestamp(invoice.getPaymentDate().getTime()) : null);
            ps.setLong(4, invoice.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Fix: Return boolean for validation
    public boolean insertInvoiceDetails(Long invoiceId, List<InvoiceDetail> details) {
        String sql = "INSERT INTO invoice_details (invoice_id, service_name, quantity, unit_price, amount) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (InvoiceDetail detail : details) {
                ps.setLong(1, invoiceId);
                ps.setString(2, detail.getServiceName());
                ps.setDouble(3, detail.getQuantity()); // Fix: setDouble
                ps.setBigDecimal(4, detail.getUnitPrice());
                ps.setBigDecimal(5, detail.getAmount());
                ps.addBatch();
            }
            ps.executeBatch();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<InvoiceDetail> getInvoiceDetails(Long invoiceId) {
        List<InvoiceDetail> details = new ArrayList<>();
        String sql = "SELECT * FROM invoice_details WHERE invoice_id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                InvoiceDetail d = new InvoiceDetail();
                d.setId(rs.getLong("id"));
                d.setInvoiceId(rs.getLong("invoice_id"));
                d.setServiceName(rs.getString("service_name"));
                d.setQuantity(rs.getDouble("quantity")); // Fix: getDouble
                d.setUnitPrice(rs.getBigDecimal("unit_price"));
                d.setAmount(rs.getBigDecimal("amount"));
                details.add(d);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return details;
    }

    public Invoice getLatestInvoiceByApartmentId(Long apartmentId) {
        String sql = "SELECT i.* FROM invoices i "
                + "JOIN contracts c ON i.contract_id = c.id "
                + "WHERE c.apartment_id = ? AND i.is_deleted = 0 "
                + "ORDER BY i.created_at DESC LIMIT 1";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, apartmentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRowToInvoice(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ================== DASHBOARD STATISTICS (FIXED) ==================
    public BigDecimal getTotalRevenue() {
        String sql = "SELECT SUM(total_amount) FROM invoices WHERE status = 'PAID' AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getBigDecimal(1) != null ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    public int countUnpaidInvoices() {
        String sql = "SELECT COUNT(*) FROM invoices WHERE status = 'UNPAID' AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public BigDecimal getTotalRevenueByBuilding(Long buildingId) {
        // Fix: Use total_amount and explicit JOINs
        String sql = "SELECT SUM(i.total_amount) FROM invoices i "
                + "JOIN contracts c ON i.contract_id = c.id "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE i.status = 'PAID' AND i.is_deleted = 0 AND f.building_id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (buildingId == null) {
                return getTotalRevenue();
            }
            ps.setLong(1, buildingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1) != null ? rs.getBigDecimal(1) : BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    public int countUnpaidInvoicesByBuilding(Long buildingId) {
        String sql = "SELECT COUNT(*) FROM invoices i "
                + "JOIN contracts c ON i.contract_id = c.id "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE i.status = 'UNPAID' AND i.is_deleted = 0 AND f.building_id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (buildingId == null) {
                return countUnpaidInvoices();
            }
            ps.setLong(1, buildingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countPaidInvoicesByBuilding(Long buildingId) {
        String sql = "SELECT COUNT(*) FROM invoices i "
                + "JOIN contracts c ON i.contract_id = c.id "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE i.status = 'PAID' AND i.is_deleted = 0 AND f.building_id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (buildingId == null) {
                // Fallback count ALL for Admin
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM invoices WHERE status='PAID' AND is_deleted=0")) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
                return 0;
            }
            ps.setLong(1, buildingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public BigDecimal getMonthlyRevenue(int month, int year, Long buildingId) {
        // Fix: Use total_amount
        String sql = "SELECT SUM(i.total_amount) FROM invoices i "
                + "JOIN contracts c ON i.contract_id = c.id "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE i.status = 'PAID' AND i.is_deleted = 0 "
                + "AND MONTH(i.payment_date) = ? AND YEAR(i.payment_date) = ?";

        if (buildingId != null) {
            sql += " AND f.building_id = ?";
        }

        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            if (buildingId != null) {
                ps.setLong(3, buildingId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1) != null ? rs.getBigDecimal(1) : BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    // Overload for backward compatibility
    public BigDecimal getMonthlyRevenue(int month, int year) {
        return getMonthlyRevenue(month, year, null);
    }

    private Invoice mapRowToInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getLong("id"));
        invoice.setContractId(rs.getLong("contract_id"));
        invoice.setMonth(rs.getInt("month"));
        invoice.setYear(rs.getInt("year"));
        invoice.setTotalAmount(rs.getBigDecimal("total_amount")); // ✅ Dùng total_amount
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
