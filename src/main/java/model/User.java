package model;

import java.util.Date;

/**
 * User Entity Represents a user account in the system with hierarchical
 * permissions
 *
 * Roles: - ADMIN: System administrator (full access to all buildings) -
 * MANAGER: Building manager (manages one building and its staff) - STAFF:
 * Building staff (works at assigned building, handles
 * residents/contracts/invoices)
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

    // ========== NEW FIELDS (Phase 2 - Hierarchical Permissions) ==========
    private Long buildingId;           // ID of assigned building (NULL for ADMIN)
    private String buildingName;       // Name of building (for display only, not in DB)
    private Long assignedBy;           // ID of user who created/assigned this account
    private String assignedByName;     // Name of assigner (for display only, not in DB)
    private Date assignedDate;         // Date when assigned to building

    // ========== CONSTRUCTORS ==========
    public User() {
    }

    public User(String username, String password, String fullName, String role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.isActive = true;
    }

    public User(Long id, String username, String password, String fullName,
            String role, boolean isActive, Date createdAt, Date lastLogin) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    public User(Long id, String username, String password, String fullName,
            String role, boolean isActive, Date createdAt, Date lastLogin,
            Long buildingId, String buildingName, Long assignedBy,
            String assignedByName, Date assignedDate) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.buildingId = buildingId;
        this.buildingName = buildingName;
        this.assignedBy = assignedBy;
        this.assignedByName = assignedByName;
        this.assignedDate = assignedDate;
    }

    // ========== GETTERS AND SETTERS ==========
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public Long getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(Long assignedBy) {
        this.assignedBy = assignedBy;
    }

    public String getAssignedByName() {
        return assignedByName;
    }

    public void setAssignedByName(String assignedByName) {
        this.assignedByName = assignedByName;
    }

    public Date getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(Date assignedDate) {
        this.assignedDate = assignedDate;
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

    // ========== BUILDING ACCESS METHODS ==========
    public boolean hasBuilding() {
        return this.buildingId != null;
    }

    /**
     * ADMIN access all buildings. MANAGER/STAFF chỉ access building được gán.
     */
    public boolean canAccessBuilding(Long buildingId) {
        if (isAdmin()) {
            return true;
        }
        if (this.buildingId == null) {
            return false;
        }
        return this.buildingId.equals(buildingId);
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
        if (role == null) {
            return "Không xác định";
        }

        switch (role.toUpperCase()) {
            case ROLE_ADMIN:
                return "Quản trị viên";
            case ROLE_MANAGER:
                return "Quản lý tòa nhà";
            case ROLE_STAFF:
                return "Nhân viên";
            default:
                return role;
        }
    }

    public String getFullRoleDisplay() {
        String roleDisplay = getRoleDisplayName();
        if (hasBuilding() && buildingName != null) {
            return roleDisplay + " - " + buildingName;
        }
        return roleDisplay;
    }

    public String getStatusDisplay() {
        return isActive ? "Hoạt động" : "Vô hiệu";
    }

    // ========== VALIDATION ==========
    public boolean hasValidRole() {
        if (role == null) {
            return false;
        }
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
        if (needsBuildingAssignment() && buildingId == null) {
            return getRoleDisplayName() + " phải được gán vào tòa nhà";
        }
        if (isAdmin() && buildingId != null) {
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
        safe.setBuildingId(this.buildingId);
        safe.setBuildingName(this.buildingName);
        safe.setAssignedBy(this.assignedBy);
        safe.setAssignedByName(this.assignedByName);
        safe.setAssignedDate(this.assignedDate);
        return safe;
    }

    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", username='" + username + '\''
                + ", fullName='" + fullName + '\''
                + ", role='" + role + '\''
                + ", isActive=" + isActive
                + ", buildingId=" + buildingId
                + ", buildingName='" + buildingName + '\''
                + ", assignedBy=" + assignedBy
                + ", assignedDate=" + assignedDate
                + '}';
    }
}
