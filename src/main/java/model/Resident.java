package model;

import java.util.Date;

/**
 * Model class representing a Resident
 */
public class Resident {

    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String identityCard;
    private String gender;
    private Date dob;
    private String hometown;
    private boolean isDeleted;

    // Constructors
    public Resident() {
    }

    public Resident(Long id, String fullName, String phone, String email, String identityCard,
            String gender, Date dob, String hometown, boolean isDeleted) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.identityCard = identityCard;
        this.gender = gender;
        this.dob = dob;
        this.hometown = hometown;
        this.isDeleted = isDeleted;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIdentityCard() {
        return identityCard;
    }

    public void setIdentityCard(String identityCard) {
        this.identityCard = identityCard;
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

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    // ===== ALIAS METHODS FOR PANEL COMPATIBILITY =====
    /**
     * Alias for getIdentityCard() - for panel compatibility ID Card can be CMND
     * or CCCD
     */
    public String getIdCard() {
        return this.identityCard;
    }

    /**
     * Alias for setIdentityCard() - for panel compatibility
     */
    public void setIdCard(String idCard) {
        this.identityCard = idCard;
    }

    @Override
    public String toString() {
        return fullName;
    }
}
