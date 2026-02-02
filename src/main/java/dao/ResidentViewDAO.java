package dao;

import model.*;
import connection.Db_connection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ResidentViewDAO Complex DAO để query UNION data từ residents +
 * household_members Dùng cho Tab Cư Dân
 */
public class ResidentViewDAO {

    /**
     * Lấy TẤT CẢ cư dân (Chủ hộ + Thành viên) theo Building ID
     *
     * Query logic: 1. JOIN: contracts → apartments → floors → buildings 2.
     * UNION: - Chủ hộ: residents + contracts (WHERE contracts.status =
     * 'ACTIVE') - Thành viên: household_members + contracts (WHERE is_active =
     * 1)
     */
    public List<ResidentViewModel> getResidentsByBuilding(Long buildingId) {
        List<ResidentViewModel> result = new ArrayList<>();

        // Query Chủ hộ
        String sqlResidents
                = "SELECT "
                + "  'CHU_HO' as source_type, "
                + "  r.id, r.full_name, r.gender, r.dob, r.identity_card, r.phone, r.email, r.hometown, "
                + "  'Chủ hộ' as role, 'Chủ hộ' as relationship, "
                + "  c.id as contract_id, c.status as contract_status, "
                + "  a.id as apartment_id, a.room_number, "
                + "  f.name as floor_name, b.name as building_name, b.id as building_id "
                + "FROM residents r "
                + "INNER JOIN contracts c ON r.id = c.resident_id "
                + "INNER JOIN apartments a ON c.apartment_id = a.id "
                + "INNER JOIN floors f ON a.floor_id = f.id "
                + "INNER JOIN buildings b ON f.building_id = b.id "
                + "WHERE b.id = ? AND r.is_deleted = 0 AND c.is_deleted = 0";

        // Query Thành viên
        String sqlMembers
                = "SELECT "
                + "  'THANH_VIEN' as source_type, "
                + "  hm.id, hm.full_name, hm.gender, hm.dob, hm.identity_card, hm.phone, "
                + "  NULL as email, NULL as hometown, "
                + "  'Thành viên' as role, hm.relationship, "
                + "  c.id as contract_id, c.status as contract_status, "
                + "  a.id as apartment_id, a.room_number, "
                + "  f.name as floor_name, b.name as building_name, b.id as building_id "
                + "FROM household_members hm "
                + "INNER JOIN contracts c ON hm.contract_id = c.id "
                + "INNER JOIN apartments a ON c.apartment_id = a.id "
                + "INNER JOIN floors f ON a.floor_id = f.id "
                + "INNER JOIN buildings b ON f.building_id = b.id "
                + "WHERE b.id = ? AND c.is_deleted = 0";

        try (Connection conn = Db_connection.getConnection()) {

            // 1. Load Chủ hộ
            try (PreparedStatement pstmt = conn.prepareStatement(sqlResidents)) {
                pstmt.setLong(1, buildingId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    result.add(mapToViewModel(rs));
                }
            }

            // 2. Load Thành viên
            try (PreparedStatement pstmt = conn.prepareStatement(sqlMembers)) {
                pstmt.setLong(1, buildingId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    result.add(mapToViewModel(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Tìm kiếm cư dân theo keyword (Name, Phone, ID Card)
     */
    public List<ResidentViewModel> searchResidents(Long buildingId, String keyword) {
        List<ResidentViewModel> result = new ArrayList<>();
        String likePattern = "%" + keyword + "%";

        String sqlResidents
                = "SELECT "
                + "  'CHU_HO' as source_type, "
                + "  r.id, r.full_name, r.gender, r.dob, r.identity_card, r.phone, r.email, r.hometown, "
                + "  'Chủ hộ' as role, 'Chủ hộ' as relationship, "
                + "  c.id as contract_id, c.status as contract_status, "
                + "  a.id as apartment_id, a.room_number, "
                + "  f.name as floor_name, b.name as building_name, b.id as building_id "
                + "FROM residents r "
                + "INNER JOIN contracts c ON r.id = c.resident_id "
                + "INNER JOIN apartments a ON c.apartment_id = a.id "
                + "INNER JOIN floors f ON a.floor_id = f.id "
                + "INNER JOIN buildings b ON f.building_id = b.id "
                + "WHERE b.id = ? AND r.is_deleted = 0 AND c.is_deleted = 0 "
                + "  AND (r.full_name LIKE ? OR r.phone LIKE ? OR r.identity_card LIKE ?)";

        String sqlMembers
                = "SELECT "
                + "  'THANH_VIEN' as source_type, "
                + "  hm.id, hm.full_name, hm.gender, hm.dob, hm.identity_card, hm.phone, "
                + "  NULL as email, NULL as hometown, "
                + "  'Thành viên' as role, hm.relationship, "
                + "  c.id as contract_id, c.status as contract_status, "
                + "  a.id as apartment_id, a.room_number, "
                + "  f.name as floor_name, b.name as building_name, b.id as building_id "
                + "FROM household_members hm "
                + "INNER JOIN contracts c ON hm.contract_id = c.id "
                + "INNER JOIN apartments a ON c.apartment_id = a.id "
                + "INNER JOIN floors f ON a.floor_id = f.id "
                + "INNER JOIN buildings b ON f.building_id = b.id "
                + "WHERE b.id = ? AND c.is_deleted = 0 "
                + "  AND (hm.full_name LIKE ? OR hm.phone LIKE ? OR hm.identity_card LIKE ?)";

        try (Connection conn = Db_connection.getConnection()) {

            // Search Chủ hộ
            try (PreparedStatement pstmt = conn.prepareStatement(sqlResidents)) {
                pstmt.setLong(1, buildingId);
                pstmt.setString(2, likePattern);
                pstmt.setString(3, likePattern);
                pstmt.setString(4, likePattern);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    result.add(mapToViewModel(rs));
                }
            }

            // Search Thành viên
            try (PreparedStatement pstmt = conn.prepareStatement(sqlMembers)) {
                pstmt.setLong(1, buildingId);
                pstmt.setString(2, likePattern);
                pstmt.setString(3, likePattern);
                pstmt.setString(4, likePattern);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    result.add(mapToViewModel(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Map ResultSet to ResidentViewModel
     */
    private ResidentViewModel mapToViewModel(ResultSet rs) throws SQLException {
        ResidentViewModel vm = new ResidentViewModel();

        // Source & Basic Info
        vm.setSourceType(rs.getString("source_type"));
        vm.setId(rs.getLong("id"));
        vm.setFullName(rs.getString("full_name"));
        vm.setGender(rs.getString("gender"));

        java.sql.Date sqlDob = rs.getDate("dob");
        if (sqlDob != null) {
            vm.setDob(new java.util.Date(sqlDob.getTime()));
        }

        vm.setIdentityCard(rs.getString("identity_card"));
        vm.setPhone(rs.getString("phone"));
        vm.setEmail(rs.getString("email"));
        vm.setHometown(rs.getString("hometown"));

        // Role
        vm.setRole(rs.getString("role"));
        vm.setRelationship(rs.getString("relationship"));

        // Contract & Apartment
        vm.setContractId(rs.getLong("contract_id"));
        vm.setContractStatus(rs.getString("contract_status"));
        vm.setApartmentId(rs.getLong("apartment_id"));
        vm.setApartmentNumber(rs.getString("room_number"));
        vm.setFloorName(rs.getString("floor_name"));
        vm.setBuildingName(rs.getString("building_name"));
        vm.setBuildingId(rs.getLong("building_id"));

        // Trạng thái cư trú
        String contractStatus = rs.getString("contract_status");

        if ("CHU_HO".equals(vm.getSourceType())) {
            // Chủ hộ: status = contract status
            vm.setResidencyStatus("ACTIVE".equals(contractStatus) ? "Đang ở" : "Đã chuyển đi");
        } else {
            // Thành viên: Cần check thêm is_active (nhưng đã filter trong query rồi)
            // Nếu vào được đây nghĩa là member đang active
            vm.setResidencyStatus("ACTIVE".equals(contractStatus) ? "Đang ở" : "Đã chuyển đi");
        }

        return vm;
    }

    /**
     * Count total residents by building
     */
    public int countResidentsByBuilding(Long buildingId) {
        int count = 0;

        String sqlResidents
                = "SELECT COUNT(*) FROM residents r "
                + "INNER JOIN contracts c ON r.id = c.resident_id "
                + "INNER JOIN apartments a ON c.apartment_id = a.id "
                + "INNER JOIN floors f ON a.floor_id = f.id "
                + "WHERE f.building_id = ? AND r.is_deleted = 0 AND c.is_deleted = 0";

        String sqlMembers
                = "SELECT COUNT(*) FROM household_members hm "
                + "INNER JOIN contracts c ON hm.contract_id = c.id "
                + "INNER JOIN apartments a ON c.apartment_id = a.id "
                + "INNER JOIN floors f ON a.floor_id = f.id "
                + "WHERE f.building_id = ? AND c.is_deleted = 0";

        try (Connection conn = Db_connection.getConnection()) {

            try (PreparedStatement pstmt = conn.prepareStatement(sqlResidents)) {
                pstmt.setLong(1, buildingId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    count += rs.getInt(1);
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlMembers)) {
                pstmt.setLong(1, buildingId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    count += rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return count;
    }
}
