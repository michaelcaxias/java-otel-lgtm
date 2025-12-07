package org.example.javaotellgtm.client;

import org.example.javaotellgtm.dto.external.JsonPlaceholderPost;
import org.example.javaotellgtm.dto.external.JsonPlaceholderUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign Client para JSONPlaceholder API (https://jsonplaceholder.typicode.com)
 * API pública gratuita para testes
 *
 * OpenTelemetry auto-instrumenta chamadas Feign automaticamente via spring-boot-starter-opentelemetry
 * Cria spans CLIENT automáticos para cada chamada HTTP
 */
@FeignClient(
        name = "jsonplaceholder",
        url = "https://jsonplaceholder.typicode.com"
)
public interface JsonPlaceholderClient {

    /**
     * Busca todos os posts
     * Auto-instrumentado: Span CLIENT criado automaticamente
     */
    @GetMapping("/posts")
    List<JsonPlaceholderPost> getAllPosts();

    /**
     * Busca um post por ID
     * Auto-instrumentado: Span CLIENT criado automaticamente
     */
    @GetMapping("/posts/{id}")
    JsonPlaceholderPost getPostById(@PathVariable("id") Long id);

    /**
     * Busca posts de um usuário específico
     * Auto-instrumentado: Span CLIENT criado automaticamente
     */
    @GetMapping("/posts?userId={userId}")
    List<JsonPlaceholderPost> getPostsByUserId(@PathVariable("userId") Long userId);

    /**
     * Busca todos os usuários
     * Auto-instrumentado: Span CLIENT criado automaticamente
     */
    @GetMapping("/users")
    List<JsonPlaceholderUser> getAllUsers();

    /**
     * Busca um usuário por ID
     * Auto-instrumentado: Span CLIENT criado automaticamente
     */
    @GetMapping("/users/{id}")
    JsonPlaceholderUser getUserById(@PathVariable("id") Long id);
}
