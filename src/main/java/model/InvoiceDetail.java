package model;

import java.math.BigDecimal;

/**
 * Model class representing Invoice Detail
 */
public class InvoiceDetail {

    private Long id;
    private Long invoiceId;
    private String serviceName;
    private BigDecimal unitPrice;
    private Double quantity;
    private BigDecimal amount;

    // Constructors
    public InvoiceDetail() {
    }

    public InvoiceDetail(Long id, Long invoiceId, String serviceName, BigDecimal unitPrice,
            Double quantity, BigDecimal amount) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.serviceName = serviceName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.amount = amount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
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

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
