package dao;

import model.ContractHouseholdViewModel;
import connection.Db_connection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for Contract + Household View Query: contracts LEFT JOIN
 * household_members (count)
 */
public class ContractHouseholdViewDAO {

    /**
     * Get all active contracts with household member count for a building
     */
    public List<ContractHouseholdViewModel> getContractsByBuilding(Long buildingId) {
        List<ContractHouseholdViewModel> list = new ArrayList<>();

        String sql
                = "SELECT "
                + "    c.id AS contract_id, "
                + "    c.start_date, "
                + "    c.end_date, "
                + "    c.status AS contract_status, "
                + "    a.id AS apartment_id, "
                + "    a.room_number AS apartment_number, "
                + "    f.id AS floor_id, "
                + "    f.name AS floor_name, "
                + "    b.id AS building_id, "
                + "    b.name AS building_name, "
                + "    r.id AS resident_id, "
                + "    r.full_name AS resident_full_name, "
                + "    r.phone AS resident_phone, "
                + "    r.email AS resident_email, "
                + "    r.identity_card AS resident_identity_card, "
                + "    r.gender AS resident_gender, "
                + "    r.dob AS resident_dob, "
                + "    COALESCE(hm_count.total_members, 0) AS household_member_count, "
                + "    COALESCE(hm_count.active_members, 0) AS active_household_member_count "
                + "FROM contracts c "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "JOIN buildings b ON f.building_id = b.id "
                + "JOIN residents r ON c.resident_id = r.id "
                + "LEFT JOIN ("
                + "    SELECT contract_id, "
                + "           COUNT(*) AS total_members, "
                + "           SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) AS active_members "
                + "    FROM household_members "
                + "    GROUP BY contract_id"
                + ") hm_count ON c.id = hm_count.contract_id "
                + "WHERE b.id = ? "
                + "  AND c.is_deleted = 0 "
                + "  AND a.is_deleted = 0 "
                + "  AND r.is_deleted = 0 "
                + "ORDER BY f.floor_number, a.room_number";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, buildingId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                list.add(mapToViewModel(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Search contracts by keyword (resident name, phone, apartment)
     */
    public List<ContractHouseholdViewModel> searchContracts(Long buildingId, String keyword) {
        List<ContractHouseholdViewModel> list = new ArrayList<>();

        String sql
                = "SELECT "
                + "    c.id AS contract_id, "
                + "    c.start_date, "
                + "    c.end_date, "
                + "    c.status AS contract_status, "
                + "    a.id AS apartment_id, "
                + "    a.room_number AS apartment_number, "
                + "    f.id AS floor_id, "
                + "    f.name AS floor_name, "
                + "    b.id AS building_id, "
                + "    b.name AS building_name, "
                + "    r.id AS resident_id, "
                + "    r.full_name AS resident_full_name, "
                + "    r.phone AS resident_phone, "
                + "    r.email AS resident_email, "
                + "    r.identity_card AS resident_identity_card, "
                + "    r.gender AS resident_gender, "
                + "    r.dob AS resident_dob, "
                + "    COALESCE(hm_count.total_members, 0) AS household_member_count, "
                + "    COALESCE(hm_count.active_members, 0) AS active_household_member_count "
                + "FROM contracts c "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "JOIN buildings b ON f.building_id = b.id "
                + "JOIN residents r ON c.resident_id = r.id "
                + "LEFT JOIN ("
                + "    SELECT contract_id, "
                + "           COUNT(*) AS total_members, "
                + "           SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) AS active_members "
                + "    FROM household_members "
                + "    GROUP BY contract_id"
                + ") hm_count ON c.id = hm_count.contract_id "
                + "WHERE b.id = ? "
                + "  AND c.is_deleted = 0 "
                + "  AND a.is_deleted = 0 "
                + "  AND r.is_deleted = 0 "
                + "  AND (r.full_name LIKE ? OR r.phone LIKE ? OR a.room_number LIKE ?) "
                + "ORDER BY f.floor_number, a.room_number";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, buildingId);
            String pattern = "%" + keyword + "%";
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            pstmt.setString(4, pattern);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                list.add(mapToViewModel(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Count contracts by building
     */
    public int countContractsByBuilding(Long buildingId) {
        String sql
                = "SELECT COUNT(*) "
                + "FROM contracts c "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE f.building_id = ? AND c.is_deleted = 0";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, buildingId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Map ResultSet to ViewModel
     */
    private ContractHouseholdViewModel mapToViewModel(ResultSet rs) throws SQLException {
        ContractHouseholdViewModel vm = new ContractHouseholdViewModel();

        // Contract info
        vm.setContractId(rs.getLong("contract_id"));
        vm.setStartDate(rs.getDate("start_date"));
        vm.setEndDate(rs.getDate("end_date"));
        vm.setContractStatus(rs.getString("contract_status"));

        // Apartment info
        vm.setApartmentId(rs.getLong("apartment_id"));
        vm.setApartmentNumber(rs.getString("apartment_number"));
        vm.setFloorName(rs.getString("floor_name"));
        vm.setBuildingId(rs.getLong("building_id"));
        vm.setBuildingName(rs.getString("building_name"));

        // Resident info
        vm.setResidentId(rs.getLong("resident_id"));
        vm.setResidentFullName(rs.getString("resident_full_name"));
        vm.setResidentPhone(rs.getString("resident_phone"));
        vm.setResidentEmail(rs.getString("resident_email"));
        vm.setResidentIdentityCard(rs.getString("resident_identity_card"));
        vm.setResidentGender(rs.getString("resident_gender"));
        vm.setResidentDob(rs.getDate("resident_dob"));

        // Household member count
        vm.setHouseholdMemberCount(rs.getInt("household_member_count"));
        vm.setActiveHouseholdMemberCount(rs.getInt("active_household_member_count"));

        return vm;
    }
}
