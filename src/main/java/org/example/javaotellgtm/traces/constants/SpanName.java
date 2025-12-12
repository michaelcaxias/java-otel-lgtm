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
  // Bill Intent Operations
  // Following OpenTelemetry semantic conventions: namespace.operation.detail
  // ============================================================

  public static final String INTENT_CREATE_BARCODE = "bill_intent.create.barcode";
  public static final String INTENT_CREATE_DEBT = "bill_intent.create.debt";
  public static final String INTENT_CREATE_PRODUCT = "bill_intent.create.product";
  public static final String INTENT_PATCH_PARAMETER = "bill_intent.patch.parameter";

  public static final String INTENT_KVS_SAVE = "bill_intent.save.kvs";
  public static final String INTENT_KVS_GET = "bill_intent.get.kvs";

  private SpanName() {
    throw new UnsupportedOperationException("SpanName is a utility class and cannot be instantiated");
  }
}
