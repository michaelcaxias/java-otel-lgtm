package org.example.javaotellgtm.aop;

import io.opentelemetry.api.trace.SpanKind;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação customizada para criar spans automaticamente usando Tracer.
 * Similar ao @WithSpan do OpenTelemetry, mas com nossa própria implementação AOP.
 *
 * <p>Exemplo de uso:
 * <pre>
 * {@code
 * @Traced(value = "create-order", kind = SpanKind.INTERNAL)
 * public Order createOrder(String customerId, CreateOrderRequest request) {
 *     // O AOP cria o span automaticamente
 *     // Use Span.current() para adicionar atributos e eventos
 *     return order;
 * }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {

    /**
     * Nome do span. Se não especificado, usa o nome do método.
     */
    String value() default "";

    /**
     * Tipo do span (INTERNAL, SERVER, CLIENT, PRODUCER, CONSUMER).
     * Default: INTERNAL
     */
    SpanKind kind() default SpanKind.INTERNAL;

    /**
     * Atributos estáticos do span no formato "chave:valor".
     * Exemplo: {"operation:create", "entity:order"}
     */
    String[] attributes() default {};
}
