package org.example.javaotellgtm.util;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class para criar Span Links no OpenTelemetry.
 * Span Links conectam traces relacionados que não têm relação pai-filho direta.
 *
 * Caso de uso típico: Producer-Consumer pattern
 * - Producer cria span e publica mensagem com traceId/spanId
 * - Consumer cria novo span com LINK para o span do producer
 */
@Slf4j
@UtilityClass
public class SpanLinkHelper {

    /**
     * Cria um SpanContext a partir de traceId e spanId em formato String.
     *
     * @param traceId TraceId em formato hexadecimal (32 chars)
     * @param spanId SpanId em formato hexadecimal (16 chars)
     * @param traceFlags Flags do trace em formato hexadecimal (2 chars)
     * @return SpanContext válido ou SpanContext.getInvalid() em caso de erro
     */
    public static SpanContext createSpanContext(String traceId, String spanId, String traceFlags) {
        if (traceId == null || spanId == null) {
            log.warn("Cannot create SpanContext: traceId or spanId is null");
            return SpanContext.getInvalid();
        }

        try {
            TraceFlags flags = traceFlags != null
                ? TraceFlags.fromHex(traceFlags, 0)
                : TraceFlags.getDefault();

            return SpanContext.createFromRemoteParent(
                    traceId,
                    spanId,
                    flags,
                    TraceState.getDefault()
            );
        } catch (Exception e) {
            log.error("Error creating SpanContext from traceId: {}, spanId: {}", traceId, spanId, e);
            return SpanContext.getInvalid();
        }
    }

    /**
     * Cria um span com link para outro span (producer/consumer pattern).
     *
     * @param tracer Tracer do OpenTelemetry
     * @param spanName Nome do novo span
     * @param spanKind Tipo do span (CONSUMER, INTERNAL, etc)
     * @param linkedTraceId TraceId do span a ser linkado
     * @param linkedSpanId SpanId do span a ser linkado
     * @param linkedTraceFlags Flags do trace a ser linkado
     * @return Span iniciado com link ou span normal se contexto inválido
     */
    public static Span createSpanWithLink(
            Tracer tracer,
            String spanName,
            SpanKind spanKind,
            String linkedTraceId,
            String linkedSpanId,
            String linkedTraceFlags) {

        SpanContext linkedContext = createSpanContext(linkedTraceId, linkedSpanId, linkedTraceFlags);

        if (!linkedContext.isValid()) {
            log.warn("Creating span '{}' without link due to invalid context", spanName);
            return tracer.spanBuilder(spanName)
                    .setSpanKind(spanKind)
                    .setParent(Context.current())
                    .startSpan();
        }

        log.debug("Creating span '{}' with link to traceId: {}, spanId: {}",
                spanName, linkedTraceId, linkedSpanId);

        return tracer.spanBuilder(spanName)
                .setSpanKind(spanKind)
                .setParent(Context.current())
                .addLink(linkedContext)  // ← SPAN LINK!
                .startSpan();
    }
}
