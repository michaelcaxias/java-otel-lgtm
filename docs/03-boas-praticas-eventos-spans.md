# Boas Práticas para Eventos em Spans

## 🎯 Princípio Fundamental

**Eventos devem ser usados com moderação** - apenas para momentos significativos, excepcionais ou que agregam valor real ao troubleshooting.

---

## ❌ Uso EXCESSIVO de Eventos (Anti-Pattern)

### Problema
Adicionar eventos para cada pequeno passo ou operação trivial:

```java
@TraceSpan(SpanName.ORDER_CREATE)
public Order createOrder(...) {
    span.addEvent("Starting order creation");           // ❌ Redundante
    span.addEvent("Calculating order items");           // ❌ Operação trivial
    span.addEvent("Order total calculated");            // ❌ Não agrega valor
    span.addEvent("Saving order to database");          // ❌ Óbvio pelo contexto
    span.addEvent("Order saved to database");           // ❌ Redundante
    span.addEvent("Publishing order created event");    // ❌ Óbvio
    span.addEvent("Order event published");             // ❌ Redundante
}
```

### Por que é um problema?

1. **Poluição Visual** - Dificulta encontrar informações realmente importantes
2. **Performance** - Overhead desnecessário em cada operação
3. **Armazenamento** - Aumenta custos de storage e indexação
4. **Ruído** - Esconde os eventos que realmente importam
5. **Manutenção** - Mais código para manter sem valor agregado

---

## ✅ Uso APROPRIADO de Eventos

### Quando Usar Eventos

Use eventos **APENAS** para:

1. **Erros e Exceções**
   ```java
   span.addEvent("order.not_found");
   span.addEvent("payment.failed");
   span.addEvent("validation.error");
   ```

2. **Mudanças de Estado Importantes**
   ```java
   span.addEvent("order.status.changed");
   span.addEvent("payment.confirmed");
   ```

3. **Integrações com Sistemas Externos**
   ```java
   span.addEvent("external_api.call.started");
   span.addEvent("external_api.call.failed");
   ```

4. **Pontos de Decisão Críticos**
   ```java
   span.addEvent("retry.attempted");
   span.addEvent("circuit_breaker.opened");
   span.addEvent("fallback.triggered");
   ```

5. **Operações Assíncronas Importantes**
   ```java
   span.addEvent("message.published");
   span.addEvent("async_process.queued");
   ```

### Quando NÃO Usar Eventos

❌ **Não use eventos para:**

1. Operações triviais do fluxo normal
2. Iniciar/finalizar operações (use duration do span)
3. Informações que já estão nos logs
4. Cada chamada de método
5. Operações síncronas simples (query DB, cálculos)

---

## 📋 Exemplos Refatorados

### Antes ❌ (Uso Excessivo)
```java
@TraceSpan(SpanName.ORDER_CREATE)
public Order createOrder(...) {
    span.addEvent("Starting order creation");           // ❌
    span.addEvent("Calculating order items");           // ❌

    // ... cálculos ...

    span.addEvent("Order total calculated");            // ❌
    span.addEvent("Saving order to database");          // ❌
    order = orderRepository.save(order);
    span.addEvent("Order saved to database");           // ❌

    span.addEvent("Publishing order created event");    // ❌
    publishOrderEvent(order, OrderEvent.EventType.ORDER_CREATED);
    span.addEvent("Order event published");             // ❌

    return order;
}
```

### Depois ✅ (Uso Apropriado)
```java
@TraceSpan(SpanName.ORDER_CREATE)
public Order createOrder(...) {
    log.info("Creating new order for customer: {}", customerName);

    // Cálculos e lógica de negócio
    // Spans já capturam a duração, não precisa de eventos

    order = orderRepository.save(order);
    SpanWrap.addAttributes(order);

    // Evento APENAS se a publicação é crítica para rastreamento
    publishOrderEvent(order, OrderEvent.EventType.ORDER_CREATED);

    return order;
}
```

### Exemplo com Erro ✅
```java
@TraceSpan(SpanName.ORDER_FETCH)
public Order getOrder(String orderId) {
    log.info("Fetching order: {}", orderId);

    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> {
                // ✅ Evento para situação excepcional
                SpanWrap.addEvent("order.not_found", Map.of(
                    "order.id", orderId,
                    "error.type", "NOT_FOUND"
                ));
                SpanWrap.addAttributes(Map.of(
                        AttributeName.ERROR.getKey(), "true",
                        AttributeName.ERROR_MESSAGE.getKey(), "Order not found: " + orderId
                ));
                return new RuntimeException("Order not found: " + orderId);
            });

    SpanWrap.addAttributes(order);
    return order;
}
```

### Exemplo com Integração Externa ✅
```java
@TraceSpan(SpanName.EXTERNAL_API_GET_POST_WITH_AUTHOR)
public EnrichedPost getPostWithAuthor(Long postId) {
    log.info("Fetching post {} with author details", postId);

    try {
        JsonPlaceholderPost post = jsonPlaceholderClient.getPostById(postId);
        JsonPlaceholderUser user = jsonPlaceholderClient.getUserById(post.getUserId());

        SpanWrap.addAttributes(Map.of(
                AttributeName.POST_TITLE.getKey(), post.getTitle(),
                AttributeName.POST_USER_ID.getKey(), post.getUserId().toString(),
                AttributeName.EXTERNAL_USER_NAME.getKey(), user.getName()
        ));

        return new EnrichedPost(post, user);

    } catch (Exception e) {
        // ✅ Evento para falha em integração externa
        SpanWrap.addEvent("external_api.call.failed", Map.of(
            "post.id", postId.toString(),
            "error.message", e.getMessage()
        ));
        throw e;
    }
}
```

---

## 🎯 Regras de Ouro

### 1. **Priorize Atributos sobre Eventos**
```java
// ❌ Evento para informação estática
span.addEvent("Order processed with payment method: " + paymentMethod);

// ✅ Atributo para informação estruturada
SpanWrap.addAttributes(Map.of(
    AttributeName.ORDER_PAYMENT_METHOD.getKey(), paymentMethod
));
```

### 2. **Use Logs para Detalhes do Fluxo**
```java
// ❌ Evento para cada passo
span.addEvent("Starting validation");
span.addEvent("Validation completed");

// ✅ Log para fluxo detalhado
log.debug("Starting validation");
// ... validação ...
log.debug("Validation completed");
```

### 3. **Eventos com Contexto Rico**
```java
// ❌ Evento vazio ou genérico
span.addEvent("Error occurred");

// ✅ Evento com atributos significativos
SpanWrap.addEvent("payment.processing.failed", Map.of(
    "payment.method", "CREDIT_CARD",
    "error.code", "INSUFFICIENT_FUNDS",
    "retry.attempt", "3"
));
```

### 4. **Máximo 3-5 Eventos por Span**
- Se você precisa de mais de 5 eventos, provavelmente está usando demais
- Considere criar child spans para operações complexas
- Use atributos e logs para detalhes

---

## 📊 Comparação: Antes vs Depois

### OrderService.createOrder()

| Métrica | Antes (Excessivo) | Depois (Apropriado) |
|---------|-------------------|---------------------|
| Eventos por span | 7 | 0-1 (apenas em erros) |
| Linhas de código | +7 | -6 |
| Clareza | Baixa (ruído) | Alta (limpo) |
| Performance | Overhead | Otimizado |
| Valor agregado | Baixo | Alto |

### ExternalApiService.getPostWithAuthor()

| Métrica | Antes (Excessivo) | Depois (Apropriado) |
|---------|-------------------|---------------------|
| Eventos por span | 6 | 0-1 (apenas em falhas) |
| Informação útil | Redundante | Concisa |
| Troubleshooting | Difícil | Fácil |

---

## 🔍 Quando um Evento Realmente Agrega Valor?

Pergunte-se:

1. **É uma situação excepcional?** (erro, timeout, retry)
2. **Ajuda a entender o que deu errado?**
3. **Não está nos logs ou atributos?**
4. **É importante para troubleshooting?**
5. **Acontece raramente no fluxo normal?**

Se a resposta for **NÃO** para qualquer uma dessas perguntas, provavelmente você **NÃO** deve usar um evento.

---

## 🎓 Lições Aprendidas

1. **Spans já capturam duração** - Não precisa de eventos para início/fim
2. **Atributos são para contexto** - Use-os em vez de eventos descritivos
3. **Logs são para debug** - Detalhes do fluxo vão nos logs, não em eventos
4. **Eventos são para o excepcional** - Use apenas quando algo importante/inesperado acontece
5. **Menos é mais** - Um span limpo é mais útil que um poluído

---

## 📚 Referências

- [OpenTelemetry Events Specification](https://opentelemetry.io/docs/specs/otel/trace/api/#add-events)
- [OpenTelemetry Best Practices](https://opentelemetry.io/docs/concepts/signals/traces/#events)
- [When to use Span Events](https://opentelemetry.io/docs/instrumentation/java/manual/#span-events)

---

**Resumo:** Use eventos com **moderação e propósito**. Priorize atributos para contexto e logs para fluxo detalhado. Eventos devem ser **excepcionais e significativos**.
