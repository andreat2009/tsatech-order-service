package com.newproject.order.dto;

import java.math.BigDecimal;

/**
 * Vista minimale autoritativa di un ordine, usata in chiamate server-to-server (es. payment-service
 * che valida amount == total). Non espone dati cliente sensibili.
 */
public class OrderSummaryResponse {
    private Long id;
    private BigDecimal total;
    private String currency;
    private String status;

    public OrderSummaryResponse() {
    }

    public OrderSummaryResponse(Long id, BigDecimal total, String currency, String status) {
        this.id = id;
        this.total = total;
        this.currency = currency;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
