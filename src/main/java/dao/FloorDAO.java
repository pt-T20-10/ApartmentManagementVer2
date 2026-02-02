package dao;

import model.Floor;
import connection.Db_connection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO class for Floor operations FINAL VERSION: Smart Insert + Full Validation
 * + Cascade Update
 */
public class FloorDAO {

    // --- MAPPER ---
    private Floor mapResultSetToFloor(ResultSet rs) throws SQLException {
        Floor floor = new Floor();
        floor.setId(rs.getLong("id"));
        floor.setBuildingId(rs.getLong("building_id"));
        floor.setFloorNumber(rs.getInt("floor_number"));
        floor.setName(rs.getString("name"));
        floor.setStatus(rs.getString("status"));
        floor.setDeleted(rs.getBoolean("is_deleted"));
        return floor;
    }

    // --- 1. LẤY DANH SÁCH & THỐNG KÊ ---
    public List<FloorWithStats> getFloorsWithStatsByBuildingId(Long buildingId) {
        List<FloorWithStats> results = new ArrayList<>();
        String sql
                = "SELECT f.id, f.building_id, f.floor_number, f.name, f.status, f.is_deleted, "
                + "       COALESCE(apt_stats.total_apts, 0)  AS total_apts, "
                + "       COALESCE(apt_stats.rented_apts, 0) AS rented_apts, "
                + "       COALESCE(apt_stats.owned_apts, 0)  AS owned_apts "
                + "FROM floors f "
                + "LEFT JOIN ( "
                + "    SELECT a.floor_id, "
                + "           COUNT(a.id) AS total_apts, "
                + "           SUM(CASE WHEN a.status IN ('RENTED', 'Đã thuê', 'OCCUPIED') THEN 1 ELSE 0 END) AS rented_apts, "
                + "           SUM(CASE WHEN a.status = 'OWNED' THEN 1 ELSE 0 END) AS owned_apts "
                + "    FROM apartments a "
                + "    WHERE a.is_deleted = 0 "
                + "    GROUP BY a.floor_id "
                + ") apt_stats ON f.id = apt_stats.floor_id "
                + "WHERE f.building_id = ? AND (f.is_deleted = 0 OR f.is_deleted IS NULL) "
                + "ORDER BY f.floor_number ASC";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, buildingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Floor floor = mapResultSetToFloor(rs);
                    FloorStats stats = new FloorStats();
                    stats.totalApartments = rs.getInt("total_apts");
                    stats.rentedApartments = rs.getInt("rented_apts");
                    stats.ownedApartments = rs.getInt("owned_apts");
                    results.add(new FloorWithStats(floor, stats));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    // --- 2. CÁC HÀM GET CƠ BẢN ---
    public Floor getFloorById(Long id) {
        String sql = "SELECT * FROM floors WHERE id = ? AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFloor(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Floor> getFloorsByBuildingId(Long buildingId) {
        List<Floor> floors = new ArrayList<>();
        String sql = "SELECT * FROM floors WHERE building_id = ? AND is_deleted = 0 ORDER BY floor_number";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, buildingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    floors.add(mapResultSetToFloor(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return floors;
    }

    // --- 3. CHECK VALIDATION (ĐÃ KHÔI PHỤC ĐẦY ĐỦ) ---
    // ✅ Kiểm tra trùng Tên (BatchAddFloorDialog cần cái này)
    public boolean isFloorNameExists(Long buildingId, String name) {
        return isFloorNameExists(buildingId, name, null);
    }

    public boolean isFloorNameExists(Long buildingId, String name, Long excludeFloorId) {
        long excludeId = (excludeFloorId != null) ? excludeFloorId : -1;
        String sql = "SELECT COUNT(*) FROM floors WHERE building_id = ? AND name = ? AND id != ? AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, buildingId);
            pstmt.setString(2, name);
            pstmt.setLong(3, excludeId);
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

    // ✅ Kiểm tra trùng Số tầng
    public boolean isFloorNumberExists(Long buildingId, int floorNumber) {
        return isFloorNumberExists(buildingId, floorNumber, null);
    }

    public boolean isFloorNumberExists(Long buildingId, int floorNumber, Long excludeFloorId) {
        long excludeId = (excludeFloorId != null) ? excludeFloorId : -1;
        String sql = "SELECT COUNT(*) FROM floors WHERE building_id = ? AND floor_number = ? AND id != ? AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, buildingId);
            pstmt.setInt(2, floorNumber);
            pstmt.setLong(3, excludeId);
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

    // --- 4. CRUD OPERATIONS (SMART INSERT) ---
    public boolean insertFloor(Floor floor) {
        // Kiểm tra xem có tầng đã xóa trùng số không
        Long deletedId = getDeletedFloorId(floor.getBuildingId(), floor.getFloorNumber());

        if (deletedId != null) {
            // Restore nếu tìm thấy
            return restoreFloor(deletedId, floor.getName(), floor.getStatus());
        } else {
            // Insert mới nếu không
            String sql = "INSERT INTO floors (building_id, floor_number, name, status, is_deleted) VALUES (?, ?, ?, ?, 0)";
            try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, floor.getBuildingId());
                pstmt.setInt(2, floor.getFloorNumber());
                pstmt.setString(3, floor.getName());
                pstmt.setString(4, floor.getStatus());
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private Long getDeletedFloorId(Long buildingId, int floorNumber) {
        String sql = "SELECT id FROM floors WHERE building_id = ? AND floor_number = ? AND is_deleted = 1 LIMIT 1";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, buildingId);
            pstmt.setInt(2, floorNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean restoreFloor(Long floorId, String name, String status) {
        String sql = "UPDATE floors SET is_deleted = 0, name = ?, status = ? WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, status);
            pstmt.setLong(3, floorId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateFloor(Floor floor) {
        String sql = "UPDATE floors SET floor_number = ?, name = ?, status = ? WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, floor.getFloorNumber());
            pstmt.setString(2, floor.getName());
            pstmt.setString(3, floor.getStatus());
            pstmt.setLong(4, floor.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteFloor(Long id) {
        String sql = "UPDATE floors SET is_deleted = 1 WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- 5. LOGIC NGHIỆP VỤ KHÁC ---
    public boolean updateStatusCascade(Long floorId, String newStatus) {
        Connection conn = null;
        PreparedStatement pstFloor = null;
        PreparedStatement pstApts = null;
        try {
            conn = Db_connection.getConnection();
            conn.setAutoCommit(false);

            String sqlFloor = "UPDATE floors SET status = ? WHERE id = ?";
            pstFloor = conn.prepareStatement(sqlFloor);
            pstFloor.setString(1, newStatus);
            pstFloor.setLong(2, floorId);
            pstFloor.executeUpdate();

            if ("MAINTENANCE".equalsIgnoreCase(newStatus)) {
                String sqlApt = "UPDATE apartments SET status = 'MAINTENANCE' WHERE floor_id = ? AND is_deleted = 0";
                pstApts = conn.prepareStatement(sqlApt);
                pstApts.setLong(1, floorId);
                pstApts.executeUpdate();
            } else if ("ACTIVE".equalsIgnoreCase(newStatus)) {
                String sqlApt = "UPDATE apartments SET status = 'AVAILABLE' WHERE floor_id = ? AND status = 'MAINTENANCE' AND is_deleted = 0";
                pstApts = conn.prepareStatement(sqlApt);
                pstApts.setLong(1, floorId);
                pstApts.executeUpdate();
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
            }
            return false;
        } finally {
            try {
                if (pstFloor != null) {
                    pstFloor.close();
                }
                if (pstApts != null) {
                    pstApts.close();
                }
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    public boolean canDeleteFloor(Long floorId) {
        String sqlApt = "SELECT COUNT(*) FROM apartments WHERE floor_id = ? AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sqlApt)) {
            pstmt.setLong(1, floorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return false;
                }
            }
        } catch (SQLException e) {
            return false;
        }
        return !hasActiveContracts(floorId);
    }

    public boolean hasActiveContracts(Long floorId) {
        String sql = "SELECT COUNT(*) FROM contracts c "
                + "JOIN apartments a ON c.apartment_id = a.id "
                + "WHERE a.floor_id = ? AND c.status = 'ACTIVE' AND c.is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, floorId);
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

    // --- 6. INNER CLASSES ---
    public static class FloorStats {

        public int totalApartments = 0;
        public int rentedApartments = 0;
        public int ownedApartments = 0;

        public int getAvailableApartments() {
            return totalApartments - (rentedApartments + ownedApartments);
        }

        public int getOccupancyRate() {
            if (totalApartments == 0) {
                return 0;
            }
            return ((rentedApartments + ownedApartments) * 100) / totalApartments;
        }
    }

    public static class FloorWithStats {

        public Floor floor;
        public FloorStats stats;

        public FloorWithStats(Floor f, FloorStats s) {
            this.floor = f;
            this.stats = s;
        }
    }
}
