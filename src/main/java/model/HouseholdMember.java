package model;

import java.util.Date;

/**
 * Household Member Entity Represents a member living in an apartment (not the
 * contract holder)
 */
public class HouseholdMember {

    private Long id;
    private Long contractId;
    private String fullName;
    private String relationship;  // Mối quan hệ: Vợ, Chồng, Con, Bố, Mẹ...
    private boolean isHead;       // 0 = Thành viên, 1 = Chủ hộ (ít dùng vì chủ hộ là resident)
    private String gender;
    private Date dob;
    private String identityCard;  // CMND/CCCD
    private String phone;
    private boolean isActive;     // 1 = Đang ở, 0 = Đã rời đi
    private Date createdAt;

    // For display purposes (JOIN data)
    private String apartmentNumber;
    private String floorName;
    private String buildingName;
    private String contractStatus;

    // Constructors
    public HouseholdMember() {
        this.isActive = true;
    }

    public HouseholdMember(Long id, Long contractId, String fullName, String relationship,
            boolean isHead, String gender, Date dob, String identityCard,
            String phone, boolean isActive, Date createdAt) {
        this.id = id;
        this.contractId = contractId;
        this.fullName = fullName;
        this.relationship = relationship;
        this.isHead = isHead;
        this.gender = gender;
        this.dob = dob;
        this.identityCard = identityCard;
        this.phone = phone;
        this.isActive = isActive;
        this.createdAt = createdAt;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public boolean isHead() {
        return isHead;
    }

    public void setHead(boolean head) {
        isHead = head;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public String getIdentityCard() {
        return identityCard;
    }

    public void setIdentityCard(String identityCard) {
        this.identityCard = identityCard;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    // Display helpers
    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public void setApartmentNumber(String apartmentNumber) {
        this.apartmentNumber = apartmentNumber;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getContractStatus() {
        return contractStatus;
    }

    public void setContractStatus(String contractStatus) {
        this.contractStatus = contractStatus;
    }

    @Override
    public String toString() {
        return "HouseholdMember{"
                + "id=" + id
                + ", fullName='" + fullName + '\''
                + ", relationship='" + relationship + '\''
                + ", isActive=" + isActive
                + '}';
    }
}
