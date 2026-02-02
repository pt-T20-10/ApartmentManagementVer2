package model;

import java.math.BigDecimal;
import java.util.Date;

/**
 * ViewModel cho tab Cư Dân - Redesigned 1 row = 1 hợp đồng (1 căn hộ với Chủ
 * hộ) Bao gồm: Thông tin hợp đồng + Chủ hộ + Số lượng thành viên
 */
public class ContractHouseholdViewModel {

    // Contract info
    private Long contractId;
    private Date startDate;
    private Date endDate;
    private String contractStatus;

    // Apartment info
    private Long apartmentId;
    private String apartmentNumber;
    private String floorName;
    private Long buildingId;
    private String buildingName;

    // Resident (Chủ hộ) info
    private Long residentId;
    private String residentFullName;
    private String residentPhone;
    private String residentEmail;
    private String residentIdentityCard;
    private String residentGender;
    private Date residentDob;

    // Household member count
    private int householdMemberCount; // Số thành viên (không bao gồm chủ hộ)
    private int activeHouseholdMemberCount; // Số thành viên đang ở

    // Computed fields
    private int totalPeople; // Tổng số người (chủ hộ + thành viên)

    // Constructors
    public ContractHouseholdViewModel() {
    }

    // Getters and Setters
    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
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

    public String getContractStatus() {
        return contractStatus;
    }

    public void setContractStatus(String contractStatus) {
        this.contractStatus = contractStatus;
    }

    public Long getApartmentId() {
        return apartmentId;
    }

    public void setApartmentId(Long apartmentId) {
        this.apartmentId = apartmentId;
    }

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

    public Long getResidentId() {
        return residentId;
    }

    public void setResidentId(Long residentId) {
        this.residentId = residentId;
    }

    public String getResidentFullName() {
        return residentFullName;
    }

    public void setResidentFullName(String residentFullName) {
        this.residentFullName = residentFullName;
    }

    public String getResidentPhone() {
        return residentPhone;
    }

    public void setResidentPhone(String residentPhone) {
        this.residentPhone = residentPhone;
    }

    public String getResidentEmail() {
        return residentEmail;
    }

    public void setResidentEmail(String residentEmail) {
        this.residentEmail = residentEmail;
    }

    public String getResidentIdentityCard() {
        return residentIdentityCard;
    }

    public void setResidentIdentityCard(String residentIdentityCard) {
        this.residentIdentityCard = residentIdentityCard;
    }

    public String getResidentGender() {
        return residentGender;
    }

    public void setResidentGender(String residentGender) {
        this.residentGender = residentGender;
    }

    public Date getResidentDob() {
        return residentDob;
    }

    public void setResidentDob(Date residentDob) {
        this.residentDob = residentDob;
    }

    public int getHouseholdMemberCount() {
        return householdMemberCount;
    }

    public void setHouseholdMemberCount(int householdMemberCount) {
        this.householdMemberCount = householdMemberCount;
        this.totalPeople = 1 + householdMemberCount; // 1 (chủ hộ) + thành viên
    }

    public int getActiveHouseholdMemberCount() {
        return activeHouseholdMemberCount;
    }

    public void setActiveHouseholdMemberCount(int activeHouseholdMemberCount) {
        this.activeHouseholdMemberCount = activeHouseholdMemberCount;
    }

    public int getTotalPeople() {
        return totalPeople;
    }

    // Helper methods
    public String getResidencyStatus() {
        if ("ACTIVE".equalsIgnoreCase(contractStatus)) {
            return "Đang ở";
        }
        return "Đã chuyển đi";
    }

    public String getHouseholdMemberCountDisplay() {
        if (householdMemberCount == 0) {
            return "Không có";
        }
        return householdMemberCount + " người";
    }

    public String getTotalPeopleDisplay() {
        return totalPeople + " người";
    }
}
