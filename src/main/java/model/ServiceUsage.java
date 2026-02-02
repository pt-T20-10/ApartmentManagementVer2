package model;

/**
 * ServiceUsage Entity Represents service usage for a contract in a specific
 * month
 */
public class ServiceUsage {

    private Long id;
    private Long contractId;
    private Long serviceId;
    private int month;
    private int year;
    private Double oldIndex;
    private Double newIndex;
    private Double actualUsage;

    // For display purposes
    private String serviceName;
    private String apartmentNumber;

    // Constructors
    public ServiceUsage() {
    }

    public ServiceUsage(Long id, Long contractId, Long serviceId, int month, int year,
            Double oldIndex, Double newIndex, Double actualUsage) {
        this.id = id;
        this.contractId = contractId;
        this.serviceId = serviceId;
        this.month = month;
        this.year = year;
        this.oldIndex = oldIndex;
        this.newIndex = newIndex;
        this.actualUsage = actualUsage;
    }

    public ServiceUsage(Long contractId, Long serviceId, int month, int year, Double actualUsage) {
        this.contractId = contractId;
        this.serviceId = serviceId;
        this.month = month;
        this.year = year;
        this.actualUsage = actualUsage;
        this.oldIndex = 0.0;
        this.newIndex = 0.0;
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

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
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

    public Double getOldIndex() {
        return oldIndex;
    }

    public void setOldIndex(Double oldIndex) {
        this.oldIndex = oldIndex;
    }

    public Double getNewIndex() {
        return newIndex;
    }

    public void setNewIndex(Double newIndex) {
        this.newIndex = newIndex;
    }

    public Double getActualUsage() {
        return actualUsage;
    }

    public void setActualUsage(Double actualUsage) {
        this.actualUsage = actualUsage;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public void setApartmentNumber(String apartmentNumber) {
        this.apartmentNumber = apartmentNumber;
    }

    @Override
    public String toString() {
        return "ServiceUsage{"
                + "id=" + id
                + ", contractId=" + contractId
                + ", serviceId=" + serviceId
                + ", month=" + month
                + ", year=" + year
                + ", actualUsage=" + actualUsage
                + '}';
    }
}
