package org.example.javaotellgtm.traces.annotation;

import com.mercadolibre.wallet_sp_bill_intent.infrastructure.o11y.traces.aspect.TracingAspect;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark method parameters that should be added as span attributes.
 *
 * <p>When used with {@link TraceSpan}, this annotation will automatically add the parameter value
 * as an attribute to the created span.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @TraceSpan(value = "bill_intent.create.barcode", kind = SpanKind.INTERNAL)
 * public BillIntent createIntent(@SpanAttribute("user.id") String userId,
 *                                 @SpanAttribute("bill_intent.site") String siteId) {
 *     // method implementation
 * }
 * }</pre>
 *
 * <p>Supported parameter types:
 *
 * <ul>
 *   <li>String - added directly as attribute
 *   <li>Number types (Integer, Long, Double, etc.) - converted to long or double
 *   <li>Boolean - added as boolean attribute
 *   <li>Other types - converted to String using toString()
 * </ul>
 *
 * <p>Null values are safely ignored and will not be added as attributes.
 *
 * @see TraceSpan
 * @see TracingAspect
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface SpanAttribute {

  /**
   * The attribute key to use in the span.
   *
   * @return attribute key name
   */
  String value() default "";
}
