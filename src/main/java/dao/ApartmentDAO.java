package dao;

import model.Apartment;
import model.User;
import connection.Db_connection;
import util.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ApartmentDAO {

    // --- HELPER ---
    private Apartment mapResultSetToApartment(ResultSet rs) throws SQLException {
        Apartment apartment = new Apartment();
        apartment.setId(rs.getLong("id"));
        apartment.setFloorId(rs.getLong("floor_id"));
        apartment.setRoomNumber(rs.getString("room_number"));
        apartment.setArea(rs.getDouble("area"));
        apartment.setStatus(rs.getString("status"));
        apartment.setDescription(rs.getString("description"));
        apartment.setDeleted(rs.getBoolean("is_deleted"));
        apartment.setApartmentType(rs.getString("apartment_type"));
        apartment.setBedroomCount(rs.getInt("bedroom_count"));
        apartment.setBathroomCount(rs.getInt("bathroom_count"));
        return apartment;
    }

    public List<Apartment> getAllApartments() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        List<Apartment> apartments = new ArrayList<>();
        String sql = "SELECT a.* FROM apartments a "
                + "JOIN floors f ON a.floor_id = f.id "
                + "WHERE a.is_deleted = 0";

        if (currentUser != null && (currentUser.isManager() || currentUser.isStaff()) && currentUser.getBuildingId() != null) {
            sql += " AND f.building_id = ?";
        }

        sql += " ORDER BY a.room_number";

        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            if (currentUser != null && (currentUser.isManager() || currentUser.isStaff()) && currentUser.getBuildingId() != null) {
                ps.setLong(1, currentUser.getBuildingId());
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                apartments.add(mapResultSetToApartment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return apartments;
    }

    public Apartment getApartmentById(Long id) {
        String sql = "SELECT * FROM apartments WHERE id = ? AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToApartment(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insertApartment(Apartment apartment) {
        String sql = "INSERT INTO apartments (floor_id, room_number, area, status, description, apartment_type, bedroom_count, bathroom_count, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, apartment.getFloorId());
            pstmt.setString(2, apartment.getRoomNumber());
            pstmt.setDouble(3, apartment.getArea());
            pstmt.setString(4, apartment.getStatus());
            pstmt.setString(5, apartment.getDescription());
            pstmt.setString(6, apartment.getApartmentType());
            pstmt.setInt(7, apartment.getBedroomCount());
            pstmt.setInt(8, apartment.getBathroomCount());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateApartment(Apartment apartment) {
        String sql = "UPDATE apartments SET floor_id=?, room_number=?, area=?, status=?, description=?, apartment_type=?, bedroom_count=?, bathroom_count=? WHERE id=?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, apartment.getFloorId());
            pstmt.setString(2, apartment.getRoomNumber());
            pstmt.setDouble(3, apartment.getArea());
            pstmt.setString(4, apartment.getStatus());
            pstmt.setString(5, apartment.getDescription());
            pstmt.setString(6, apartment.getApartmentType());
            pstmt.setInt(7, apartment.getBedroomCount());
            pstmt.setInt(8, apartment.getBathroomCount());
            pstmt.setLong(9, apartment.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteApartment(Long id) {
        String sql = "UPDATE apartments SET is_deleted = 1 WHERE id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- SUPPORT METHODS ---
    public List<Apartment> getApartmentsByFloorId(Long floorId) {
        List<Apartment> apartments = new ArrayList<>();
        String sql = "SELECT * FROM apartments WHERE floor_id = ? AND is_deleted = 0 ORDER BY room_number";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, floorId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                apartments.add(mapResultSetToApartment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return apartments;
    }

    public List<Apartment> getApartmentsByBuildingId(Long buildingId) {
        List<Apartment> apartments = new ArrayList<>();
        String sql = "SELECT a.* FROM apartments a JOIN floors f ON a.floor_id = f.id WHERE f.building_id = ? AND a.is_deleted = 0 ORDER BY a.room_number";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, buildingId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                apartments.add(mapResultSetToApartment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return apartments;
    }

    // --- STATISTICS ---
    public int countApartments() {
        String sql = "SELECT COUNT(*) FROM apartments WHERE is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countAvailableApartments() {
        String sql = "SELECT COUNT(*) FROM apartments WHERE status = 'AVAILABLE' AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countRentedApartments() {
        String sql = "SELECT COUNT(*) FROM apartments WHERE (status = 'RENTED' OR status = 'OWNED') AND is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int countApartmentsByBuilding(Long buildingId) {
        String sql = "SELECT COUNT(*) FROM apartments a JOIN floors f ON a.floor_id = f.id WHERE f.building_id = ? AND a.is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (buildingId != null) {
                ps.setLong(1, buildingId);
            } else {
                return countApartments();
            }
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

    public int countAvailableApartmentsByBuilding(Long buildingId) {
        String sql = "SELECT COUNT(*) FROM apartments a JOIN floors f ON a.floor_id = f.id WHERE f.building_id = ? AND a.status = 'AVAILABLE' AND a.is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (buildingId != null) {
                ps.setLong(1, buildingId);
            } else {
                return countAvailableApartments();
            }
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

    public int countRentedApartmentsByBuilding(Long buildingId) {
        String sql = "SELECT COUNT(*) FROM apartments a JOIN floors f ON a.floor_id = f.id WHERE f.building_id = ? AND (a.status = 'RENTED' OR a.status = 'OWNED') AND a.is_deleted = 0";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (buildingId != null) {
                ps.setLong(1, buildingId);
            } else {
                return countRentedApartments();
            }
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

    public boolean hasHistory(Long apartmentId) {
        String sql = "SELECT COUNT(*) FROM contracts WHERE apartment_id = ?";
        try (Connection conn = Db_connection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, apartmentId);
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
}
