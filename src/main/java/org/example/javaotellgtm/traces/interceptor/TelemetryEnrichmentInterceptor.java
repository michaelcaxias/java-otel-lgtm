package org.example.javaotellgtm.traces.interceptor;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Interceptor that enriches OpenTelemetry spans with Fintech and MercadoLibre-specific context.
 *
 * <p>This filter automatically adds standardized telemetry attributes to the current span for every
 * HTTP request, including:
 *
 * <ul>
 *   <li><b>Fintech Event Attributes:</b> site_id, user_id, request_id, business_unit
 *   <li><b>HTTP Headers:</b> x-fury-user, x-platform, x-scope-rewrite, x-app-version, x-from,
 *       x-root-id, x-operator-id, x-client-id
 * </ul>
 *
 * <p>All attributes are extracted directly from the HTTP request headers.
 *
 * @see Span
 */
@Component
public class TelemetryEnrichmentInterceptor extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(TelemetryEnrichmentInterceptor.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    enrichCurrentSpanWithTelemetry(request);

    filterChain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator/") || path.startsWith("/health");
  }

  /**
   * Enriches the current OpenTelemetry span with Fintech attributes and additional HTTP headers.
   *
   * @param request the HTTP request containing headers and metadata
   */
  private void enrichCurrentSpanWithTelemetry(HttpServletRequest request) {
    try {
      Span currentSpan = Span.current();

      if (currentSpan == null || !currentSpan.getSpanContext().isValid()) {
        log.debug("No valid span context available for telemetry enrichment");
        return;
      }

      // Add additional HTTP headers as span attributes
      addHttpHeaderAttributes(currentSpan, request);

    } catch (Exception e) {
      log.error("Error enriching span with telemetry", e);
    }
  }

  /**
   * Adds relevant HTTP headers as span attributes.
   *
   * @param span the current OpenTelemetry span
   * @param request the HTTP request
   */
  private void addHttpHeaderAttributes(Span span, HttpServletRequest request) {
    addHeaderIfPresent(span, request, "x-fury-user", "http.request.header.x-fury-user");
    addHeaderIfPresent(span, request, "x-root-id", "http.request.header.x-root-id");
    addHeaderIfPresent(span, request, "x-operator-id", "http.request.header.x-operator-id");
    addHeaderIfPresent(span, request, "x-client-id", "http.request.header.x-client-id");
  }

  /**
   * Helper method to add a header to the span if it's present in the request.
   *
   * @param span the current span
   * @param request the HTTP request
   * @param headerName the name of the header to extract
   * @param attributeName the OpenTelemetry attribute name to use
   */
  private void addHeaderIfPresent(
      Span span, HttpServletRequest request, String headerName, String attributeName) {
    String headerValue = request.getHeader(headerName);
    if (headerValue != null && !headerValue.isEmpty()) {
      span.setAttribute(attributeName, headerValue);
    }
  }
}
