package org.example.javaotellgtm.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para marcar parâmetros de método que devem ser adicionados
 * automaticamente como atributos do span.
 *
 * <p>Exemplo de uso:
 * <pre>
 * {@code
 * @Traced("create-order")
 * public Order createOrder(
 *     @SpanAttribute("customer.id") String customerId,
 *     @SpanAttribute("customer.email") String customerEmail,
 *     CreateOrderRequest request) {
 *     // customerId e customerEmail são automaticamente adicionados ao span
 *     return order;
 * }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface SpanAttribute {

    /**
     * Nome do atributo no span.
     * Se não especificado, usa o nome do parâmetro.
     */
    String value() default "";
}
