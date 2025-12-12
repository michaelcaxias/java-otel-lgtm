package org.example.javaotellgtm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.javaotellgtm.traces.constants.AttributeName;
import org.example.javaotellgtm.traces.contract.TelemetryEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order implements TelemetryEvent {

    @Id
    private String id;

    private String customerId;
    private String customerName;
    private String customerEmail; // Note: This is stored but NOT exposed in telemetry (PII)

    private List<OrderItem> items;

    private BigDecimal totalAmount;

    private OrderStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String shippingAddress;
    private String paymentMethod;

    /**
     * Returns telemetry attributes for this Order.
     * Note: Email is NOT included as it's PII (Personally Identifiable Information).
     *
     * @return map of attribute key-value pairs for observability
     */
    @Override
    public Map<String, String> attributes() {
        Map<String, String> attrs = new HashMap<>();

        if (id != null) {
            attrs.put(AttributeName.ORDER_ID.getKey(), id);
        }
        if (customerId != null) {
            attrs.put(AttributeName.CUSTOMER_ID.getKey(), customerId);
        }
        if (customerName != null) {
            attrs.put(AttributeName.CUSTOMER_NAME.getKey(), customerName);
        }
        if (status != null) {
            attrs.put(AttributeName.ORDER_STATUS.getKey(), status.name());
        }
        if (totalAmount != null) {
            attrs.put(AttributeName.ORDER_TOTAL_AMOUNT.getKey(), totalAmount.toString());
        }
        if (items != null) {
            attrs.put(AttributeName.ORDER_ITEMS_COUNT.getKey(), String.valueOf(items.size()));
        }
        if (paymentMethod != null) {
            attrs.put(AttributeName.ORDER_PAYMENT_METHOD.getKey(), paymentMethod);
        }

        return attrs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
