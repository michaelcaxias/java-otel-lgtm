package org.example.javaotellgtm.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representando um Post da API JSONPlaceholder
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonPlaceholderPost {
    private Long id;
    private Long userId;
    private String title;
    private String body;
}
