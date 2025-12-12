package org.example.javaotellgtm.traces.contract;

import com.mercadolibre.wallet_sp_bill_intent.infrastructure.o11y.traces.annotation.SpanAttribute;
import com.mercadolibre.wallet_sp_bill_intent.infrastructure.o11y.traces.constants.AttributeName;
import com.mercadolibre.wallet_sp_bill_intent.infrastructure.o11y.traces.processor.SpanWrap;
import java.util.Map;

/**
 * Marks domain objects that can provide telemetry attributes for OpenTelemetry spans.
 *
 * <p>Domain objects implementing this interface can expose their business attributes in a
 * structured way for distributed tracing. The attributes are then added to the current span by
 * {@link SpanWrap}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * public record BillIntent(...) implements TelemetryEvent {
 *     @Override
 *     public Map<String, String> attributes() {
 *         return Map.of(
 *             AttributeName.INTENT_ID.getKey(), id,
 *             AttributeName.INTENT_SITE.getKey(), site,
 *             AttributeName.INTENT_FLOW.getKey(), flow.name()
 *         );
 *     }
 * }
 *
 * // In use case
 * SpanWrap.addAttributes(billIntent);
 * }</pre>
 *
 * @see SpanWrap#addAttributes(Map)
 * @see AttributeName
 */
public interface TelemetryEvent {

  /**
   * Returns a map of Observability span attributes for this domain object.
   *
   * <p>Keys should use {@link AttributeName} enum constants to ensure consistency. Null values will
   * be automatically filtered out by {@link SpanWrap} or {@link SpanAttribute}.
   *
   * @return map of attribute key-value pairs (null values are allowed and will be skipped)
   */
  Map<String, String> attributes();
}
