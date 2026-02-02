package model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Model class representing an Invoice
 */
public class Invoice {

    private Long id;
    private Long contractId;
    private Long apartmentId; // Added for compatibility
    private int month;
    private int year;
    private BigDecimal totalAmount;
    private String status; // UNPAID, PAID,
    private Date createdAt;
    private Date paymentDate;
    private boolean isDeleted;

    // Constructors
    public Invoice() {
    }

    public Invoice(Long id, Long contractId, int month, int year, BigDecimal totalAmount,
            String status, Date createdAt, Date paymentDate, boolean isDeleted) {
        this.id = id;
        this.contractId = contractId;
        this.month = month;
        this.year = year;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.paymentDate = paymentDate;
        this.isDeleted = isDeleted;
    }

    // Getters and Setters
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

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    // ===== ALIAS METHODS FOR PANEL COMPATIBILITY =====
    /**
     * Get apartment ID (alternative to contractId for direct apartment
     * reference)
     */
    public Long getApartmentId() {
        return this.apartmentId;
    }

    /**
     * Set apartment ID
     */
    public void setApartmentId(Long apartmentId) {
        this.apartmentId = apartmentId;
    }

    /**
     * Alias for getMonth() - for panel compatibility
     */
    public Integer getInvoiceMonth() {
        return this.month;
    }

    /**
     * Alias for setMonth() - for panel compatibility
     */
    public void setInvoiceMonth(Integer month) {
        this.month = month;
    }

    /**
     * Alias for getYear() - for panel compatibility
     */
    public Integer getInvoiceYear() {
        return this.year;
    }

    /**
     * Alias for setYear() - for panel compatibility
     */
    public void setInvoiceYear(Integer year) {
        this.year = year;
    }

    /**
     * Alias for getStatus() - for panel compatibility
     */
    public String getPaymentStatus() {
        return this.status;
    }

    /**
     * Alias for setStatus() - for panel compatibility
     */
    public void setPaymentStatus(String paymentStatus) {
        this.status = paymentStatus;
    }
}
