package org.example.javaotellgtm.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.javaotellgtm.model.OrderStatus;
import org.example.javaotellgtm.traces.constants.AttributeName;
import org.example.javaotellgtm.traces.contract.TelemetryEvent;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable, TelemetryEvent {

    private String orderId;
    private String customerId;
    private String customerEmail; // Note: NOT exposed in telemetry (PII)
    private BigDecimal totalAmount;
    private OrderStatus status;
    private EventType eventType;
    private LocalDateTime timestamp;

    /**
     * Returns a map of Observability span attributes for this domain object.
     * Note: Email is NOT included as it's PII (Personally Identifiable Information).
     *
     * @return map of attribute key-value pairs (null values are allowed and will be skipped)
     */
    @Override
    public Map<String, String> attributes() {
        Map<String, String> attrs = new HashMap<>();

        attrs.put(AttributeName.ORDER_ID.getKey(), orderId);
        attrs.put(AttributeName.CUSTOMER_ID.getKey(), customerId);
        attrs.put(AttributeName.EVENT_TYPE.getKey(), eventType.toString());
        attrs.put(AttributeName.ORDER_TOTAL_AMOUNT.getKey(), totalAmount.toString());
        attrs.put(AttributeName.ORDER_STATUS.getKey(), status.name());

        return attrs;
    }

    public enum EventType {
        ORDER_CREATED,
        PAYMENT_PROCESSING,
        PAYMENT_CONFIRMED,
        ORDER_PREPARING,
        ORDER_SHIPPED,
        ORDER_DELIVERED,
        ORDER_CANCELLED
    }
}
