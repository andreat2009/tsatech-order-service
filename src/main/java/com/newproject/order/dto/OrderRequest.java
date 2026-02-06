package com.newproject.order.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class OrderRequest {
    @NotNull
    private Long customerId;

    @NotNull
    private String currency;

    @NotNull
    private BigDecimal total;

    private String status;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
