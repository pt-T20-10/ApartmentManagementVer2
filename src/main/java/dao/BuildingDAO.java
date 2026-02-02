package dao;

import model.Building;
import model.User;
import connection.Db_connection;
import util.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BuildingDAO - UPDATED Phase 3 Added building filter for MANAGER role
 */
public class BuildingDAO {

    private Building mapResultSetToBuilding(ResultSet rs) throws SQLException {
        Building building = new Building();
        building.setId(rs.getLong("id"));
        building.setName(rs.getString("name"));
        building.setAddress(rs.getString("address"));
        building.setDescription(rs.getString("description"));
        building.setStatus(rs.getString("status"));
        building.setDeleted(rs.getBoolean("is_deleted"));

        // Map manager info from JOIN
        building.setManagerUserId(rs.getLong("manager_user_id"));
        try {
            building.setManagerName(rs.getString("manager_full_name"));
        } catch (SQLException e) {
            building.setManagerName("N/A");
        }

        return building;
    }

    /**
     * Get all buildings with building filter UPDATED: Phase 3 - MANAGER only
     * sees their building
     */
    public List<Building> getAllBuildings() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        return getAllBuildings(currentUser);
    }

    /**
     * Get all buildings with user filter NEW: Phase 3
     */
    public List<Building> getAllBuildings(User currentUser) {
        List<Building> buildings = new ArrayList<>();

        String sql = "SELECT b.*, u.full_name as manager_full_name "
                + "FROM buildings b "
                + "LEFT JOIN users u ON b.manager_user_id = u.id "
                + "WHERE b.is_deleted = 0 ";

        // MANAGER + STAFF chỉ thấy building của mình
        if (currentUser != null && !currentUser.isAdmin() && currentUser.getBuildingId() != null) {
            sql += "AND b.id = ? ";
        }

        sql += "ORDER BY b.id DESC";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            if (currentUser != null && !currentUser.isAdmin() && currentUser.getBuildingId() != null) {
                ps.setLong(1, currentUser.getBuildingId());
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                buildings.add(mapResultSetToBuilding(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return buildings;
    }

    public Building getBuildingById(Long id) {
        String sql = "SELECT b.*, u.full_name as manager_full_name "
                + "FROM buildings b "
                + "LEFT JOIN users u ON b.manager_user_id = u.id "
                + "WHERE b.id = ? AND b.is_deleted = 0";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToBuilding(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insertBuilding(Building building) {
        String sql = "INSERT INTO buildings (name, address, manager_user_id, description, status, is_deleted) "
                + "VALUES (?, ?, ?, ?, ?, 0)";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, building.getName());
            pstmt.setString(2, building.getAddress());

            if (building.getManagerUserId() != null) {
                pstmt.setLong(3, building.getManagerUserId());
            } else {
                pstmt.setNull(3, Types.BIGINT);
            }

            pstmt.setString(4, building.getDescription());
            pstmt.setString(5, building.getStatus());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateBuilding(Building building) {
        String sql = "UPDATE buildings SET name=?, address=?, manager_user_id=?, description=?, status=? WHERE id=?";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, building.getName());
            pstmt.setString(2, building.getAddress());

            if (building.getManagerUserId() != null) {
                pstmt.setLong(3, building.getManagerUserId());
            } else {
                pstmt.setNull(3, Types.BIGINT);
            }

            pstmt.setString(4, building.getDescription());
            pstmt.setString(5, building.getStatus());
            pstmt.setLong(6, building.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteBuilding(Long id) {
        String sql = "UPDATE buildings SET is_deleted = 1 WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addBuilding(Building building) {
        return insertBuilding(building);
    }

    public List<Building> searchBuildingsByName(String keyword) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        List<Building> buildings = new ArrayList<>();

        String sql = "SELECT b.*, u.full_name as manager_full_name "
                + "FROM buildings b "
                + "LEFT JOIN users u ON b.manager_user_id = u.id "
                + "WHERE (b.name LIKE ? OR b.address LIKE ?) "
                + "AND b.is_deleted = 0 ";

        // MANAGER + STAFF chỉ thấy building của mình
        if (currentUser != null && !currentUser.isAdmin() && currentUser.getBuildingId() != null) {
            sql += "AND b.id = ? ";
        }

        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            String query = "%" + keyword + "%";
            ps.setString(1, query);
            ps.setString(2, query);

            if (currentUser != null && !currentUser.isAdmin() && currentUser.getBuildingId() != null) {
                ps.setLong(3, currentUser.getBuildingId());
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                buildings.add(mapResultSetToBuilding(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return buildings;
    }

    // --- STATISTICS ---
    public static class BuildingStats {

        public int totalFloors = 0;
        public int totalApartments = 0;
        public int rentedApartments = 0;

        public int getOccupancyRate() {
            if (totalApartments == 0) {
                return 0;
            }
            return (rentedApartments * 100) / totalApartments;
        }
    }

    public BuildingStats getBuildingStatistics(Long buildingId) {
        BuildingStats stats = new BuildingStats();
        String sqlFloors = "SELECT COUNT(*) FROM floors WHERE building_id = ? AND is_deleted = 0";
        String sqlApts = "SELECT COUNT(*) FROM apartments a JOIN floors f ON a.floor_id = f.id "
                + "WHERE f.building_id = ? AND a.is_deleted = 0";
        String sqlRented = "SELECT COUNT(DISTINCT c.apartment_id) FROM contracts c "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE f.building_id = ? AND c.status = 'ACTIVE' AND c.is_deleted = 0";

        try (Connection conn = Db_connection.getConnection()) {
            try (PreparedStatement pst1 = conn.prepareStatement(sqlFloors)) {
                pst1.setLong(1, buildingId);
                try (ResultSet rs = pst1.executeQuery()) {
                    if (rs.next()) {
                        stats.totalFloors = rs.getInt(1);
                    }
                }
            }
            try (PreparedStatement pst2 = conn.prepareStatement(sqlApts)) {
                pst2.setLong(1, buildingId);
                try (ResultSet rs = pst2.executeQuery()) {
                    if (rs.next()) {
                        stats.totalApartments = rs.getInt(1);
                    }
                }
            }
            try (PreparedStatement pst3 = conn.prepareStatement(sqlRented)) {
                pst3.setLong(1, buildingId);
                try (ResultSet rs = pst3.executeQuery()) {
                    if (rs.next()) {
                        stats.rentedApartments = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    /**
     * Count buildings (with filter) UPDATED: Phase 3
     */
    public int countBuildings() {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        String sql = "SELECT COUNT(*) FROM buildings WHERE is_deleted = 0";

        // MANAGER + STAFF chỉ thấy building của mình
        if (currentUser != null && !currentUser.isAdmin() && currentUser.getBuildingId() != null) {
            sql += " AND id = ?";
        }

        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            if (currentUser != null && !currentUser.isAdmin() && currentUser.getBuildingId() != null) {
                ps.setLong(1, currentUser.getBuildingId());
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean hasActiveContracts(Long buildingId) {
        String sql = "SELECT COUNT(*) FROM contracts c "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE f.building_id = ? "
                + "AND c.status = 'ACTIVE' "
                + "AND c.is_deleted = 0";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, buildingId);
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

    public boolean updateStatusCascade(Long buildingId, String newStatus) {
        Connection conn = null;
        PreparedStatement pstBuilding = null;
        PreparedStatement pstFloors = null;
        PreparedStatement pstApartments = null;

        try {
            conn = Db_connection.getConnection();
            conn.setAutoCommit(false);

            // 1. Update building status
            String sqlBuilding = "UPDATE buildings SET status = ? WHERE id = ?";
            pstBuilding = conn.prepareStatement(sqlBuilding);
            pstBuilding.setString(1, newStatus);
            pstBuilding.setLong(2, buildingId);
            pstBuilding.executeUpdate();

            // 2. Cascade logic
            if ("MAINTENANCE".equalsIgnoreCase(newStatus)) {
                // Floors -> MAINTENANCE
                String sqlFloor = "UPDATE floors SET status = 'MAINTENANCE' "
                        + "WHERE building_id = ? AND is_deleted = 0";
                pstFloors = conn.prepareStatement(sqlFloor);
                pstFloors.setLong(1, buildingId);
                pstFloors.executeUpdate();

                // Apartments -> MAINTENANCE
                String sqlApt = "UPDATE apartments SET status = 'MAINTENANCE' "
                        + "WHERE floor_id IN (SELECT id FROM floors WHERE building_id = ?) "
                        + "AND is_deleted = 0";
                pstApartments = conn.prepareStatement(sqlApt);
                pstApartments.setLong(1, buildingId);
                pstApartments.executeUpdate();

            } else if ("ACTIVE".equalsIgnoreCase(newStatus)) {
                // Floors -> ACTIVE
                String sqlFloor = "UPDATE floors SET status = 'ACTIVE' "
                        + "WHERE building_id = ? AND is_deleted = 0";
                pstFloors = conn.prepareStatement(sqlFloor);
                pstFloors.setLong(1, buildingId);
                pstFloors.executeUpdate();

                // Apartments -> AVAILABLE (only from MAINTENANCE)
                String sqlApt = "UPDATE apartments SET status = 'AVAILABLE' "
                        + "WHERE floor_id IN (SELECT id FROM floors WHERE building_id = ?) "
                        + "AND status = 'MAINTENANCE' "
                        + "AND is_deleted = 0";
                pstApartments = conn.prepareStatement(sqlApt);
                pstApartments.setLong(1, buildingId);
                pstApartments.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (pstBuilding != null) {
                    pstBuilding.close();
                }
                if (pstFloors != null) {
                    pstFloors.close();
                }
                if (pstApartments != null) {
                    pstApartments.close();
                }
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
