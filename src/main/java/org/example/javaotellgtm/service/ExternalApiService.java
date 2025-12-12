package org.example.javaotellgtm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.javaotellgtm.client.JsonPlaceholderClient;
import org.example.javaotellgtm.dto.external.JsonPlaceholderPost;
import org.example.javaotellgtm.dto.external.JsonPlaceholderUser;
import org.example.javaotellgtm.traces.annotation.SpanAttribute;
import org.example.javaotellgtm.traces.annotation.TraceSpan;
import org.example.javaotellgtm.traces.constants.AttributeName;
import org.example.javaotellgtm.traces.constants.SpanName;
import org.example.javaotellgtm.traces.processor.SpanWrap;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service para integração com API externa (JSONPlaceholder).
 *
 * <p>Demonstra auto-instrumentação do OpenTelemetry:
 * <ul>
 *   <li>@TraceSpan cria span INTERNAL (nosso AOP custom)
 *   <li>FeignClient cria span CLIENT automaticamente (auto-instrumentação Spring Boot)
 *   <li>Contexto propagado automaticamente via headers HTTP (traceparent)
 * </ul>
 *
 * <p>Note: Email de usuário externo NÃO é adicionado aos spans pois é PII.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiService {

    private final JsonPlaceholderClient jsonPlaceholderClient;

    @TraceSpan(SpanName.EXTERNAL_API_GET_POST_WITH_AUTHOR)
    public EnrichedPost getPostWithAuthor(@SpanAttribute("post.id") Long postId) {
        log.info("Fetching post {} with author details from external API", postId);

        try {
            // ✨ FeignClient creates CLIENT span automatically
            JsonPlaceholderPost post = jsonPlaceholderClient.getPostById(postId);

            SpanWrap.addAttributes(Map.of(
                    AttributeName.POST_TITLE.getKey(), post.getTitle(),
                    AttributeName.POST_USER_ID.getKey(), post.getUserId().toString()
            ));

            // ✨ Auto-instrumented! Another CLIENT span created automatically
            JsonPlaceholderUser user = jsonPlaceholderClient.getUserById(post.getUserId());

            // Note: user.email is NOT added as it's PII (Personally Identifiable Information)
            SpanWrap.addAttributes(Map.of(
                    AttributeName.EXTERNAL_USER_NAME.getKey(), user.getName()
            ));

            log.info("Successfully fetched post {} by author {}", postId, user.getName());

            return new EnrichedPost(post, user);

        } catch (Exception e) {
            // Event ONLY for external API failure (exceptional situation)
            SpanWrap.addEvent("external_api.call.failed", Map.of(
                    "post.id", postId.toString(),
                    "error.message", e.getMessage()
            ));
            throw e;
        }
    }

    /**
     * Busca todos os posts de um usuário.
     */
    @TraceSpan(SpanName.EXTERNAL_API_GET_USER_POSTS)
    public List<JsonPlaceholderPost> getUserPosts(@SpanAttribute("user.id") Long userId) {
        log.info("Fetching all posts for user {}", userId);

        // ✨ FeignClient creates CLIENT span automatically
        List<JsonPlaceholderPost> posts = jsonPlaceholderClient.getPostsByUserId(userId);

        SpanWrap.addAttributes(Map.of(
                AttributeName.POSTS_COUNT.getKey(), String.valueOf(posts.size())
        ));

        log.info("Found {} posts for user {}", posts.size(), userId);

        return posts;
    }

    /**
     * Lista todos os posts disponíveis.
     */
    @TraceSpan(SpanName.EXTERNAL_API_LIST_POSTS)
    public List<JsonPlaceholderPost> getAllPosts() {
        log.info("Fetching all posts from external API");

        // ✨ FeignClient creates CLIENT span automatically
        List<JsonPlaceholderPost> posts = jsonPlaceholderClient.getAllPosts();

        SpanWrap.addAttributes(Map.of(
                AttributeName.POSTS_COUNT.getKey(), String.valueOf(posts.size())
        ));

        log.info("Retrieved {} posts from external API", posts.size());

        return posts;
    }

    /**
     * Lista todos os usuários disponíveis.
     */
    @TraceSpan(SpanName.EXTERNAL_API_LIST_USERS)
    public List<JsonPlaceholderUser> getAllUsers() {
        log.info("Fetching all users from external API");

        // ✨ FeignClient creates CLIENT span automatically
        List<JsonPlaceholderUser> users = jsonPlaceholderClient.getAllUsers();

        SpanWrap.addAttributes(Map.of(
                AttributeName.USERS_COUNT.getKey(), String.valueOf(users.size())
        ));

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
