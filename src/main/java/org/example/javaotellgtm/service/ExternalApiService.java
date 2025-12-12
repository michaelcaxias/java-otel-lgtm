package org.example.javaotellgtm.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javaotellgtm.client.JsonPlaceholderClient;
import org.example.javaotellgtm.dto.external.JsonPlaceholderPost;
import org.example.javaotellgtm.dto.external.JsonPlaceholderUser;
import org.example.javaotellgtm.traces.annotation.SpanAttribute;
import org.example.javaotellgtm.traces.annotation.TraceSpan;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service para integração com API externa (JSONPlaceholder)
 *
 * Demonstra auto-instrumentação do OpenTelemetry:
 * - @Traced cria span INTERNAL (nosso AOP custom)
 * - FeignClient cria span CLIENT automaticamente (auto-instrumentação Spring Boot)
 * - Contexto propagado automaticamente via headers HTTP (traceparent)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiService {

    private final JsonPlaceholderClient jsonPlaceholderClient;

    @TraceSpan(value = "get-post-with-author", kind = SpanKind.INTERNAL)
    public EnrichedPost getPostWithAuthor(@SpanAttribute("post.id") Long postId) {
        Span span = Span.current();

        log.info("Fetching post {} with author details from external API", postId);
        span.addEvent("Starting external API calls");

        span.addEvent("Fetching post from JSONPlaceholder");
        JsonPlaceholderPost post = jsonPlaceholderClient.getPostById(postId);

        span.setAttribute("post.title", post.getTitle());
        span.setAttribute("post.user_id", post.getUserId().toString());
        span.addEvent("Post fetched successfully");

        // ✨ Auto-instrumentado! Outro span CLIENT criado automaticamente
        span.addEvent("Fetching user from JSONPlaceholder");
        JsonPlaceholderUser user = jsonPlaceholderClient.getUserById(post.getUserId());

        span.setAttribute("user.name", user.getName());
        span.setAttribute("user.email", user.getEmail());
        span.addEvent("User fetched successfully");

        EnrichedPost enrichedPost = new EnrichedPost(post, user);
        span.addEvent("Post enriched with author data");

        log.info("Successfully fetched post {} by author {}", postId, user.getName());

        return enrichedPost;
    }

    /**
     * Busca todos os posts de um usuário
     */
    @TraceSpan(value = "get-user-posts", kind = SpanKind.INTERNAL)
    public List<JsonPlaceholderPost> getUserPosts(@SpanAttribute("user.id") Long userId) {
        Span span = Span.current();

        log.info("Fetching all posts for user {}", userId);
        span.addEvent("Fetching user posts from JSONPlaceholder");

        // ✨ Auto-instrumentado!
        List<JsonPlaceholderPost> posts = jsonPlaceholderClient.getPostsByUserId(userId);

        span.setAttribute("posts.count", posts.size());
        span.addEvent("User posts fetched successfully");

        log.info("Found {} posts for user {}", posts.size(), userId);

        return posts;
    }

    /**
     * Lista todos os posts disponíveis
     */
    @TraceSpan(value = "list-all-posts", kind = SpanKind.INTERNAL)
    public List<JsonPlaceholderPost> getAllPosts() {
        Span span = Span.current();

        log.info("Fetching all posts from external API");
        span.addEvent("Fetching all posts from JSONPlaceholder");

        // ✨ Auto-instrumentado!
        List<JsonPlaceholderPost> posts = jsonPlaceholderClient.getAllPosts();

        span.setAttribute("posts.count", posts.size());
        span.addEvent("All posts fetched successfully");

        log.info("Retrieved {} posts from external API", posts.size());

        return posts;
    }

    /**
     * Lista todos os usuários disponíveis
     */
    @TraceSpan(value = "list-all-users", kind = SpanKind.INTERNAL)
    public List<JsonPlaceholderUser> getAllUsers() {
        Span span = Span.current();

        log.info("Fetching all users from external API");
        span.addEvent("Fetching all users from JSONPlaceholder");

        // ✨ Auto-instrumentado!
        List<JsonPlaceholderUser> users = jsonPlaceholderClient.getAllUsers();

        span.setAttribute("users.count", users.size());
        span.addEvent("All users fetched successfully");

        log.info("Retrieved {} users from external API", users.size());

        return users;
    }

    /**
     * Classe para representar um post enriquecido com dados do autor
     */
    public record EnrichedPost(
            JsonPlaceholderPost post,
            JsonPlaceholderUser author
    ) {}
}
