package model;

import java.math.BigDecimal;

/**
 * Apartment Entity Represents an apartment/unit in a floor Note: base_price has
 * been removed - pricing is now handled in contracts
 */
public class Apartment {

    private Long id;
    private Long floorId;
    private String roomNumber;
    private Double area;
    private String status; // AVAILABLE, RENTED, MAINTENANCE
    private String description;
    private boolean isDeleted;

    // --- 3 THUỘC TÍNH MỚI (Đã cập nhật) ---
    private String apartmentType;   // Loại căn hộ (Studio, Standard...)
    private Integer bedroomCount;   // Số phòng ngủ
    private Integer bathroomCount;  // Số phòng tắm

    // For display purposes
    private String floorName;
    private String buildingName;

    // --- Constructors ---
    public Apartment() {
        // Giá trị mặc định
        this.apartmentType = "Standard";
        this.bedroomCount = 1;
        this.bathroomCount = 1;
    }

    public Apartment(Long id, Long floorId, String roomNumber, Double area, String status,
            String description, boolean isDeleted,
            String apartmentType, Integer bedroomCount, Integer bathroomCount) {
        this.id = id;
        this.floorId = floorId;
        this.roomNumber = roomNumber;
        this.area = area;
        this.status = status;
        this.description = description;
        this.isDeleted = isDeleted;
        this.apartmentType = apartmentType;
        this.bedroomCount = bedroomCount;
        this.bathroomCount = bathroomCount;
    }

    // Constructor rút gọn
    public Apartment(Long floorId, String roomNumber, Double area, String status) {
        this.floorId = floorId;
        this.roomNumber = roomNumber;
        this.area = area;
        this.status = status;
        this.isDeleted = false;
        // Mặc định
        this.apartmentType = "Standard";
        this.bedroomCount = 1;
        this.bathroomCount = 1;
    }

    // --- Getters and Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFloorId() {
        return floorId;
    }

    public void setFloorId(Long floorId) {
        this.floorId = floorId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Double getArea() {
        return area;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public void setArea(BigDecimal area) {
        this.area = area != null ? area.doubleValue() : null;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    // --- Getters & Setters cho 3 thuộc tính mới ---
    public String getApartmentType() {
        return apartmentType;
    }

    public void setApartmentType(String apartmentType) {
        this.apartmentType = apartmentType;
    }

    public Integer getBedroomCount() {
        return bedroomCount;
    }

    public void setBedroomCount(Integer bedroomCount) {
        this.bedroomCount = bedroomCount;
    }

    public Integer getBathroomCount() {
        return bathroomCount;
    }

    public void setBathroomCount(Integer bathroomCount) {
        this.bathroomCount = bathroomCount;
    }

    // --- Display Helpers ---
    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    // Alias methods (Giữ lại để tương thích code cũ nếu cần)
    public String getApartmentNumber() {
        return this.roomNumber;
    }

    public void setApartmentNumber(String apartmentNumber) {
        this.roomNumber = apartmentNumber;
    }

    // Alias cho bedrooms cũ (trỏ về bedroomCount mới)
    public Integer getBedrooms() {
        return bedroomCount;
    }

    public void setBedrooms(Integer bedrooms) {
        this.bedroomCount = bedrooms;
    }

    @Override
    public String toString() {
        return "Apartment{"
                + "id=" + id
                + ", room='" + roomNumber + '\''
                + ", type='" + apartmentType + '\''
                + ", bed=" + bedroomCount
                + ", bath=" + bathroomCount
                + '}';
    }
}
