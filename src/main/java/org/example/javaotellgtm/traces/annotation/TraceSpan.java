package org.example.javaotellgtm.traces.annotation;

import com.mercadolibre.wallet_sp_bill_intent.infrastructure.o11y.traces.aspect.TracingAspect;
import io.opentelemetry.api.trace.SpanKind;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to create OpenTelemetry spans using AOP.
 *
 * <p>This annotation can be applied to methods to automatically create spans with the configured
 * tracer (instrumentation scope: "wallet-sp-bill-intent").
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @TraceSpan(value = "bill_intent.create.barcode", kind = SpanKind.INTERNAL)
 * public BillIntent createIntent(@SpanAttribute("user.id") String userId) {
 *     // method implementation
 * }
 * }</pre>
 *
 * @see SpanAttribute
 * @see TracingAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceSpan {

  /**
   * The name of the span. If empty, defaults to the method name.
   *
   * @return span name
   */
  String value() default "";

  /**
   * The kind of span to create.
   *
   * @return span kind
   */
  SpanKind kind() default SpanKind.INTERNAL;
}
