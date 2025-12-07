package org.example.javaotellgtm.aop;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.javaotellgtm.dto.OrderEvent;
import org.example.javaotellgtm.util.SpanLinkHelper;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Aspect que intercepta métodos anotados com @Traced e cria spans automaticamente.
 * Implementação customizada similar ao @WithSpan do OpenTelemetry.
 * Suporta Span Links para conectar traces relacionados (producer-consumer).
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TracingAspect {

    private final Tracer tracer;

    /**
     * Intercepta métodos anotados com @Traced e cria spans automaticamente.
     * Se detectar OrderEvent com contexto de tracing, cria span com link.
     */
    @Around("@annotation(traced)")
    public Object traceMethod(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        Method method = signature.getMethod();

        // Determinar nome do span
        String spanName = traced.value().isEmpty()
                ? className + "." + method.getName()
                : traced.value();

        // ✅ Criar span com link se encontrar OrderEvent nos parâmetros
        Span span = createSpanWithLinkIfApplicable(spanName, traced, joinPoint.getArgs());

        // Adicionar atributos estáticos da anotação
        for (String attribute : traced.attributes()) {
            String[] parts = attribute.split(":", 2);
            if (parts.length == 2) {
                span.setAttribute(parts[0].trim(), parts[1].trim());
            }
        }

        // Adicionar atributos dos parâmetros anotados com @SpanAttribute
        addParameterAttributes(span, method, joinPoint.getArgs());

        // Adicionar metadados do método
        span.setAttribute("code.function", method.getName());
        span.setAttribute("code.namespace", method.getDeclaringClass().getName());

        // Executar método com span ativo
        try (Scope scope = span.makeCurrent()) {
            log.debug("Starting span: {} [kind={}]", spanName, traced.kind());

            Object result = joinPoint.proceed();

            span.setStatus(StatusCode.OK);
            log.debug("Completed span: {}", spanName);

            return result;

        } catch (Throwable throwable) {
            // Registrar exceção no span
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR, throwable.getMessage());

            log.debug("Span failed: {} - {}", spanName, throwable.getMessage());

            throw throwable;

        } finally {
            // Sempre finalizar o span
            span.end();
        }
    }

    /**
     * ✅ Cria span com link se encontrar OrderEvent nos parâmetros.
     * Isso permite conectar o trace do producer com o trace do consumer.
     */
    private Span createSpanWithLinkIfApplicable(String spanName, Traced traced, Object[] args) {
        // Procurar OrderEvent nos argumentos
        for (Object arg : args) {
            if (arg instanceof OrderEvent event) {
                if (event.getTraceId() != null && event.getSpanId() != null) {
                    log.info("Creating span '{}' with link to producer trace (traceId: {}, spanId: {})",
                            spanName, event.getTraceId(), event.getSpanId());

                    return SpanLinkHelper.createSpanWithLink(
                            tracer,
                            spanName,
                            traced.kind(),
                            event.getTraceId(),
                            event.getSpanId(),
                            event.getTraceFlags()
                    );
                }
            }
        }

        // Criar span normal sem link
        return tracer.spanBuilder(spanName)
                .setSpanKind(traced.kind())
                .setParent(Context.current())
                .startSpan();
    }

    /**
     * Adiciona atributos ao span baseado em parâmetros anotados com @SpanAttribute.
     */
    private void addParameterAttributes(Span span, Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length && i < args.length; i++) {
            Parameter parameter = parameters[i];
            SpanAttribute spanAttribute = parameter.getAnnotation(SpanAttribute.class);

            if (spanAttribute != null && args[i] != null) {
                String attributeName = spanAttribute.value().isEmpty()
                        ? parameter.getName()
                        : spanAttribute.value();

                span.setAttribute(attributeName, args[i].toString());
            }
        }
    }
}
