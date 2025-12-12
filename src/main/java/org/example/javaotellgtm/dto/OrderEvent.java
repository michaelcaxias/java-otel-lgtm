package org.example.javaotellgtm.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.javaotellgtm.model.OrderStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.example.javaotellgtm.traces.contract.TelemetryEvent;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable, TelemetryEvent {

    private String orderId;
    private String customerId;
    private String customerEmail;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private EventType eventType;
    private LocalDateTime timestamp;

    /**
     * Returns a map of Observability span attributes for this domain object.
     *
     * <p>Keys should use {@link AttributeName} enum constants to ensure consistency. Null values will
     * be automatically filtered out by {@link SpanWrap} or {@link SpanAttribute}.
     *
     * @return map of attribute key-value pairs (null values are allowed and will be skipped)
     */
    @Override
    public Map<String, String> attributes() {
        var attrs = new HashMap<String, String>();

        attrs.put("order.id", orderId);
        attrs.put("event.type", eventType.toString());
        attrs.put("customer.id", orderId);
        attrs.put("order.total_amount", totalAmount.toString());

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
