package com.newproject.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Riga di carrello inviata al checkout. Volutamente SENZA prezzo: l'unitPrice viene
 * risolto in modo autoritativo lato server (pricing-service /resolve, fallback catalog),
 * quindi qualunque prezzo arrivasse dal client verrebbe comunque ignorato.
 */
public class OrderLineRequest {
    @NotNull
    private Long productId;

    private String variantKey;
    private String variantDisplayName;
    private String sku;
    private String name;

    @NotNull
    @Positive
    private Integer quantity;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getVariantKey() {
        return variantKey;
    }

    public void setVariantKey(String variantKey) {
        this.variantKey = variantKey;
    }

    public String getVariantDisplayName() {
        return variantDisplayName;
    }

    public void setVariantDisplayName(String variantDisplayName) {
        this.variantDisplayName = variantDisplayName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
