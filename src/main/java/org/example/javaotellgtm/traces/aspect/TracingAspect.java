package org.example.javaotellgtm.traces.aspect;

import org.example.javaotellgtm.traces.annotation.SpanAttribute;
import org.example.javaotellgtm.traces.annotation.TraceSpan;
import org.example.javaotellgtm.traces.contract.TelemetryEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AspectJ aspect that handles {@link TraceSpan} annotations.
 *
 * <p>This aspect automatically creates OpenTelemetry spans for methods annotated with {@link
 * TraceSpan}, using the configured tracer with instrumentation scope "wallet-sp-bill-intent".
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Automatic span creation with configurable name and kind
 *   <li>Support for {@link SpanAttribute} to add method parameters as span attributes
 *   <li>Automatic exception recording and error status setting
 *   <li>Parent span propagation via Context
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @TraceSpan(value = "bill_intent.create.barcode", kind = SpanKind.INTERNAL)
 * public BillIntent createIntent(@SpanAttribute("user.id") String userId) {
 *     // This method will automatically create an INTERNAL span with user.id attribute
 *     return intentService.create(userId);
 * }
 * }</pre>
 *
 * @see TraceSpan
 * @see SpanAttribute
 */
@Aspect
@Component
public class TracingAspect {

  private static final Logger log = LoggerFactory.getLogger(TracingAspect.class);

  private final Tracer tracer;

  public TracingAspect(Tracer tracer) {
    this.tracer = tracer;
  }

  /**
   * Intercepts methods annotated with {@link TraceSpan} to create spans.
   *
   * @param pjp the proceeding join point
   * @param traceSpan the TraceSpan annotation
   * @return the result of the intercepted method
   * @throws Throwable if the intercepted method throws an exception
   */
  @Around("@annotation(traceSpan)")
  public Object trace(ProceedingJoinPoint pjp, TraceSpan traceSpan) throws Throwable {
    String spanName = traceSpan.value();
    if (spanName.isEmpty()) {
      var className = pjp.getSignature().getDeclaringType().getSimpleName();
      var classMethod = pjp.getSignature().getName();
      spanName = className + "." + classMethod;
    }

    Span span =
        tracer
            .spanBuilder(spanName)
            .setSpanKind(traceSpan.kind())
            .setParent(Context.current())
            .startSpan();

    addParameterAttributes(pjp, span);

    try (Scope scope = span.makeCurrent()) {
      Object result = pjp.proceed();
      span.setStatus(StatusCode.OK);
      return result;
    } catch (Throwable e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
    } finally {
      span.end();
    }
  }

  /**
   * Extracts method parameters annotated with {@link SpanAttribute} and adds them as span
   * attributes.
   *
   * @param pjp the proceeding join point
   * @param span the current span
   */
  private void addParameterAttributes(ProceedingJoinPoint pjp, Span span) {
    try {
      MethodSignature signature = (MethodSignature) pjp.getSignature();
      Method method = signature.getMethod();
      Parameter[] parameters = method.getParameters();
      Object[] args = pjp.getArgs();

      for (int i = 0; i < parameters.length; i++) {
        Parameter parameter = parameters[i];
        SpanAttribute spanAttribute = parameter.getAnnotation(SpanAttribute.class);

        if (spanAttribute != null && args[i] != null) {
          String attributeKey = spanAttribute.value();
          Object attributeValue = args[i];

          addAttribute(span, attributeKey, attributeValue);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to add parameter attributes to span", e);
    }
  }

  /**
   * Adds an attribute to the span, handling different value types.
   *
   * <p>Supports:
   *
   * <ul>
   *   <li>Primitive types and their wrappers
   *   <li>{@link TelemetryEvent} objects - extracts all attributes
   *   <li>Other objects - converted to String using toString()
   * </ul>
   *
   * @param span the span to add the attribute to
   * @param key the attribute key
   * @param value the attribute value
   */
  private void addAttribute(Span span, String key, Object value) {
    if (value == null) {
      return;
    }

    if (value instanceof TelemetryEvent telemetryEvent) {
      telemetryEvent
          .attributes()
          .forEach(
              (attrKey, attrValue) -> {
                if (attrKey != null && attrValue != null) {
                  span.setAttribute(attrKey, attrValue);
                }
              });
      return;
    }

    switch (value) {
      case String stringValue -> span.setAttribute(key, stringValue);
      case Long longValue -> span.setAttribute(key, longValue);
      case Integer intValue -> span.setAttribute(key, intValue.longValue());
      case Double doubleValue -> span.setAttribute(key, doubleValue);
      case Float floatValue -> span.setAttribute(key, floatValue.doubleValue());
      case Boolean booleanValue -> span.setAttribute(key, booleanValue);
      default -> span.setAttribute(key, value.toString());
    }
  }
}
