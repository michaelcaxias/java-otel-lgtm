package org.example.javaotellgtm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.javaotellgtm.model.OrderStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {

    private String orderId;
    private String customerId;
    private String customerEmail;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private EventType eventType;
    private LocalDateTime timestamp;

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
