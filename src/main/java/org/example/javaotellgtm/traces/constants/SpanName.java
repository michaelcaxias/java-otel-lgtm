package org.example.javaotellgtm.traces.constants;

/**
 * Centralized span names for OpenTelemetry tracing.
 *
 * <p>This class provides consistent and descriptive span names across the application, following
 * OpenTelemetry semantic conventions for better observability.
 *
 * <p>Naming convention: namespace.operation.detail (use dots and snake_case)
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @TraceSpan(SpanName.INTENT_CREATE_BARCODE)
 * public BillIntent createBarcodeIntent(String userId, BarcodeRequest request) {
 *     // ...
 * }
 * }</pre>
 */
public final class SpanName {
  // ============================================================
  // Order Operations
  // Following OpenTelemetry semantic conventions: namespace.operation.detail
  // ============================================================

  public static final String ORDER_CREATE = "order.create";
  public static final String ORDER_FETCH = "order.fetch";
  public static final String ORDER_LIST_ALL = "order.list.all";
  public static final String ORDER_LIST_BY_CUSTOMER = "order.list.by_customer";
  public static final String ORDER_UPDATE_STATUS = "order.update.status";
  public static final String ORDER_CANCEL = "order.cancel";

  // ============================================================
  // External API Operations
  // ============================================================

  public static final String EXTERNAL_API_GET_POST_WITH_AUTHOR = "external_api.get.post_with_author";
  public static final String EXTERNAL_API_GET_USER_POSTS = "external_api.get.user_posts";
  public static final String EXTERNAL_API_LIST_POSTS = "external_api.list.posts";
  public static final String EXTERNAL_API_LIST_USERS = "external_api.list.users";

  private SpanName() {
    throw new UnsupportedOperationException("SpanName is a utility class and cannot be instantiated");
  }
}
