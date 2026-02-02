package model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User Entity - UPDATED for Many-to-Many Buildings
 * 
 * Roles:
 * - ADMIN: System administrator (full access to all buildings)
 * - MANAGER: Building manager (manages MULTIPLE buildings and their staff)
 * - STAFF: Building staff (works at assigned buildings, handles residents/contracts/invoices)
 */
public class User {

    // ========== ROLE CONSTANTS ==========
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_STAFF = "STAFF";

    // ========== EXISTING FIELDS ==========
    private Long id;
    private String username;
    private String password;
    private String fullName;
    private String role;
    private boolean isActive;
    private Date createdAt;
    private Date lastLogin;

    // ========== NEW FIELDS (Phase 2 → Phase 5: Many-to-Many) ==========
    private List<Long> buildingIds;           // IDs of assigned buildings (NULL/empty for ADMIN)
    private List<String> buildingNames;       // Names of buildings (for display only, not in DB)
    private Long assignedBy;                  // ID of user who created/assigned this account
    private String assignedByName;            // Name of assigner (for display only, not in DB)
    private Date assignedDate;                // Date when assigned (kept for backward compat)

    // ========== CONSTRUCTORS ==========

    public User() {
        this.buildingIds = new ArrayList<>();
        this.buildingNames = new ArrayList<>();
    }

    public User(String username, String password, String fullName, String role) {
        this();
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.isActive = true;
    }

    public User(Long id, String username, String password, String fullName,
                String role, boolean isActive, Date createdAt, Date lastLogin) {
        this();
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    // ========== GETTERS AND SETTERS ==========

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getLastLogin() { return lastLogin; }
    public void setLastLogin(Date lastLogin) { this.lastLogin = lastLogin; }

    // --- Building IDs (Many-to-Many) ---
    public List<Long> getBuildingIds() { return buildingIds; }
    public void setBuildingIds(List<Long> buildingIds) { 
        this.buildingIds = buildingIds != null ? buildingIds : new ArrayList<>(); 
    }

    public List<String> getBuildingNames() { return buildingNames; }
    public void setBuildingNames(List<String> buildingNames) { 
        this.buildingNames = buildingNames != null ? buildingNames : new ArrayList<>(); 
    }

    public Long getAssignedBy() { return assignedBy; }
    public void setAssignedBy(Long assignedBy) { this.assignedBy = assignedBy; }

    public String getAssignedByName() { return assignedByName; }
    public void setAssignedByName(String assignedByName) { this.assignedByName = assignedByName; }

    public Date getAssignedDate() { return assignedDate; }
    public void setAssignedDate(Date assignedDate) { this.assignedDate = assignedDate; }

    // ========== BACKWARD COMPATIBILITY ==========
    // Giữ để không break code cũ — trả về building đầu tiên
    
    @Deprecated
    public Long getBuildingId() {
        return (buildingIds != null && !buildingIds.isEmpty()) ? buildingIds.get(0) : null;
    }

    @Deprecated
    public void setBuildingId(Long buildingId) {
        if (buildingId != null) {
            this.buildingIds = new ArrayList<>();
            this.buildingIds.add(buildingId);
        } else {
            this.buildingIds = new ArrayList<>();
        }
    }

    @Deprecated
    public String getBuildingName() {
        return (buildingNames != null && !buildingNames.isEmpty()) ? buildingNames.get(0) : null;
    }

    @Deprecated
    public void setBuildingName(String buildingName) {
        if (buildingName != null) {
            this.buildingNames = new ArrayList<>();
            this.buildingNames.add(buildingName);
        } else {
            this.buildingNames = new ArrayList<>();
        }
    }

    // ========== ROLE CHECK METHODS ==========

    public boolean isAdmin() {
        return ROLE_ADMIN.equalsIgnoreCase(this.role);
    }

    public boolean isManager() {
        return ROLE_MANAGER.equalsIgnoreCase(this.role);
    }

    public boolean isStaff() {
        return ROLE_STAFF.equalsIgnoreCase(this.role);
    }

    // ========== BUILDING ACCESS METHODS (UPDATED) ==========

    public boolean hasBuilding() {
        return this.buildingIds != null && !this.buildingIds.isEmpty();
    }

    /**
     * ADMIN access all buildings.
     * MANAGER/STAFF chỉ access buildings được gán.
     */
    public boolean canAccessBuilding(Long buildingId) {
        if (isAdmin()) return true;
        if (buildingIds == null || buildingIds.isEmpty()) return false;
        return buildingIds.contains(buildingId);
    }

    /**
     * MANAGER có thể tạo/quản lý STAFF.
     */
    public boolean canManageStaff() {
        return isAdmin() || isManager();
    }

    /**
     * Chỉ ADMIN tạo/sửa MANAGER.
     */
    public boolean canManageManagers() {
        return isAdmin();
    }

    // ========== DISPLAY HELPERS ==========

    public String getRoleDisplayName() {
        if (role == null) return "Không xác định";

        switch (role.toUpperCase()) {
            case ROLE_ADMIN:    return "Quản trị viên";
            case ROLE_MANAGER:  return "Quản lý tòa nhà";
            case ROLE_STAFF:    return "Nhân viên";
            default:            return role;
        }
    }

    public String getFullRoleDisplay() {
        String roleDisplay = getRoleDisplayName();
        if (hasBuilding() && buildingNames != null && !buildingNames.isEmpty()) {
            return roleDisplay + " - " + String.join(", ", buildingNames);
        }
        return roleDisplay;
    }

    public String getStatusDisplay() {
        return isActive ? "Hoạt động" : "Vô hiệu";
    }

    // ========== VALIDATION ==========

    public boolean hasValidRole() {
        if (role == null) return false;
        String upper = role.toUpperCase();
        return ROLE_ADMIN.equals(upper)
            || ROLE_MANAGER.equals(upper)
            || ROLE_STAFF.equals(upper);
    }

    /**
     * MANAGER + STAFF phải có building.
     */
    public boolean needsBuildingAssignment() {
        return !isAdmin();
    }

    /**
     * @return error message nếu invalid, null nếu valid
     */
    public String validate() {
        if (username == null || username.trim().isEmpty()) {
            return "Username không được để trống";
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Họ tên không được để trống";
        }
        if (!hasValidRole()) {
            return "Vai trò không hợp lệ";
        }
        if (needsBuildingAssignment() && !hasBuilding()) {
            return getRoleDisplayName() + " phải được gán vào ít nhất 1 tòa nhà";
        }
        if (isAdmin() && hasBuilding()) {
            return "Quản trị viên không được gán vào tòa nhà cụ thể";
        }
        return null;
    }

    // ========== UTILITY ==========

    /**
     * Safe copy không có password — dùng cho display.
     */
    public User getSafeUser() {
        User safe = new User();
        safe.setId(this.id);
        safe.setUsername(this.username);
        safe.setFullName(this.fullName);
        safe.setRole(this.role);
        safe.setActive(this.isActive);
        safe.setCreatedAt(this.createdAt);
        safe.setLastLogin(this.lastLogin);
        safe.setBuildingIds(new ArrayList<>(this.buildingIds));
        safe.setBuildingNames(new ArrayList<>(this.buildingNames));
        safe.setAssignedBy(this.assignedBy);
        safe.setAssignedByName(this.assignedByName);
        safe.setAssignedDate(this.assignedDate);
        return safe;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                ", isActive=" + isActive +
                ", buildingIds=" + buildingIds +
                ", buildingNames=" + buildingNames +
                ", assignedBy=" + assignedBy +
                ", assignedDate=" + assignedDate +
                '}';
    }
}