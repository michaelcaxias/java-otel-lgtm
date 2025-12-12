package org.example.javaotellgtm.controller;

import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javaotellgtm.dto.external.JsonPlaceholderPost;
import org.example.javaotellgtm.dto.external.JsonPlaceholderUser;
import org.example.javaotellgtm.service.ExternalApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para demonstrar auto-instrumentação do OpenTelemetry
 * com chamadas a APIs externas via Feign Client.
 *
 * <p>Trace completo:
 * <ul>
 *   <li>HTTP Request (SERVER) - auto-instrumented by Spring Boot
 *   <li>Service (INTERNAL) - instrumented with @TraceSpan
 *   <li>Feign Client (CLIENT) - auto-instrumented by OpenTelemetry
 *   <li>External API
 * </ul>
 *
 * <p>Todo contexto propagado automaticamente via HTTP headers!
 * <p>Note: Controllers are auto-instrumented, no need for @TraceSpan with SERVER kind.
 */
@Slf4j
@RestController
@RequestMapping("/api/external")
@RequiredArgsConstructor
public class ExternalApiController {

    private final ExternalApiService externalApiService;

    /**
     * Demonstra trace completo com múltiplas chamadas HTTP externas.
     * Grafana mostrará: SERVER → INTERNAL → CLIENT (post) → CLIENT (user)
     * Note: This endpoint is auto-instrumented with SERVER span by Spring Boot.
     */
    @GetMapping("/posts/{id}/enriched")
    public ResponseEntity<ExternalApiService.EnrichedPost> getEnrichedPost(@PathVariable Long id) {
        Span span = Span.current();
        span.addEvent("Received request for enriched post");

        log.info("Received request to get enriched post: {}", id);

        ExternalApiService.EnrichedPost enrichedPost = externalApiService.getPostWithAuthor(id);

        span.addEvent("Enriched post retrieved successfully");
        return ResponseEntity.ok(enrichedPost);
    }

    /**
     * Lista todos os posts.
     * Note: This endpoint is auto-instrumented with SERVER span by Spring Boot.
     */
    @GetMapping("/posts")
    public ResponseEntity<List<JsonPlaceholderPost>> getAllPosts() {
        log.info("Received request to get all posts");

        List<JsonPlaceholderPost> posts = externalApiService.getAllPosts();

        return ResponseEntity.ok(posts);
    }

    /**
     * Lista posts de um usuário específico.
     * Note: This endpoint is auto-instrumented with SERVER span by Spring Boot.
     */
    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<List<JsonPlaceholderPost>> getUserPosts(@PathVariable Long userId) {
        log.info("Received request to get posts for user: {}", userId);

        List<JsonPlaceholderPost> posts = externalApiService.getUserPosts(userId);

        return ResponseEntity.ok(posts);
    }

    /**
     * Lista todos os usuários.
     * Note: This endpoint is auto-instrumented with SERVER span by Spring Boot.
     */
    @GetMapping("/users")
    public ResponseEntity<List<JsonPlaceholderUser>> getAllUsers() {
        log.info("Received request to get all users");

        List<JsonPlaceholderUser> users = externalApiService.getAllUsers();

        return ResponseEntity.ok(users);
    }
}
