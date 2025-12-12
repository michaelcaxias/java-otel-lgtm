package org.example.javaotellgtm.traces.processor;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.javaotellgtm.traces.contract.TelemetryEvent;

/**
 * Utility class for adding telemetry attributes and events to the current OpenTelemetry span.
 *
 * <p>This class centralizes the logic of enriching spans with domain-specific attributes and
 * events, handling null values and span validation automatically.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // In adapter
 * Faas faas = repository.findByUserId(userId);
 * SpanWrap.addAttributes(faas.attributes());
 *
 * // Adding events
 * SpanWrap.addEvent("validation.completed", Map.of("status", "success"));
 * }</pre>
 *
 * @see TelemetryEvent
 * @see AttributeName
 */
@Slf4j
public final class SpanWrap {

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
   * Faas faas = repository.findByUserId(userId);
   * SpanWrap.addAttributes(faas); // Direct usage with TelemetryEvent
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
   *   <li>Filters out null values automatically
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
      Span currentSpan = getCurrentValidSpan();

      if (currentSpan == null) {
        return;
      }

      attributes.forEach(
          (key, value) -> {
            if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
              currentSpan.setAttribute(key, value);
            }
          });

    } catch (Exception e) {
      log.error("Error adding attributes to span", e);
    }
  }

  /**
   * Adds an event to the current OpenTelemetry span with attributes from a TelemetryEvent.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>Validates that a span context is active and valid
   *   <li>Converts TelemetryEvent attributes to OpenTelemetry format
   *   <li>Filters out blank keys and values automatically
   *   <li>Adds the event to the current span with the provided attributes
   * </ul>
   *
   * <p>If no valid span context exists, the operation is silently skipped with a warning log.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * TelemetryEvent event = () -> Map.of("status", "success", "amount", "100.00");
   * SpanWrap.addEvent("payment.completed", event);
   * }</pre>
   *
   * @param eventName the name of the event to add to the span
   * @param event the telemetry event containing attributes. If null, no event is added.
   * @see #addEvent(String, Map)
   */
  public static void addEvent(String eventName, TelemetryEvent event) {
    if (event == null) {
      return;
    }
    addEvent(eventName, event.attributes());
  }

  /**
   * Adds an event to the current OpenTelemetry span with custom attributes.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>Validates that a span context is active and valid
   *   <li>Filters out blank keys and values automatically
   *   <li>Adds the event to the current span with the provided attributes
   * </ul>
   *
   * <p>If no valid span context exists, the operation is silently skipped with a warning log.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * Map<String, String> attributes = Map.of(
   *     "validation.type", "barcode",
   *     "validation.status", "success"
   * );
   * SpanWrap.addEvent("validation.completed", attributes);
   * }</pre>
   *
   * @param eventName the name of the event to add to the span. If blank, no event is added.
   * @param attributes map of attribute key-value pairs to add to the event. Null or blank keys and
   *     values will be skipped.
   */
  public static void addEvent(String eventName, Map<String, String> attributes) {
    if (StringUtils.isBlank(eventName)) {
      log.warn("Event name cannot be blank");
      return;
    }

    try {
      Span currentSpan = getCurrentValidSpan();

      if (currentSpan == null) {
        return;
      }

      if (attributes == null || attributes.isEmpty()) {
        currentSpan.addEvent(eventName);
        return;
      }

      AttributesBuilder attributesBuilder = Attributes.builder();
      attributes.forEach(
          (key, value) -> {
            if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
              attributesBuilder.put(key, value);
            }
          });

      currentSpan.addEvent(eventName, attributesBuilder.build());

    } catch (Exception e) {
      log.error("Error adding event to span", e);
    }
  }

  /**
   * Gets the trace ID from the current OpenTelemetry span.
   *
   * <p>This method safely retrieves the trace ID from the current span context. If no valid span
   * context exists, it returns null and logs a warning.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * String traceId = SpanWrap.getTraceId();
   * if (traceId != null) {
   *     log.info("Processing request with trace ID: {}", traceId);
   * }
   * }</pre>
   *
   * @return the trace ID of the current span, or null if no valid span context exists
   */
  public static String getTraceId() {
    try {
      Span currentSpan = getCurrentValidSpan();

      if (currentSpan == null) {
        return null;
      }

      return currentSpan.getSpanContext().getTraceId();

    } catch (Exception e) {
      log.error("Error getting trace ID from span", e);
      return null;
    }
  }

  /**
   * Gets the current valid OpenTelemetry span.
   *
   * <p>This method encapsulates the logic of obtaining and validating the current span:
   *
   * <ul>
   *   <li>Obtains the current span via {@link Span#current()}
   *   <li>Validates that the span is not null
   *   <li>Validates that the span context is valid
   * </ul>
   *
   * <p>If the span is invalid or null, a warning is logged and null is returned.
   *
   * @return the current valid span, or null if no valid span context exists
   */
  private static Span getCurrentValidSpan() {
    Span currentSpan = Span.current();

    if (currentSpan == null || !currentSpan.getSpanContext().isValid()) {
      log.warn("No valid span context available");
      return null;
    }

    return currentSpan;
  }
}
