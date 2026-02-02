package model;

import java.math.BigDecimal;

/**
 * Service Entity Represents a service (electricity, water, management fee,
 * etc.)
 */
public class Service {

    private Long id;
    private String serviceName;
    private BigDecimal unitPrice;
    private String unitType; // KWH, KHOI, THANG, XE
    private String description; // Added for compatibility
    private boolean isMandatory;
    private boolean isDeleted;

    // Constructors
    public Service() {
    }

    public Service(Long id, String serviceName, BigDecimal unitPrice, String unitType,
            boolean isMandatory, boolean isDeleted) {
        this.id = id;
        this.serviceName = serviceName;
        this.unitPrice = unitPrice;
        this.unitType = unitType;
        this.isMandatory = isMandatory;
        this.isDeleted = isDeleted;
    }

    public Service(String serviceName, BigDecimal unitPrice, String unitType, boolean isMandatory) {
        this.serviceName = serviceName;
        this.unitPrice = unitPrice;
        this.unitType = unitType;
        this.isMandatory = isMandatory;
        this.isDeleted = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    // ===== ALIAS METHODS FOR PANEL COMPATIBILITY =====
    /**
     * Alias for getServiceName() - for panel compatibility
     */
    public String getName() {
        return this.serviceName;
    }

    /**
     * Alias for setServiceName() - for panel compatibility
     */
    public void setName(String name) {
        this.serviceName = name;
    }

    /**
     * Alias for getUnitType() - for panel compatibility
     */
    public String getUnit() {
        return this.unitType;
    }

    /**
     * Alias for setUnitType() - for panel compatibility
     */
    public void setUnit(String unit) {
        this.unitType = unit;
    }

    /**
     * Get service description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Set service description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Service{"
                + "id=" + id
                + ", serviceName='" + serviceName + '\''
                + ", unitPrice=" + unitPrice
                + ", unitType='" + unitType + '\''
                + '}';
    }
}
