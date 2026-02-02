package model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Model class representing ContractService Links contracts with services they
 * use Maps to: contract_services table
 */
public class ContractService {

    private Long id;
    private Long contractId;
    private Long serviceId;
    private Date appliedDate;
    private BigDecimal unitPrice;      // Price at time of application
    private boolean isActive;          // Still active or discontinued
    private Date createdAt;

    // For display purposes (joined data)
    private String serviceName;
    private String unitType;           // KWH, KHOI, THANG, XE

    // ===== CONSTRUCTORS =====
    public ContractService() {
        this.isActive = true;
    }

    public ContractService(Long id, Long contractId, Long serviceId, Date appliedDate,
            BigDecimal unitPrice, boolean isActive, Date createdAt) {
        this.id = id;
        this.contractId = contractId;
        this.serviceId = serviceId;
        this.appliedDate = appliedDate;
        this.unitPrice = unitPrice;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    // Constructor for creating new contract service
    public ContractService(Long contractId, Long serviceId, Date appliedDate, BigDecimal unitPrice) {
        this.contractId = contractId;
        this.serviceId = serviceId;
        this.appliedDate = appliedDate;
        this.unitPrice = unitPrice;
        this.isActive = true;
    }

    // ===== GETTERS AND SETTERS =====
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Date getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(Date appliedDate) {
        this.appliedDate = appliedDate;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
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

    // ===== DISPLAY FIELDS =====
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    // ===== HELPER METHODS =====
    /**
     * Get active status display
     */
    public String getActiveStatusDisplay() {
        return isActive ? "Đang áp dụng" : "Đã ngừng";
    }

    /**
     * Get unit type display in Vietnamese
     */
    public String getUnitTypeDisplay() {
        if (unitType == null) {
            return "";
        }

        switch (unitType.toUpperCase()) {
            case "KWH":
                return "KWh";
            case "KHOI":
                return "Khối";
            case "THANG":
                return "Tháng";
            case "XE":
                return "Xe";
            default:
                return unitType;
        }
    }

    @Override
    public String toString() {
        return "ContractService{"
                + "id=" + id
                + ", contractId=" + contractId
                + ", serviceId=" + serviceId
                + ", serviceName='" + serviceName + '\''
                + ", unitPrice=" + unitPrice
                + ", isActive=" + isActive
                + '}';
    }
}
