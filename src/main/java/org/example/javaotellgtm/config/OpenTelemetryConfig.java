package org.example.javaotellgtm.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

    /**
     * Provides a Tracer bean with application-specific instrumentation scope.
     *
     * <p>The instrumentation scope name will appear as 'instrumentation.library.name' in the span
     * attributes, helping to identify the origin of spans.
     *
     * @return configured Tracer instance with instrumentation scope
     */
    @Bean
    public Tracer tracer() {
        return GlobalOpenTelemetry.get().getTracer("java-otel-lgtm");
    }
}
