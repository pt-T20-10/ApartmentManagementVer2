package model;

/**
 * Floor Entity Represents a floor in a building
 */
public class Floor {

    private Long id;
    private Long buildingId;
    private int floorNumber;
    private String name;
    private String description;
    private boolean isDeleted;

    // --- THUỘC TÍNH MỚI (STEP 2) ---
    private String status; // "Đang hoạt động", "Đang bảo trì", "Đóng cửa"...
    // -------------------------------

    // For display purposes
    private String buildingName;

    // Constructors
    public Floor() {
        // Mặc định khi tạo mới
        this.status = "Đang hoạt động";
    }

    // Constructor đầy đủ
    public Floor(Long id, Long buildingId, int floorNumber, String name, String description, String status, boolean isDeleted) {
        this.id = id;
        this.buildingId = buildingId;
        this.floorNumber = floorNumber;
        this.name = name;
        this.description = description;
        this.status = status;
        this.isDeleted = isDeleted;
    }

    // Constructor rút gọn (Thường dùng khi thêm mới)
    public Floor(Long buildingId, int floorNumber, String name) {
        this.buildingId = buildingId;
        this.floorNumber = floorNumber;
        this.name = name;
        this.status = "Đang hoạt động";
        this.isDeleted = false;
    }

    // --- Getters and Setters MỚI ---
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    // -------------------------------

    // --- Các Getter/Setter cũ (Giữ nguyên) ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        this.isDeleted = deleted;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    @Override
    public String toString() {
        return "Floor{"
                + "id=" + id
                + ", buildingId=" + buildingId
                + ", floorNumber=" + floorNumber
                + ", name='" + name + '\''
                + ", status='" + status + '\''
                + '}';
    }
}
