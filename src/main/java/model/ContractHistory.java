package model;

import java.util.Date;

/**
 * Model class representing ContractHistory Tracks all changes made to contracts
 * Maps to: contract_history table
 */
public class ContractHistory {

    private Long id;
    private Long contractId;
    private String action;              // CREATED, RENEWED, EXTENDED, TERMINATED, STATUS_CHANGED
    private String oldValue;            // Old value (JSON or text)
    private String newValue;            // New value (JSON or text)
    private Date oldEndDate;            // For RENEWED/EXTENDED
    private Date newEndDate;            // For RENEWED/EXTENDED
    private String reason;              // Reason for change
    private Long createdBy;             // User ID who made the change
    private Date createdAt;

    // For display purposes
    private String createdByName;       // User full name

    // ===== CONSTRUCTORS =====
    public ContractHistory() {
    }

    public ContractHistory(Long id, Long contractId, String action, String oldValue,
            String newValue, Date oldEndDate, Date newEndDate, String reason,
            Long createdBy, Date createdAt) {
        this.id = id;
        this.contractId = contractId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.oldEndDate = oldEndDate;
        this.newEndDate = newEndDate;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    // Constructor for creating new history record
    public ContractHistory(Long contractId, String action, String reason) {
        this.contractId = contractId;
        this.action = action;
        this.reason = reason;
        this.createdAt = new Date();
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public Date getOldEndDate() {
        return oldEndDate;
    }

    public void setOldEndDate(Date oldEndDate) {
        this.oldEndDate = oldEndDate;
    }

    public Date getNewEndDate() {
        return newEndDate;
    }

    public void setNewEndDate(Date newEndDate) {
        this.newEndDate = newEndDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    // ===== HELPER METHODS =====
    /**
     * Get action display in Vietnamese
     */
    public String getActionDisplay() {
        if (action == null) {
            return "";
        }

        switch (action.toUpperCase()) {
            case "CREATED":
                return "Tạo mới";
            case "RENEWED":
                return "Gia hạn";
            case "EXTENDED":
                return "Kéo dài";
            case "TERMINATED":
                return "Kết thúc";
            case "STATUS_CHANGED":
                return "Thay đổi trạng thái";
            default:
                return action;
        }
    }

    /**
     * Get icon for action type
     */
    public String getActionIcon() {
        if (action == null) {
            return "📝";
        }

        switch (action.toUpperCase()) {
            case "CREATED":
                return "✨";
            case "RENEWED":
            case "EXTENDED":
                return "🔄";
            case "TERMINATED":
                return "❌";
            case "STATUS_CHANGED":
                return "🔀";
            default:
                return "📝";
        }
    }

    /**
     * Get formatted change description
     */
    public String getChangeDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(getActionDisplay());

        if (oldEndDate != null && newEndDate != null) {
            desc.append(": Gia hạn từ ")
                    .append(new java.text.SimpleDateFormat("dd/MM/yyyy").format(oldEndDate))
                    .append(" đến ")
                    .append(new java.text.SimpleDateFormat("dd/MM/yyyy").format(newEndDate));
        } else if (oldValue != null && newValue != null) {
            desc.append(": ").append(newValue);
        }

        return desc.toString();
    }

    @Override
    public String toString() {
        return "ContractHistory{"
                + "id=" + id
                + ", contractId=" + contractId
                + ", action='" + action + '\''
                + ", reason='" + reason + '\''
                + ", createdAt=" + createdAt
                + '}';
    }
}
