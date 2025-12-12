package org.example.javaotellgtm.traces.constants;

/**
 * Centralized span attribute names for OpenTelemetry tracing.
 *
 * <p>This enum provides consistent attribute names across the application, following OpenTelemetry
 * semantic conventions for better observability and standardization.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * SpanWrap.addAttributes(Map.of(
 *     AttributeName.USER_ID.getKey(), userId,
 *     AttributeName.INTENT_ID.getKey(), intentId
 * ));
 * }</pre>
 */
public enum AttributeName {
  // ============================================================
  // User Attributes
  // Following OpenTelemetry semantic conventions
  // ============================================================

  /** User identifier (internal ID, NOT PII). */
  USER_ID("user.id"),

  /** Customer identifier (internal ID, NOT PII). */
  CUSTOMER_ID("customer.id"),

  /** Customer name (for display purposes, not PII). */
  CUSTOMER_NAME("customer.name"),

  // ============================================================
  // Order Attributes
  // ============================================================

  /** Order identifier. */
  ORDER_ID("order.id"),

  /** Order status (e.g., PENDING, COMPLETED, CANCELLED). */
  ORDER_STATUS("order.status"),

  /** Order total amount. */
  ORDER_TOTAL_AMOUNT("order.total_amount"),

  /** Number of items in the order. */
  ORDER_ITEMS_COUNT("order.items_count"),

  /** Payment method. */
  ORDER_PAYMENT_METHOD("order.payment_method"),

  /** Number of orders returned in list operations. */
  ORDERS_COUNT("orders.count"),

  // ============================================================
  // Order Status Update Attributes
  // ============================================================

  /** Previous order status. */
  ORDER_STATUS_OLD("order.status.old"),

  /** New order status. */
  ORDER_STATUS_NEW("order.status.new"),

  // ============================================================
  // External API Attributes
  // ============================================================

  /** External post identifier. */
  POST_ID("post.id"),

  /** Post title. */
  POST_TITLE("post.title"),

  /** Post user identifier. */
  POST_USER_ID("post.user_id"),

  /** External user name. */
  EXTERNAL_USER_NAME("external.user.name"),

  /** Number of posts returned. */
  POSTS_COUNT("posts.count"),

  /** Number of users returned. */
  USERS_COUNT("users.count"),

  // ============================================================
  // Event Attributes
  // ============================================================

  /** Event type. */
  EVENT_TYPE("event.type"),

  // ============================================================
  // Error Attributes
  // ============================================================

  /** Error indicator. */
  ERROR("error"),

  /** Error message. */
  ERROR_MESSAGE("error.message");

  private final String key;

  AttributeName(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
