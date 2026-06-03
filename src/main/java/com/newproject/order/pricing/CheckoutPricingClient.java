package com.newproject.order.pricing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.newproject.order.dto.OrderLineRequest;
import com.newproject.order.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client server-to-server usato dal checkout autoritativo di order-service.
 * Risolve i prezzi unitari (pricing /resolve con fallback catalog), il costo di spedizione
 * (shipping-service) e lo sconto/total (coupon /quote). Fail-closed: se una dipendenza non
 * risponde l'eccezione si propaga e il checkout fallisce, piuttosto che fidarsi del client.
 */
@Component
public class CheckoutPricingClient {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutPricingClient.class);

    private final RestClient pricing;
    private final RestClient coupon;
    private final RestClient catalog;
    private final RestClient shipping;

    public CheckoutPricingClient(
        @Value("${ecommerce.services.pricing-base-url}") String pricingBaseUrl,
        @Value("${ecommerce.services.coupon-base-url}") String couponBaseUrl,
        @Value("${ecommerce.services.catalog-base-url}") String catalogBaseUrl,
        @Value("${ecommerce.services.shipping-base-url}") String shippingBaseUrl
    ) {
        this.pricing = build(pricingBaseUrl);
        this.coupon = build(couponBaseUrl);
        this.catalog = build(catalogBaseUrl);
        this.shipping = build(shippingBaseUrl);
    }

    private RestClient build(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /**
     * Mappa autoritativa prezzo unitario per (productId|variantKey). Per le righe non coperte
     * da una regola pricing usa il prezzo base da catalog (variante se presente). Se nessuna
     * delle due fonti ha un prezzo solleva 400.
     */
    public Map<String, BigDecimal> resolveUnitPrices(List<OrderLineRequest> items, String currency, String customerGroupCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("currency", currency);
        body.put("customerGroupCode", customerGroupCode);
        List<Map<String, Object>> lines = new ArrayList<>();
        for (OrderLineRequest item : items) {
            Map<String, Object> line = new HashMap<>();
            line.put("productId", item.getProductId());
            line.put("variantKey", normalizeVariantKey(item.getVariantKey()));
            line.put("quantity", item.getQuantity());
            lines.add(line);
        }
        body.put("items", lines);

        ResolveResponse response;
        try {
            response = pricing.post()
                .uri("/api/pricing/resolve")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ResolveResponse.class);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("pricing-service /resolve failed: " + ex.getStatusCode(), ex);
        }

        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        if (response != null && response.items != null) {
            for (ResolveItem item : response.items) {
                if (item.amount != null) {
                    prices.put(key(item.productId, normalizeVariantKey(item.variantKey)), item.amount);
                }
            }
        }

        for (OrderLineRequest item : items) {
            String variantKey = normalizeVariantKey(item.getVariantKey());
            String k = key(item.getProductId(), variantKey);
            if (!prices.containsKey(k)) {
                BigDecimal base = catalogPrice(item.getProductId(), variantKey);
                if (base == null) {
                    throw new BadRequestException("No price available for product " + item.getProductId()
                        + (variantKey != null ? " [" + variantKey + "]" : ""));
                }
                prices.put(k, base);
            }
        }
        return prices;
    }

    private BigDecimal catalogPrice(Long productId, String variantKey) {
        CatalogProduct product;
        try {
            product = catalog.get()
                .uri("/api/catalog/products/{id}", productId)
                .retrieve()
                .body(CatalogProduct.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            throw new IllegalStateException("catalog-service product lookup failed: " + ex.getStatusCode(), ex);
        }
        if (product == null) {
            return null;
        }
        if (variantKey != null && product.variants != null) {
            for (CatalogVariant variant : product.variants) {
                if (variantKey.equals(variant.variantKey) && variant.priceOverride != null) {
                    return variant.priceOverride;
                }
            }
        }
        return product.price;
    }

    /** Risolve il metodo di spedizione (default = primo metodo abilitato se il codice è assente). */
    public ResolvedShipping resolveShipping(String methodCode) {
        ShippingMethodView[] methods;
        try {
            methods = shipping.get()
                .uri("/api/shipping-methods?enabledOnly=true")
                .retrieve()
                .body(ShippingMethodView[].class);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("shipping-service lookup failed: " + ex.getStatusCode(), ex);
        }
        if (methods == null || methods.length == 0) {
            return new ResolvedShipping(normalizeCode(methodCode), BigDecimal.ZERO);
        }
        if (methodCode != null && !methodCode.isBlank()) {
            for (ShippingMethodView method : methods) {
                if (methodCode.equalsIgnoreCase(method.code)) {
                    return new ResolvedShipping(method.code, method.cost != null ? method.cost : BigDecimal.ZERO);
                }
            }
            throw new BadRequestException("Invalid shipping method: " + methodCode);
        }
        ShippingMethodView first = methods[0];
        return new ResolvedShipping(first.code, first.cost != null ? first.cost : BigDecimal.ZERO);
    }

    /** Sconto/total autoritativi (coupon + offerte automatiche). */
    public QuoteResult quote(BigDecimal subtotal, BigDecimal shipping, String couponCode, String customerGroupCode, int itemCount) {
        Map<String, Object> body = new HashMap<>();
        body.put("subtotal", subtotal);
        body.put("shipping", shipping);
        body.put("couponCode", couponCode);
        body.put("customerGroupCode", customerGroupCode);
        body.put("itemCount", itemCount);

        QuoteResponse response;
        try {
            response = coupon.post()
                .uri("/api/pricing/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(QuoteResponse.class);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException("coupon-service /quote failed: " + ex.getStatusCode(), ex);
        }

        BigDecimal safeShipping = shipping != null ? shipping : BigDecimal.ZERO;
        if (response == null) {
            return new QuoteResult(BigDecimal.ZERO, subtotal.add(safeShipping), null, null);
        }
        BigDecimal discount = response.discount != null ? response.discount : BigDecimal.ZERO;
        BigDecimal total = response.total != null ? response.total : subtotal.add(safeShipping).subtract(discount);
        return new QuoteResult(discount, total, response.appliedCoupon, joinOfferCodes(response.appliedOffers));
    }

    private String joinOfferCodes(List<AppliedOffer> offers) {
        if (offers == null || offers.isEmpty()) {
            return null;
        }
        List<String> codes = new ArrayList<>();
        for (AppliedOffer offer : offers) {
            if (offer != null && offer.code != null && !offer.code.isBlank()) {
                codes.add(offer.code.trim());
            }
        }
        return codes.isEmpty() ? null : String.join(",", codes);
    }

    private String key(Long productId, String variantKey) {
        return productId + "|" + (variantKey == null ? "" : variantKey);
    }

    private String normalizeVariantKey(String variantKey) {
        if (variantKey == null) {
            return null;
        }
        String trimmed = variantKey.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ResolvedShipping(String code, BigDecimal cost) {}

    public record QuoteResult(BigDecimal discount, BigDecimal total, String appliedCoupon, String appliedOfferCodes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ResolveResponse {
        public List<ResolveItem> items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ResolveItem {
        public Long productId;
        public String variantKey;
        public BigDecimal amount;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CatalogProduct {
        public BigDecimal price;
        public List<CatalogVariant> variants;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CatalogVariant {
        public String variantKey;
        public BigDecimal priceOverride;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ShippingMethodView {
        public String code;
        public BigDecimal cost;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class QuoteResponse {
        public BigDecimal discount;
        public BigDecimal total;
        public String appliedCoupon;
        public List<AppliedOffer> appliedOffers;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AppliedOffer {
        public String code;
    }
}
