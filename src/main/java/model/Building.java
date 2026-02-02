package model;

public class Building {

    private Long id;
    private String name;
    private String address;
    private String description;

    // [QUAN TRỌNG] Thêm ID để liên kết với bảng Users
    private Long managerUserId;
    private String managerName; // Giữ lại để hiển thị tên (lấy từ JOIN)

    private String status;
    private boolean isDeleted;

    public Building() {
        this.status = "Đang hoạt động";
    }

    public Building(Long id, String name, String address, String managerName, String description, String đang_hoạt_động, boolean isDeleted) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.managerUserId = managerUserId;
        this.managerName = managerName;
        this.description = description;
        this.status = status;
        this.isDeleted = isDeleted;
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    // [MỚI] Getter/Setter cho Manager ID
    public Long getManagerUserId() {
        return managerUserId;
    }

    public void setManagerUserId(Long managerUserId) {
        this.managerUserId = managerUserId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
