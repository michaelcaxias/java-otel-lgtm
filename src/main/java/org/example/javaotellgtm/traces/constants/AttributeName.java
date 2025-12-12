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
  // Bill Intent Attributes
  // ============================================================

  /** Bill intent identifier. */
  INTENT_ID("bill_intent.id"),

  /** Bill intent flow (barcode, debt, product). */
  INTENT_FLOW("bill_intent.flow"),

  /** Bill intent provider. */
  INTENT_PROVIDER("bill_intent.provider"),
  INTENT_PROVIDER_ID("bill_intent.provider.id"),

  /** Submitted parameter identifier. */
  PARAMETER_ID("bill_intent.parameter.id"),

  /** Submitted parameter type (e.g., reference, amount). */
  PARAMETER_TYPE("bill_intent.parameter.type"),

  /** Utility Status (e.g., approved, created) . */
  UTILITY_STATUS("utility.status"),

  /** Utility identifier. */
  UTILITY_ID("utility.id");

  private final String key;

  AttributeName(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
