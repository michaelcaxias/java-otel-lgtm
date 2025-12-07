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
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Aspect que intercepta métodos anotados com @Traced e cria spans automaticamente.
 * Implementação customizada similar ao @WithSpan do OpenTelemetry.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TracingAspect {

    private final Tracer tracer;

    /**
     * Intercepta métodos anotados com @Traced e cria spans automaticamente.
     */
    @Around("@annotation(traced)")
    public Object traceMethod(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Determinar nome do span
        String spanName = traced.value().isEmpty()
                ? method.getName()
                : traced.value();

        // Criar span
        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(traced.kind())
                .setParent(Context.current())
                .startSpan();

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
