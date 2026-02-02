package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Model class representing a Contract UPDATED: Enhanced logic for RENTAL vs
 * OWNERSHIP contracts
 *
 * RENTAL (Thuê): - Has: signed_date, start_date, end_date - Price: monthly_rent
 * (tiền thuê/tháng) - Can be: renewed, terminated
 *
 * OWNERSHIP (Sở hữu): - Has: signed_date ONLY (no start/end dates) - Price:
 * monthly_rent (giá mua - total purchase price) - Cannot be: renewed (permanent
 * ownership) - Can be: terminated (in special cases)
 */
public class Contract {

    // Primary key
    private Long id;

    // Contract identification
    private String contractNumber;

    // Foreign keys
    private Long apartmentId;
    private Long residentId;

    // Contract type: RENTAL or OWNERSHIP
    private String contractType;

    // Dates
    private Date signedDate;        // Ngày ký - REQUIRED for all types
    private Date startDate;         // Ngày bắt đầu - ONLY for RENTAL
    private Date endDate;           // Ngày kết thúc - ONLY for RENTAL
    private Date terminatedDate;    // Ngày kết thúc sớm

    // Financial
    private BigDecimal depositAmount;     // Tiền cọc
    private BigDecimal monthlyRent;       // Dual purpose: 
    // - RENTAL: tiền thuê/tháng
    // - OWNERSHIP: giá mua

    // Status
    private String status;

    // Notes
    private String notes;

    // Flags
    private boolean isDeleted;

    // Audit fields
    private Date createdAt;
    private Date updatedAt;

    // Tenant info (for display)
    private String tenantName;
    private String tenantPhone;

    // ===== CONSTRUCTORS =====
    public Contract() {
        this.contractType = "RENTAL";
        this.status = "ACTIVE";
        this.isDeleted = false;
    }

    public Contract(Long id, String contractNumber, Long apartmentId, Long residentId,
            String contractType, Date signedDate, Date startDate, Date endDate,
            Date terminatedDate, BigDecimal depositAmount, BigDecimal monthlyRent,
            String status, String notes, boolean isDeleted, Date createdAt, Date updatedAt) {
        this.id = id;
        this.contractNumber = contractNumber;
        this.apartmentId = apartmentId;
        this.residentId = residentId;
        this.contractType = contractType;
        this.signedDate = signedDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.terminatedDate = terminatedDate;
        this.depositAmount = depositAmount;
        this.monthlyRent = monthlyRent;
        this.status = status;
        this.notes = notes;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ===== GETTERS AND SETTERS =====
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public Long getApartmentId() {
        return apartmentId;
    }

    public void setApartmentId(Long apartmentId) {
        this.apartmentId = apartmentId;
    }

    public Long getResidentId() {
        return residentId;
    }

    public void setResidentId(Long residentId) {
        this.residentId = residentId;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public Date getSignedDate() {
        return signedDate;
    }

    public void setSignedDate(Date signedDate) {
        this.signedDate = signedDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getTerminatedDate() {
        return terminatedDate;
    }

    public void setTerminatedDate(Date terminatedDate) {
        this.terminatedDate = terminatedDate;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getTenantPhone() {
        return tenantPhone;
    }

    public void setTenantPhone(String tenantPhone) {
        this.tenantPhone = tenantPhone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ===== HELPER METHODS =====
    /**
     * Check if this is a rental contract
     */
    public boolean isRental() {
        return "RENTAL".equals(contractType);
    }

    /**
     * Check if this is an ownership contract
     */
    public boolean isOwnership() {
        return "OWNERSHIP".equals(contractType);
    }

    /**
     * Get price label based on contract type
     */
    public String getPriceLabel() {
        return isOwnership() ? "Giá mua" : "Tiền thuê/tháng";
    }

    /**
     * Get price amount (alias for clarity)
     */
    public BigDecimal getPriceAmount() {
        return this.monthlyRent;
    }

    /**
     * Check if contract can be renewed Only RENTAL contracts can be renewed
     */
    public boolean canBeRenewed() {
        return isRental() && isActive();
    }

    /**
     * Check if contract can be terminated
     */
    public boolean canBeTerminated() {
        return "ACTIVE".equals(status) && !isDeleted;
    }

    /**
     * Get display status based on contract type and dates
     */
    public String getStatusDisplay() {
        if ("TERMINATED".equals(status)) {
            return "Đã kết thúc";
        }

        // OWNERSHIP contracts are always "Đang hiệu lực" unless terminated
        if (isOwnership()) {
            return "Đang hiệu lực";
        }

        // RENTAL contracts check end date
        if (endDate == null) {
            return "Đang hiệu lực"; // Indefinite
        }

        LocalDate today = LocalDate.now();
        LocalDate contractEndDate = new java.sql.Date(endDate.getTime()).toLocalDate();
        long daysLeft = ChronoUnit.DAYS.between(today, contractEndDate);

        if (daysLeft < 0) {
            return "Đã hết hạn";
        } else if (daysLeft <= 30) {
            return "Sắp hết hạn";
        } else {
            return "Đang hiệu lực";
        }
    }

    /**
     * Get days left until expiration (RENTAL only)
     */
    public Long getDaysLeft() {
        if (isOwnership() || endDate == null) {
            return null; // No expiration for ownership
        }

        LocalDate today = LocalDate.now();
        LocalDate contractEndDate = new java.sql.Date(endDate.getTime()).toLocalDate();
        return ChronoUnit.DAYS.between(today, contractEndDate);
    }

    /**
     * Check if contract is active
     */
    public boolean isActive() {
        if ("TERMINATED".equals(status)) {
            return false;
        }

        // OWNERSHIP is always active unless terminated
        if (isOwnership()) {
            return true;
        }

        // RENTAL check expiration
        if (endDate == null) {
            return true; // Indefinite
        }

        Long daysLeft = getDaysLeft();
        return daysLeft != null && daysLeft >= 0;
    }

    /**
     * Get contract type display
     */
    public String getContractTypeDisplay() {
        return isRental() ? "Thuê" : "Sở hữu";
    }

    /**
     * Validate contract data based on type
     */
    public String validate() {
        if (contractType == null) {
            return "Loại hợp đồng không được để trống";
        }

        if (signedDate == null) {
            return "Ngày ký không được để trống";
        }

        if (isRental()) {
            if (startDate == null) {
                return "Hợp đồng thuê phải có ngày bắt đầu";
            }
            if (endDate == null) {
                return "Hợp đồng thuê phải có ngày kết thúc";
            }
            if (endDate.before(startDate)) {
                return "Ngày kết thúc phải sau ngày bắt đầu";
            }
        } else if (isOwnership()) {
            // Ownership should NOT have start/end dates
            if (startDate != null || endDate != null) {
                return "Hợp đồng sở hữu không có ngày bắt đầu/kết thúc";
            }
        }

        if (monthlyRent == null || monthlyRent.compareTo(BigDecimal.ZERO) <= 0) {
            return "Giá tiền phải lớn hơn 0";
        }

        if (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) < 0) {
            return "Tiền cọc không hợp lệ";
        }

        return null; // Valid
    }

    // ===== ALIAS METHODS FOR COMPATIBILITY =====
    public BigDecimal getDeposit() {
        return this.depositAmount;
    }

    public void setDeposit(BigDecimal deposit) {
        this.depositAmount = deposit;
    }

    @Override
    public String toString() {
        return "Contract{"
                + "id=" + id
                + ", number='" + contractNumber + '\''
                + ", type='" + contractType + '\''
                + ", status='" + status + '\''
                + ", price=" + (monthlyRent != null ? monthlyRent : "N/A")
                + '}';
    }
}
