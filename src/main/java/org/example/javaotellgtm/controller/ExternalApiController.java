package org.example.javaotellgtm.controller;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javaotellgtm.aop.SpanAttribute;
import org.example.javaotellgtm.aop.Traced;
import org.example.javaotellgtm.dto.external.JsonPlaceholderPost;
import org.example.javaotellgtm.dto.external.JsonPlaceholderUser;
import org.example.javaotellgtm.service.ExternalApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para demonstrar auto-instrumentação do OpenTelemetry
 * com chamadas a APIs externas via Feign Client
 *
 * Trace completo:
 * HTTP Request (SERVER) → Service (INTERNAL) → Feign Client (CLIENT) → External API
 * Todo contexto propagado automaticamente!
 */
@Slf4j
@RestController
@RequestMapping("/api/external")
@RequiredArgsConstructor
public class ExternalApiController {

    private final ExternalApiService externalApiService;

    /**
     * Demonstra trace completo com múltiplas chamadas HTTP externas
     * Grafana mostrará: SERVER → INTERNAL → CLIENT (post) → CLIENT (user)
     */
    @GetMapping("/posts/{id}/enriched")
    @Traced(value = "get-enriched-post-endpoint", kind = SpanKind.SERVER,
            attributes = {"http.method:GET", "endpoint:/api/external/posts/{id}/enriched"})
    public ResponseEntity<ExternalApiService.EnrichedPost> getEnrichedPost(
            @PathVariable @SpanAttribute("post.id") Long id) {

        Span span = Span.current();
        span.addEvent("Received request for enriched post");

        log.info("Received request to get enriched post: {}", id);

        ExternalApiService.EnrichedPost enrichedPost = externalApiService.getPostWithAuthor(id);

        span.addEvent("Enriched post retrieved successfully");
        return ResponseEntity.ok(enrichedPost);
    }

    /**
     * Lista todos os posts
     */
    @GetMapping("/posts")
    @Traced(value = "get-all-posts-endpoint", kind = SpanKind.SERVER,
            attributes = {"http.method:GET", "endpoint:/api/external/posts"})
    public ResponseEntity<List<JsonPlaceholderPost>> getAllPosts() {
        log.info("Received request to get all posts");

        List<JsonPlaceholderPost> posts = externalApiService.getAllPosts();

        return ResponseEntity.ok(posts);
    }

    /**
     * Lista posts de um usuário específico
     */
    @GetMapping("/users/{userId}/posts")
    @Traced(value = "get-user-posts-endpoint", kind = SpanKind.SERVER,
            attributes = {"http.method:GET", "endpoint:/api/external/users/{userId}/posts"})
    public ResponseEntity<List<JsonPlaceholderPost>> getUserPosts(
            @PathVariable @SpanAttribute("user.id") Long userId) {

        log.info("Received request to get posts for user: {}", userId);

        List<JsonPlaceholderPost> posts = externalApiService.getUserPosts(userId);

        return ResponseEntity.ok(posts);
    }

    /**
     * Lista todos os usuários
     */
    @GetMapping("/users")
    @Traced(value = "get-all-users-endpoint", kind = SpanKind.SERVER,
            attributes = {"http.method:GET", "endpoint:/api/external/users"})
    public ResponseEntity<List<JsonPlaceholderUser>> getAllUsers() {
        log.info("Received request to get all users");

        List<JsonPlaceholderUser> users = externalApiService.getAllUsers();

        return ResponseEntity.ok(users);
    }
}
