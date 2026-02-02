package util;

import model.User;

/**
 * Permission Manager - UPDATED for Many-to-Many Buildings
 * RBAC (Role-Based Access Control) với support nhiều buildings
 */
public class PermissionManager {

    // ==================== MODULE KEYS ====================
    public static final String MODULE_DASHBOARD = "DASHBOARD";
    public static final String MODULE_BUILDINGS = "BUILDINGS";
    public static final String MODULE_FLOORS = "FLOORS";
    public static final String MODULE_APARTMENTS = "APARTMENTS";
    public static final String MODULE_RESIDENTS = "RESIDENTS";
    public static final String MODULE_CONTRACTS = "CONTRACTS";
    public static final String MODULE_SERVICES = "SERVICES";
    public static final String MODULE_INVOICES = "INVOICES";
    public static final String MODULE_REPORTS = "REPORTS";
    public static final String MODULE_MY_STAFF = "MY_STAFF";

    // ==================== ROLES ====================
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_STAFF = "STAFF";

    private static PermissionManager instance;

    private PermissionManager() {
    }

    public static PermissionManager getInstance() {
        if (instance == null) {
            instance = new PermissionManager();
        }
        return instance;
    }

    // ==================== SESSION ====================
    private User getCurrentUser() {
        return SessionManager.getInstance().getCurrentUser();
    }

    private String getCurrentRole() {
        User user = getCurrentUser();
        return user != null ? user.getRole() : null;
    }

    // ==================== BUILDING FILTER (UPDATED) ====================

    /**
     * ✅ DEPRECATED: Trả về building đầu tiên (backward compat)
     * Dùng getBuildingIds() cho nhiều buildings
     */
    @Deprecated
    public Long getBuildingFilter() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return null;
        if (currentUser.isAdmin()) return null;
        return currentUser.getBuildingId(); // Returns first building
    }

    /**
     * ✅ NEW: Lấy tất cả building IDs của user
     */
    public java.util.List<Long> getBuildingIds() {
        User currentUser = getCurrentUser();
        if (currentUser == null || currentUser.isAdmin()) {
            return null; // ADMIN sees all
        }
        return currentUser.getBuildingIds();
    }

    /**
     * Check if current user can access a specific building.
     * ✅ UPDATED: Dùng List.contains() thay vì ==
     */
    public boolean canAccessBuilding(Long buildingId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;
        return currentUser.canAccessBuilding(buildingId);
    }

    /**
     * MANAGER có thể quản lý STAFF của tòa mình.
     * ADMIN không cần — dùng UserManagementPanel riêng.
     */
    public boolean canManageStaff() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;
        return currentUser.canManageStaff();
    }

    /**
     * Chỉ ADMIN có thể tạo/sửa MANAGER.
     */
    public boolean canManageManagers() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;
        return currentUser.canManageManagers();
    }

    // ==================== ACCESS (MENU / VIEW) ====================

    /**
     * Kiểm tra role có quyền truy cập module chưa.
     * 
     * ADMIN → tất cả
     * MANAGER → Buildings, Floors, Apartments, Residents, Contracts, MyStaff, Reports
     * STAFF → Buildings, Floors, Apartments, Residents, Contracts, Services, Invoices
     */
    public boolean canAccess(String module) {
        String role = getCurrentRole();
        if (role == null) return false;

        if (ROLE_ADMIN.equalsIgnoreCase(role)) {
            return true;
        }

        if (ROLE_MANAGER.equalsIgnoreCase(role)) {
            switch (module) {
                case MODULE_DASHBOARD:
                case MODULE_BUILDINGS:
                case MODULE_FLOORS:
                case MODULE_APARTMENTS:
                case MODULE_RESIDENTS:
                case MODULE_CONTRACTS:
                case MODULE_MY_STAFF:
                case MODULE_REPORTS:
                    return true;
                default:
                    return false;
            }
        }

        if (ROLE_STAFF.equalsIgnoreCase(role)) {
            switch (module) {
                case MODULE_DASHBOARD:
                case MODULE_BUILDINGS:
                case MODULE_FLOORS:
                case MODULE_APARTMENTS:
                case MODULE_RESIDENTS:
                case MODULE_CONTRACTS:
                case MODULE_SERVICES:
                case MODULE_INVOICES:
                    return true;
                default:
                    return false;
            }
        }

        return false;
    }

    // ==================== CRUD ====================

    /**
     * ADMIN → tất cả modules
     * MANAGER → Floors, Apartments, MyStaff
     * STAFF → Residents, Contracts, Services, Invoices
     */
    public boolean canAdd(String module) {
        String role = getCurrentRole();
        if (role == null) return false;

        if (ROLE_ADMIN.equalsIgnoreCase(role)) {
            return true;
        }

        if (ROLE_MANAGER.equalsIgnoreCase(role)) {
            return module.equals(MODULE_FLOORS)
                    || module.equals(MODULE_APARTMENTS)
                    || module.equals(MODULE_MY_STAFF);
        }

        if (ROLE_STAFF.equalsIgnoreCase(role)) {
            return module.equals(MODULE_RESIDENTS)
                    || module.equals(MODULE_CONTRACTS)
                    || module.equals(MODULE_SERVICES)
                    || module.equals(MODULE_INVOICES);
        }

        return false;
    }

    public boolean canEdit(String module) {
        return canAdd(module);
    }

    public boolean canDelete(String module) {
        return canAdd(module);
    }

    // ==================== ROLE HELPERS ====================

    public boolean isAdmin() {
        return ROLE_ADMIN.equalsIgnoreCase(getCurrentRole());
    }

    public boolean isManager() {
        return ROLE_MANAGER.equalsIgnoreCase(getCurrentRole());
    }

    public boolean isStaff() {
        return ROLE_STAFF.equalsIgnoreCase(getCurrentRole());
    }

    // ==================== UI HELPERS (UPDATED) ====================

    public void showAccessDeniedMessage(java.awt.Component parent, String action) {
        javax.swing.JOptionPane.showMessageDialog(
                parent,
                "Bạn không có quyền " + action + "!\n"
                + "Vai trò: " + getCurrentRole(),
                "Không Có Quyền Truy Cập",
                javax.swing.JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * ✅ UPDATED: Hiển thị tất cả tòa nhà của user (comma-separated)
     * @return Tên tòa nhà hoặc "Tất cả tòa nhà" nếu ADMIN
     */
    public String getCurrentBuildingDisplay() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return "N/A";

        if (currentUser.isAdmin()) {
            return "Tất cả tòa nhà";
        }

        if (currentUser.getBuildingNames() != null && !currentUser.getBuildingNames().isEmpty()) {
            return String.join(", ", currentUser.getBuildingNames());
        }

        if (currentUser.hasBuilding()) {
            return "Tòa ID: " + currentUser.getBuildingIds();
        }

        return "Chưa gán tòa";
    }

    /**
     * Validate + show dialog nếu không có quyền.
     * 
     * @return true nếu được cho phép
     */
    public boolean validateBuildingAccess(Long buildingId, String action, java.awt.Component parent) {
        if (!canAccessBuilding(buildingId)) {
            javax.swing.JOptionPane.showMessageDialog(
                    parent,
                    "Bạn không có quyền " + action + " tòa nhà này!\n"
                    + "Bạn chỉ có quyền truy cập: " + getCurrentBuildingDisplay(),
                    "Không Có Quyền Truy Cập",
                    javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return false;
        }
        return true;
    }
}