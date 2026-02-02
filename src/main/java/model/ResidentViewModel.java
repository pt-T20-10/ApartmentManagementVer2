package model;

import java.util.Date;

/**
 * Resident View Model UNION data từ 2 nguồn: 1. Chủ hộ (residents + contracts)
 * 2. Thành viên (household_members + contracts)
 *
 * Dùng để hiển thị trong Tab Cư Dân
 */
public class ResidentViewModel {

    // === Thông tin chung ===
    private Long id;                    // ID của resident hoặc household_member
    private String sourceType;          // "CHU_HO" hoặc "THANH_VIEN"
    private String fullName;
    private String gender;
    private Date dob;
    private String identityCard;
    private String phone;

    // === Thông tin vai trò ===
    private String role;                // "Chủ hộ" hoặc "Thành viên"
    private String relationship;        // Mối quan hệ (chỉ có với thành viên)

    // === Thông tin căn hộ ===
    private Long contractId;
    private Long apartmentId;
    private String apartmentNumber;
    private String floorName;
    private String buildingName;
    private Long buildingId;            // Để filter

    // === Trạng thái cư trú ===
    private String residencyStatus;     // "Đang ở" hoặc "Đã chuyển đi"
    private String contractStatus;      // ACTIVE, EXPIRED, TERMINATED

    // === Thông tin bổ sung ===
    private String email;               // Chỉ có chủ hộ
    private String hometown;            // Chỉ có chủ hộ

    // Constructors
    public ResidentViewModel() {
    }

    /**
     * Constructor từ Resident (Chủ hộ)
     */
    public static ResidentViewModel fromResident(Resident resident, Contract contract,
            Apartment apartment, Floor floor, Building building) {
        ResidentViewModel vm = new ResidentViewModel();

        // Source
        vm.setId(resident.getId());
        vm.setSourceType("CHU_HO");

        // Basic info
        vm.setFullName(resident.getFullName());
        vm.setGender(resident.getGender());
        vm.setDob(resident.getDob());
        vm.setIdentityCard(resident.getIdentityCard());
        vm.setPhone(resident.getPhone());
        vm.setEmail(resident.getEmail());
        vm.setHometown(resident.getHometown());

        // Role
        vm.setRole("Chủ hộ");
        vm.setRelationship("Chủ hộ");

        // Contract & Apartment
        if (contract != null) {
            vm.setContractId(contract.getId());
            vm.setContractStatus(contract.getStatus());
            vm.setResidencyStatus("ACTIVE".equals(contract.getStatus()) ? "Đang ở" : "Đã chuyển đi");
        }

        if (apartment != null) {
            vm.setApartmentId(apartment.getId());
            vm.setApartmentNumber(apartment.getRoomNumber());
        }

        if (floor != null) {
            vm.setFloorName(floor.getName());
        }

        if (building != null) {
            vm.setBuildingId(building.getId());
            vm.setBuildingName(building.getName());
        }

        return vm;
    }

    /**
     * Constructor từ HouseholdMember (Thành viên)
     */
    public static ResidentViewModel fromHouseholdMember(HouseholdMember member, Contract contract,
            Apartment apartment, Floor floor, Building building) {
        ResidentViewModel vm = new ResidentViewModel();

        // Source
        vm.setId(member.getId());
        vm.setSourceType("THANH_VIEN");

        // Basic info
        vm.setFullName(member.getFullName());
        vm.setGender(member.getGender());
        vm.setDob(member.getDob());
        vm.setIdentityCard(member.getIdentityCard());
        vm.setPhone(member.getPhone());

        // Role
        vm.setRole("Thành viên");
        vm.setRelationship(member.getRelationship());

        // Contract & Apartment
        if (contract != null) {
            vm.setContractId(contract.getId());
            vm.setContractStatus(contract.getStatus());

            // Trạng thái cư trú = Contract ACTIVE && Member is_active
            boolean isLiving = "ACTIVE".equals(contract.getStatus()) && member.isActive();
            vm.setResidencyStatus(isLiving ? "Đang ở" : "Đã chuyển đi");
        }

        if (apartment != null) {
            vm.setApartmentId(apartment.getId());
            vm.setApartmentNumber(apartment.getRoomNumber());
        }

        if (floor != null) {
            vm.setFloorName(floor.getName());
        }

        if (building != null) {
            vm.setBuildingId(building.getId());
            vm.setBuildingName(building.getName());
        }

        return vm;
    }

    // === Getters and Setters ===
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
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

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getResidencyStatus() {
        return residencyStatus;
    }

    public void setResidencyStatus(String residencyStatus) {
        this.residencyStatus = residencyStatus;
    }

    public String getContractStatus() {
        return contractStatus;
    }

    public void setContractStatus(String contractStatus) {
        this.contractStatus = contractStatus;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    /**
     * Check if this resident is currently living (Đang ở)
     */
    public boolean isCurrentlyLiving() {
        return "Đang ở".equals(residencyStatus);
    }

    /**
     * Check if this is a head of household (Chủ hộ)
     */
    public boolean isHead() {
        return "CHU_HO".equals(sourceType);
    }

    @Override
    public String toString() {
        return "ResidentViewModel{"
                + "fullName='" + fullName + '\''
                + ", role='" + role + '\''
                + ", apartment='" + apartmentNumber + '\''
                + ", status='" + residencyStatus + '\''
                + '}';
    }
}
