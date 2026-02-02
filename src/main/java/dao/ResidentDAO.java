package dao;

import model.Resident;
import connection.Db_connection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResidentDAO {

    // ... (Giữ nguyên CRUD) ...

    public int countResidents() {
        String sql = "SELECT COUNT(*) FROM residents WHERE is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ✅ FIXED: Đếm cư dân theo ID tòa nhà
    public int countResidentsByBuilding(Long buildingId) {
        if (buildingId == null) return countResidents(); // Fallback for global count
        
        String sql = "SELECT COUNT(DISTINCT r.id) FROM residents r "
                   + "JOIN contracts c ON c.resident_id = r.id "
                   + "JOIN apartments a ON c.apartment_id = a.id "
                   + "JOIN floors f ON a.floor_id = f.id "
                   + "WHERE f.building_id = ? AND r.is_deleted = 0 AND c.status = 'ACTIVE'";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, buildingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
    
    // ... (Giữ nguyên mapRowToResident) ...
    private Resident mapRowToResident(ResultSet rs) throws SQLException {
        Resident resident = new Resident();
        resident.setId(rs.getLong("id"));
        resident.setFullName(rs.getString("full_name"));
        resident.setPhone(rs.getString("phone"));
        resident.setEmail(rs.getString("email"));
        resident.setIdentityCard(rs.getString("identity_card"));
        resident.setGender(rs.getString("gender"));
        java.sql.Date sqlDate = rs.getDate("dob");
        if (sqlDate != null) resident.setDob(new java.util.Date(sqlDate.getTime()));
        resident.setHometown(rs.getString("hometown"));
        resident.setDeleted(rs.getBoolean("is_deleted"));
        return resident;
    }
    
    // ... (Các hàm còn lại giữ nguyên) ...
    public List<Resident> getAllResidents() {
        List<Resident> residents = new ArrayList<>();
        String sql = "SELECT * FROM residents WHERE is_deleted = 0 ORDER BY full_name";
        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                residents.add(mapRowToResident(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return residents;
    }

    public Resident getResidentById(Long id) {
        String sql = "SELECT * FROM residents WHERE id = ? AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToResident(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insertResident(Resident r) {
        String sql = "INSERT INTO residents (full_name, phone, email, identity_card, gender, dob, hometown, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, 0)";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, r.getFullName());
            pstmt.setString(2, r.getPhone());
            pstmt.setString(3, r.getEmail());
            pstmt.setString(4, r.getIdentityCard());
            pstmt.setString(5, r.getGender());
            pstmt.setDate(6, r.getDob() != null ? new java.sql.Date(r.getDob().getTime()) : null);
            pstmt.setString(7, r.getHometown());
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        r.setId(rs.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateResident(Resident r) {
        String sql = "UPDATE residents SET full_name=?, phone=?, email=?, identity_card=?, gender=?, dob=?, hometown=? WHERE id=?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, r.getFullName());
            pstmt.setString(2, r.getPhone());
            pstmt.setString(3, r.getEmail());
            pstmt.setString(4, r.getIdentityCard());
            pstmt.setString(5, r.getGender());
            pstmt.setDate(6, r.getDob() != null ? new java.sql.Date(r.getDob().getTime()) : null);
            pstmt.setString(7, r.getHometown());
            pstmt.setLong(8, r.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isIdentityCardExists(String card) {
        String sql = "SELECT COUNT(*) FROM residents WHERE identity_card = ? AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, card);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}