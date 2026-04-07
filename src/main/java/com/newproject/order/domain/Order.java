package com.newproject.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(length = 32, nullable = false)
    private String status;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal total;

    @Column(name = "discount_total", precision = 15, scale = 4)
    private BigDecimal discountTotal;

    @Column(name = "customer_group_code", length = 64)
    private String customerGroupCode;

    @Column(name = "applied_coupon_code", length = 64)
    private String appliedCouponCode;

    @Column(name = "applied_offer_codes", length = 512)
    private String appliedOfferCodes;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_first_name", length = 128)
    private String customerFirstName;

    @Column(name = "customer_last_name", length = 128)
    private String customerLastName;

    @Column(name = "customer_phone", length = 64)
    private String customerPhone;

    @Column(name = "customer_locale", length = 8)
    private String customerLocale;

    @Column(name = "order_comment", length = 2000)
    private String orderComment;

    @Column(name = "guest_checkout", nullable = false)
    private Boolean guestCheckout;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public BigDecimal getDiscountTotal() { return discountTotal; }
    public void setDiscountTotal(BigDecimal discountTotal) { this.discountTotal = discountTotal; }
    public String getCustomerGroupCode() { return customerGroupCode; }
    public void setCustomerGroupCode(String customerGroupCode) { this.customerGroupCode = customerGroupCode; }
    public String getAppliedCouponCode() { return appliedCouponCode; }
    public void setAppliedCouponCode(String appliedCouponCode) { this.appliedCouponCode = appliedCouponCode; }
    public String getAppliedOfferCodes() { return appliedOfferCodes; }
    public void setAppliedOfferCodes(String appliedOfferCodes) { this.appliedOfferCodes = appliedOfferCodes; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerFirstName() { return customerFirstName; }
    public void setCustomerFirstName(String customerFirstName) { this.customerFirstName = customerFirstName; }
    public String getCustomerLastName() { return customerLastName; }
    public void setCustomerLastName(String customerLastName) { this.customerLastName = customerLastName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerLocale() { return customerLocale; }
    public void setCustomerLocale(String customerLocale) { this.customerLocale = customerLocale; }
    public String getOrderComment() { return orderComment; }
    public void setOrderComment(String orderComment) { this.orderComment = orderComment; }
    public Boolean getGuestCheckout() { return guestCheckout; }
    public void setGuestCheckout(Boolean guestCheckout) { this.guestCheckout = guestCheckout; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
