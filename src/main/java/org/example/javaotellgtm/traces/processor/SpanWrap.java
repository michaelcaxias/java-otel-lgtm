package org.example.javaotellgtm.traces.processor;

import com.mercadolibre.wallet_sp_bill_intent.infrastructure.o11y.traces.constants.AttributeName;
import com.mercadolibre.wallet_sp_bill_intent.infrastructure.o11y.traces.contract.TelemetryEvent;
import io.opentelemetry.api.trace.Span;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for adding telemetry attributes to the current OpenTelemetry span.
 *
 * <p>This class centralizes the logic of enriching spans with domain-specific attributes, handling
 * null values and span validation automatically.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // In use case
 * BillIntent intent = repository.findById(intentId);
 * SpanWrap.addAttributes(intent.attributes());
 * }</pre>
 *
 * @see TelemetryEvent
 * @see AttributeName
 */
public final class SpanWrap {

  private static final Logger log = LoggerFactory.getLogger(SpanWrap.class);

  private SpanWrap() {
    throw new UnsupportedOperationException(
        "SpanWrap is a utility class and cannot be instantiated");
  }

  /**
   * Adds telemetry attributes from a domain object to the current OpenTelemetry span.
   *
   * <p>This is a convenience method that extracts attributes from a {@link TelemetryEvent} and
   * delegates to {@link #addAttributes(Map)}.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * BillIntent intent = repository.findById(intentId);
   * SpanWrap.addAttributes(intent); // Direct usage with TelemetryEvent
   * }</pre>
   *
   * @param event the telemetry event containing span attributes. If null, the operation is skipped.
   * @see #addAttributes(Map)
   */
  public static void addAttributes(TelemetryEvent event) {
    if (event == null) {
      return;
    }
    addAttributes(event.attributes());
  }

  /**
   * Adds the provided attributes to the current OpenTelemetry span.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>Validates that a span context is active and valid
   *   <li>Filters out null or blank values automatically
   *   <li>Sets each attribute on the current span
   * </ul>
   *
   * <p>If no valid span context exists, the operation is silently skipped with a warning log.
   *
   * @param attributes map of attribute key-value pairs to add to the span. Null keys or values will
   *     be skipped.
   */
  public static void addAttributes(Map<String, String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return;
    }

    try {
      Span currentSpan = Span.current();

      if (currentSpan == null || !currentSpan.getSpanContext().isValid()) {
        log.warn("No valid span context available to add attributes");
        return;
      }

      attributes.forEach(
          (key, value) -> {
            if (isNotBlank(key) && isNotBlank(value)) {
              currentSpan.setAttribute(key, value);
            }
          });

    } catch (Exception e) {
      log.error("Error adding attributes to span", e);
    }
  }

  private static boolean isNotBlank(String str) {
    return str != null && !str.trim().isEmpty();
  }
}
