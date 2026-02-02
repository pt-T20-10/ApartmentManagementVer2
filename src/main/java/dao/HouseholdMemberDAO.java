package dao;

import model.HouseholdMember;
import connection.Db_connection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for HouseholdMember operations
 */
public class HouseholdMemberDAO {

    /**
     * Map ResultSet to HouseholdMember
     */
    private HouseholdMember mapResultSetToHouseholdMember(ResultSet rs) throws SQLException {
        HouseholdMember member = new HouseholdMember();
        member.setId(rs.getLong("id"));
        member.setContractId(rs.getLong("contract_id"));
        member.setFullName(rs.getString("full_name"));
        member.setRelationship(rs.getString("relationship"));
        member.setHead(rs.getBoolean("is_head"));
        member.setGender(rs.getString("gender"));

        java.sql.Date sqlDob = rs.getDate("dob");
        if (sqlDob != null) {
            member.setDob(new java.util.Date(sqlDob.getTime()));
        }

        member.setIdentityCard(rs.getString("identity_card"));
        member.setPhone(rs.getString("phone"));
        member.setActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            member.setCreatedAt(new java.util.Date(createdAt.getTime()));
        }

        return member;
    }

    /**
     * Get all household members
     */
    public List<HouseholdMember> getAllHouseholdMembers() {
        List<HouseholdMember> members = new ArrayList<>();
        String sql = "SELECT * FROM household_members ORDER BY created_at DESC";

        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                members.add(mapResultSetToHouseholdMember(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    /**
     * Get household members by contract ID
     */
    public List<HouseholdMember> getByContractId(Long contractId) {
        List<HouseholdMember> members = new ArrayList<>();
        String sql = "SELECT * FROM household_members WHERE contract_id = ? ORDER BY is_head DESC, full_name";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, contractId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                members.add(mapResultSetToHouseholdMember(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    /**
     * Get active household members by contract ID
     */
    public List<HouseholdMember> getActiveByContractId(Long contractId) {
        List<HouseholdMember> members = new ArrayList<>();
        String sql = "SELECT * FROM household_members WHERE contract_id = ? AND is_active = 1 "
                + "ORDER BY is_head DESC, full_name";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, contractId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                members.add(mapResultSetToHouseholdMember(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    /**
     * Get household member by ID
     */
    public HouseholdMember getById(Long id) {
        String sql = "SELECT * FROM household_members WHERE id = ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToHouseholdMember(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Insert new household member
     */
    public boolean insert(HouseholdMember member) {
        String sql = "INSERT INTO household_members (contract_id, full_name, relationship, is_head, "
                + "gender, dob, identity_card, phone, is_active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, member.getContractId());
            pstmt.setString(2, member.getFullName());
            pstmt.setString(3, member.getRelationship());
            pstmt.setBoolean(4, member.isHead());
            pstmt.setString(5, member.getGender());

            if (member.getDob() != null) {
                pstmt.setDate(6, new java.sql.Date(member.getDob().getTime()));
            } else {
                pstmt.setNull(6, Types.DATE);
            }

            pstmt.setString(7, member.getIdentityCard());
            pstmt.setString(8, member.getPhone());
            pstmt.setBoolean(9, member.isActive());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Update household member
     */
    public boolean update(HouseholdMember member) {
        String sql = "UPDATE household_members SET full_name = ?, relationship = ?, is_head = ?, "
                + "gender = ?, dob = ?, identity_card = ?, phone = ?, is_active = ? "
                + "WHERE id = ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, member.getFullName());
            pstmt.setString(2, member.getRelationship());
            pstmt.setBoolean(3, member.isHead());
            pstmt.setString(4, member.getGender());

            if (member.getDob() != null) {
                pstmt.setDate(5, new java.sql.Date(member.getDob().getTime()));
            } else {
                pstmt.setNull(5, Types.DATE);
            }

            pstmt.setString(6, member.getIdentityCard());
            pstmt.setString(7, member.getPhone());
            pstmt.setBoolean(8, member.isActive());
            pstmt.setLong(9, member.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Set member as inactive (soft delete)
     */
    public boolean setInactive(Long id) {
        String sql = "UPDATE household_members SET is_active = 0 WHERE id = ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete household member (hard delete)
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM household_members WHERE id = ?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Count active members by contract
     */
    public int countActiveByContract(Long contractId) {
        String sql = "SELECT COUNT(*) FROM household_members WHERE contract_id = ? AND is_active = 1";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, contractId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
