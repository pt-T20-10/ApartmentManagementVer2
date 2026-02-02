package util;

import model.Building;
import model.Floor;
import model.Apartment;

import java.util.ArrayList;
import java.util.List;

/**
 * Building Context Manager (Singleton) Quản lý context hiện tại: Building →
 * Floor → Apartment
 *
 * Dùng để các tab khác (Cư Dân, Hợp Đồng...) biết được user đang xem tòa
 * nhà/tầng/căn hộ nào
 */
public class BuildingContext {

    private static BuildingContext instance;

    // Current context
    private Building currentBuilding;
    private Floor currentFloor;
    private Apartment currentApartment;

    // Listeners để notify khi context thay đổi
    private List<ContextChangeListener> listeners = new ArrayList<>();

    // Private constructor (Singleton)
    private BuildingContext() {
    }

    /**
     * Get singleton instance
     */
    public static BuildingContext getInstance() {
        if (instance == null) {
            instance = new BuildingContext();
        }
        return instance;
    }

    // === Getters ===
    public Building getCurrentBuilding() {
        return currentBuilding;
    }

    public Floor getCurrentFloor() {
        return currentFloor;
    }

    public Apartment getCurrentApartment() {
        return currentApartment;
    }

    public Long getCurrentBuildingId() {
        return currentBuilding != null ? currentBuilding.getId() : null;
    }

    public Long getCurrentFloorId() {
        return currentFloor != null ? currentFloor.getId() : null;
    }

    public Long getCurrentApartmentId() {
        return currentApartment != null ? currentApartment.getId() : null;
    }

    // === Setters with notification ===
    /**
     * Set current building và reset floor, apartment
     */
    public void setCurrentBuilding(Building building) {
        this.currentBuilding = building;
        this.currentFloor = null;
        this.currentApartment = null;
        notifyListeners();
    }

    /**
     * Set current floor (không reset building)
     */
    public void setCurrentFloor(Floor floor) {
        this.currentFloor = floor;
        this.currentApartment = null;
        notifyListeners();
    }

    /**
     * Set current apartment (không reset building, floor)
     */
    public void setCurrentApartment(Apartment apartment) {
        this.currentApartment = apartment;
        notifyListeners();
    }

    /**
     * Clear all context
     */
    public void clearContext() {
        this.currentBuilding = null;
        this.currentFloor = null;
        this.currentApartment = null;
        notifyListeners();
    }

    // === Check methods ===
    /**
     * Check if building context exists
     */
    public boolean hasBuildingContext() {
        return currentBuilding != null;
    }

    /**
     * Check if floor context exists
     */
    public boolean hasFloorContext() {
        return currentFloor != null;
    }

    /**
     * Check if apartment context exists
     */
    public boolean hasApartmentContext() {
        return currentApartment != null;
    }

    // === Listener management ===
    /**
     * Add listener để nhận thông báo khi context thay đổi
     */
    public void addContextChangeListener(ContextChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove listener
     */
    public void removeContextChangeListener(ContextChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners về context change
     */
    private void notifyListeners() {
        for (ContextChangeListener listener : listeners) {
            listener.onContextChanged(this);
        }
    }

    // === Helper methods ===
    /**
     * Get full context path string
     */
    public String getContextPath() {
        StringBuilder path = new StringBuilder();

        if (currentBuilding != null) {
            path.append(currentBuilding.getName());
        }

        if (currentFloor != null) {
            if (path.length() > 0) {
                path.append(" → ");
            }
            path.append(currentFloor.getName());
        }

        if (currentApartment != null) {
            if (path.length() > 0) {
                path.append(" → ");
            }
            path.append(currentApartment.getRoomNumber());
        }

        return path.length() > 0 ? path.toString() : "Chưa chọn tòa nhà";
    }

    /**
     * Get context description for display
     */
    public String getContextDescription() {
        if (currentBuilding == null) {
            return "Vui lòng chọn tòa nhà từ Tab Tòa Nhà";
        }

        StringBuilder desc = new StringBuilder();
        desc.append("Tòa nhà: ").append(currentBuilding.getName());

        if (currentFloor != null) {
            desc.append(" | Tầng: ").append(currentFloor.getName());
        }

        if (currentApartment != null) {
            desc.append(" | Căn hộ: ").append(currentApartment.getRoomNumber());
        }

        return desc.toString();
    }

    @Override
    public String toString() {
        return "BuildingContext{"
                + "building=" + (currentBuilding != null ? currentBuilding.getName() : "null")
                + ", floor=" + (currentFloor != null ? currentFloor.getName() : "null")
                + ", apartment=" + (currentApartment != null ? currentApartment.getRoomNumber() : "null")
                + '}';
    }

    // === Listener Interface ===
    /**
     * Interface cho các component muốn lắng nghe context changes
     */
    public interface ContextChangeListener {

        void onContextChanged(BuildingContext context);
    }
}
