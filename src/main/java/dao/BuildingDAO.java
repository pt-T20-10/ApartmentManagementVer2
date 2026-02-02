package dao;

import model.Building;
import model.User;
import connection.Db_connection;
import util.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BuildingDAO - UPDATED for Many-to-Many
 * Fix: Đồng bộ dữ liệu sang bảng user_buildings khi Thêm/Sửa tòa nhà
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

        building.setManagerUserId(rs.getLong("manager_user_id"));
        try {
            building.setManagerName(rs.getString("manager_full_name"));
        } catch (SQLException e) {
            building.setManagerName("N/A");
        }

        return building;
    }

    public List<Building> getAllBuildings() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        return getAllBuildings(currentUser);
    }

    /**
     * ✅ UPDATED: Filter dựa trên user_buildings junction table
     */
    public List<Building> getAllBuildings(User currentUser) {
        List<Building> buildings = new ArrayList<>();

        String sql = "SELECT b.*, u.full_name as manager_full_name "
                + "FROM buildings b "
                + "LEFT JOIN users u ON b.manager_user_id = u.id "
                + "WHERE b.is_deleted = 0 ";

        // MANAGER/STAFF chỉ thấy buildings trong user_buildings
        if (currentUser != null && !currentUser.isAdmin() && currentUser.hasBuilding()) {
            sql += "AND b.id IN (SELECT building_id FROM user_buildings WHERE user_id = ?) ";
        }

        sql += "ORDER BY b.id DESC";

        try (Connection conn = Db_connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (currentUser != null && !currentUser.isAdmin() && currentUser.hasBuilding()) {
                ps.setLong(1, currentUser.getId());
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

        try (Connection conn = Db_connection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    public List<Building> getBuildingsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();

        List<Building> buildings = new ArrayList<>();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }

        String sql = "SELECT b.*, u.full_name as manager_full_name "
                + "FROM buildings b "
                + "LEFT JOIN users u ON b.manager_user_id = u.id "
                + "WHERE b.id IN (" + placeholders + ") AND b.is_deleted = 0 "
                + "ORDER BY b.name";

        try (Connection conn = Db_connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < ids.size(); i++) {
                ps.setLong(i + 1, ids.get(i));
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

    /**
     * ✅ UPDATED: Insert Building -> Đồng bộ sang user_buildings
     */
    public boolean insertBuilding(Building building) {
        String sql = "INSERT INTO buildings (name, address, manager_user_id, description, status, is_deleted) "
                + "VALUES (?, ?, ?, ?, ?, 0)";

        Connection conn = null;
        try {
            conn = Db_connection.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            long generatedId = -1;

            // 1. Insert vào bảng buildings
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, building.getName());
                pstmt.setString(2, building.getAddress());

                if (building.getManagerUserId() != null) {
                    pstmt.setLong(3, building.getManagerUserId());
                } else {
                    pstmt.setNull(3, Types.BIGINT);
                }

                pstmt.setString(4, building.getDescription());
                pstmt.setString(5, building.getStatus());
                
                int affected = pstmt.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getLong(1);
                    }
                }
            }

            // 2. Đồng bộ sang user_buildings (Nếu có chọn Manager)
            if (generatedId != -1 && building.getManagerUserId() != null) {
                String sqlLink = "INSERT INTO user_buildings (user_id, building_id, assigned_date) VALUES (?, ?, NOW())";
                try (PreparedStatement pstLink = conn.prepareStatement(sqlLink)) {
                    pstLink.setLong(1, building.getManagerUserId());
                    pstLink.setLong(2, generatedId);
                    pstLink.executeUpdate();
                }
            }

            conn.commit(); // Hoàn tất
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
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
        return false;
    }

    /**
     * ✅ UPDATED: Update Building -> Đồng bộ lại user_buildings
     */
    public boolean updateBuilding(Building building) {
        String sql = "UPDATE buildings SET name=?, address=?, manager_user_id=?, description=?, status=? WHERE id=?";

        Connection conn = null;
        try {
            conn = Db_connection.getConnection();
            conn.setAutoCommit(false);

            // 1. Update bảng buildings
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
                
                if (pstmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // 2. Đồng bộ bảng user_buildings (Xóa cũ -> Thêm mới để tránh trùng lặp)
            // Xóa phân quyền cũ của tòa nhà này
            String sqlDelete = "DELETE FROM user_buildings WHERE building_id = ?";
            try (PreparedStatement pstDel = conn.prepareStatement(sqlDelete)) {
                pstDel.setLong(1, building.getId());
                pstDel.executeUpdate();
            }

            // Thêm phân quyền mới nếu có Manager
            if (building.getManagerUserId() != null) {
                String sqlInsert = "INSERT INTO user_buildings (user_id, building_id, assigned_date) VALUES (?, ?, NOW())";
                try (PreparedStatement pstIns = conn.prepareStatement(sqlInsert)) {
                    pstIns.setLong(1, building.getManagerUserId());
                    pstIns.setLong(2, building.getId());
                    pstIns.executeUpdate();
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
        return false;
    }

    public boolean deleteBuilding(Long id) {
        String sql = "UPDATE buildings SET is_deleted = 1 WHERE id = ?";
        try (Connection conn = Db_connection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

        if (currentUser != null && !currentUser.isAdmin() && currentUser.hasBuilding()) {
            sql += "AND b.id IN (SELECT building_id FROM user_buildings WHERE user_id = ?) ";
        }

        try (Connection conn = Db_connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String query = "%" + keyword + "%";
            ps.setString(1, query);
            ps.setString(2, query);

            if (currentUser != null && !currentUser.isAdmin() && currentUser.hasBuilding()) {
                ps.setLong(3, currentUser.getId());
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

    public static class BuildingStats {
        public int totalFloors = 0;
        public int totalApartments = 0;
        public int rentedApartments = 0;

        public int getOccupancyRate() {
            if (totalApartments == 0) return 0;
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
                    if (rs.next()) stats.totalFloors = rs.getInt(1);
                }
            }
            try (PreparedStatement pst2 = conn.prepareStatement(sqlApts)) {
                pst2.setLong(1, buildingId);
                try (ResultSet rs = pst2.executeQuery()) {
                    if (rs.next()) stats.totalApartments = rs.getInt(1);
                }
            }
            try (PreparedStatement pst3 = conn.prepareStatement(sqlRented)) {
                pst3.setLong(1, buildingId);
                try (ResultSet rs = pst3.executeQuery()) {
                    if (rs.next()) stats.rentedApartments = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    public int countBuildings() {
        User currentUser = SessionManager.getInstance().getCurrentUser();

        String sql = "SELECT COUNT(*) FROM buildings WHERE is_deleted = 0";

        if (currentUser != null && !currentUser.isAdmin() && currentUser.hasBuilding()) {
            sql += " AND id IN (SELECT building_id FROM user_buildings WHERE user_id = ?)";
        }

        try (Connection conn = Db_connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (currentUser != null && !currentUser.isAdmin() && currentUser.hasBuilding()) {
                ps.setLong(1, currentUser.getId());
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

        try (Connection conn = Db_connection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

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

            String sqlBuilding = "UPDATE buildings SET status = ? WHERE id = ?";
            pstBuilding = conn.prepareStatement(sqlBuilding);
            pstBuilding.setString(1, newStatus);
            pstBuilding.setLong(2, buildingId);
            pstBuilding.executeUpdate();

            if ("MAINTENANCE".equalsIgnoreCase(newStatus)) {
                String sqlFloor = "UPDATE floors SET status = 'MAINTENANCE' "
                        + "WHERE building_id = ? AND is_deleted = 0";
                pstFloors = conn.prepareStatement(sqlFloor);
                pstFloors.setLong(1, buildingId);
                pstFloors.executeUpdate();

                String sqlApt = "UPDATE apartments SET status = 'MAINTENANCE' "
                        + "WHERE floor_id IN (SELECT id FROM floors WHERE building_id = ?) "
                        + "AND is_deleted = 0";
                pstApartments = conn.prepareStatement(sqlApt);
                pstApartments.setLong(1, buildingId);
                pstApartments.executeUpdate();

            } else if ("ACTIVE".equalsIgnoreCase(newStatus)) {
                String sqlFloor = "UPDATE floors SET status = 'ACTIVE' "
                        + "WHERE building_id = ? AND is_deleted = 0";
                pstFloors = conn.prepareStatement(sqlFloor);
                pstFloors.setLong(1, buildingId);
                pstFloors.executeUpdate();

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
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (pstBuilding != null) pstBuilding.close();
                if (pstFloors != null) pstFloors.close();
                if (pstApartments != null) pstApartments.close();
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