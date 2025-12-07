# 🎯 Custom AOP Tracing - Implementação Própria

Este documento explica a **implementação customizada de AOP (Aspect-Oriented Programming)** para tracing com OpenTelemetry, criada especificamente para este projeto.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Componentes](#componentes)
- [Como Usar](#como-usar)
- [Exemplos](#exemplos)
- [Comparação com @WithSpan](#comparação-com-withspan)
- [Vantagens](#vantagens)

---

## 🎓 Visão Geral

Implementamos um **sistema AOP customizado** que intercepta métodos anotados com `@Traced` e cria spans automaticamente usando o `Tracer` do OpenTelemetry.

### Por que criar nossa própria solução?

1. **Controle total** sobre a criação e gerenciamento de spans
2. **Flexibilidade** para adicionar features customizadas
3. **Aprendizado** profundo de como AOP e OpenTelemetry funcionam
4. **Independência** de bibliotecas externas específicas
5. **Simplicidade** - API mais limpa e fácil de usar

---

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│                    @Traced Annotation                        │
│  (marca métodos que devem ter spans automaticamente)        │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                     TracingAspect                            │
│  (intercepta métodos e cria spans usando Tracer)            │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  OpenTelemetry Tracer                        │
│         (cria e gerencia spans nativamente)                  │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Grafana LGTM Stack                        │
│              (coleta e visualiza traces)                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🧩 Componentes

### 1. `@Traced` - Anotação Principal

```java
@Traced(
    value = "nome-do-span",           // Nome do span (opcional)
    kind = SpanKind.INTERNAL,          // Tipo: INTERNAL, SERVER, CLIENT, PRODUCER, CONSUMER
    attributes = {"key:value", ...}    // Atributos estáticos
)
```

**Localização**: `org.example.javaotellgtm.aop.Traced`

**Uso**: Anota métodos que devem criar spans automaticamente

### 2. `@SpanAttribute` - Anotação de Parâmetros

```java
public Order getOrder(@SpanAttribute("order.id") String orderId) {
    // orderId é automaticamente adicionado ao span como atributo
}
```

**Localização**: `org.example.javaotellgtm.aop.SpanAttribute`

**Uso**: Marca parâmetros que devem ser adicionados como atributos do span

### 3. `TracingAspect` - Aspect Core

**Localização**: `org.example.javaotellgtm.aop.TracingAspect`

**Responsabilidades**:
- Intercepta métodos anotados com `@Traced`
- Cria spans usando `Tracer`
- Gerencia ciclo de vida do span (`startSpan`, `makeCurrent`, `end`)
- Adiciona atributos automaticamente
- Registra exceções e define status de erro

**Implementação**:

```java
@Around("@annotation(traced)")
public Object traceMethod(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
    // 1. Criar span
    Span span = tracer.spanBuilder(spanName)
            .setSpanKind(traced.kind())
            .setParent(Context.current())
            .startSpan();

    // 2. Adicionar atributos
    // ...

    // 3. Executar método com span ativo
    try (Scope scope = span.makeCurrent()) {
        Object result = joinPoint.proceed();
        span.setStatus(StatusCode.OK);
        return result;
    } catch (Throwable throwable) {
        span.recordException(throwable);
        span.setStatus(StatusCode.ERROR, throwable.getMessage());
        throw throwable;
    } finally {
        span.end();
    }
}
```

---

## 📖 Como Usar

### 1. Métodos Simples

```java
@Traced(value = "get-order", kind = SpanKind.INTERNAL)
public Order getOrder(String orderId) {
    // Span criado automaticamente!
    return orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
}
```

### 2. Com Atributos Estáticos

```java
@Traced(
    value = "create-order",
    kind = SpanKind.INTERNAL,
    attributes = {"operation:create", "entity:order"}
)
public Order createOrder(CreateOrderRequest request) {
    // Span criado com atributos operation=create e entity=order
    // ...
}
```

### 3. Com Atributos Dinâmicos (Parâmetros)

```java
@Traced(value = "update-status", kind = SpanKind.INTERNAL)
public Order updateOrderStatus(
        @SpanAttribute("order.id") String orderId,
        @SpanAttribute("new.status") OrderStatus newStatus) {
    // Span criado com atributos:
    // - order.id = valor de orderId
    // - new.status = valor de newStatus
    // ...
}
```

### 4. Endpoints HTTP (SpanKind.SERVER)

```java
@PostMapping
@Traced(
    value = "create-order-endpoint",
    kind = SpanKind.SERVER,
    attributes = {"http.method:POST", "endpoint:/api/orders"}
)
public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
    // Span SERVER criado automaticamente
    // ...
}
```

### 5. Produtores de Mensagens (SpanKind.PRODUCER)

```java
@Traced(
    value = "publish-order-event",
    kind = SpanKind.PRODUCER,
    attributes = {"messaging.system:rabbitmq"}
)
public void publishOrderEvent(OrderEvent event) {
    rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
}
```

### 6. Consumidores de Mensagens (SpanKind.CONSUMER)

```java
@RabbitListener(queues = ORDER_QUEUE)
@Traced(
    value = "handle-order-created",
    kind = SpanKind.CONSUMER,
    attributes = {"messaging.system:rabbitmq", "messaging.operation:process"}
)
public void handleOrderCreated(OrderEvent event) {
    // Span CONSUMER criado automaticamente
    // ...
}
```

### 7. Usando Span.current() para Customização

Você pode adicionar atributos e eventos adicionais dentro do método:

```java
@Traced(value = "process-order", kind = SpanKind.INTERNAL)
public void processOrder(String orderId) {
    Span span = Span.current();

    span.addEvent("Starting order processing");
    span.setAttribute("processor.name", "main-processor");

    // ... lógica de negócio ...

    span.addEvent("Order processing completed");
}
```

---

## 🆚 Comparação com @WithSpan

| Aspecto | **@Traced (Nossa Solução)** | **@WithSpan (OpenTelemetry)** |
|---------|----------------------------|-------------------------------|
| **Implementação** | Customizada, usando AOP próprio | Fornecida pelo OpenTelemetry |
| **Controle** | Total controle sobre o código | Limitado às features da lib |
| **Flexibilidade** | Alta - podemos adicionar qualquer feature | Média - limitado pela API |
| **Atributos Estáticos** | Suportado via `attributes` | Não suportado |
| **Gestão de Span** | Gerenciamento completo (start, current, end) | Gerenciado automaticamente |
| **Exceções** | Registradas com `recordException()` | Registradas automaticamente |
| **Status** | Controlado explicitamente | Definido automaticamente |
| **Aprendizado** | Alto - entendemos cada detalhe | Baixo - é uma caixa preta |
| **Manutenção** | Nossa responsabilidade | Mantido pela comunidade |

---

## ✅ Vantagens

### 1. **Transparência Total**
```java
// Sabemos EXATAMENTE o que acontece por trás dos panos
@Traced("my-operation")
public void myMethod() { ... }

// Equivalente a:
Span span = tracer.spanBuilder("my-operation").startSpan();
try (Scope scope = span.makeCurrent()) {
    myMethod();
    span.setStatus(StatusCode.OK);
} catch (Throwable e) {
    span.recordException(e);
    span.setStatus(StatusCode.ERROR);
    throw e;
} finally {
    span.end();
}
```

### 2. **Atributos Estáticos Built-in**
```java
@Traced(
    value = "publish-event",
    attributes = {"messaging.system:rabbitmq", "messaging.destination:orders"}
)
```

Sem nossa solução, você precisaria fazer:
```java
@WithSpan("publish-event")
public void publish() {
    Span.current().setAttribute("messaging.system", "rabbitmq");
    Span.current().setAttribute("messaging.destination", "orders");
    // ...
}
```

### 3. **Anotação de Parâmetros Simplificada**
```java
@Traced("get-order")
public Order getOrder(@SpanAttribute("order.id") String id) { ... }
```

### 4. **Totalmente Compatível com OpenTelemetry**
- Usa `Tracer` nativo do OpenTelemetry
- Spans aparecem normalmente no Grafana Tempo
- Compatível com todas as features de tracing

### 5. **Fácil de Estender**
Quer adicionar logging automático? É só modificar o `TracingAspect`:

```java
@Around("@annotation(traced)")
public Object traceMethod(ProceedingJoinPoint joinPoint, Traced traced) {
    // ... código existente ...

    // NOVA FEATURE: Log automático
    log.info("Executing traced method: {}", traced.value());

    // ... resto do código ...
}
```

---

## 🎯 Boas Práticas

### 1. **Use SpanKind Apropriado**
- `INTERNAL`: Operações internas (services, utils)
- `SERVER`: Endpoints HTTP (controllers)
- `CLIENT`: Chamadas HTTP externas
- `PRODUCER`: Publicação de mensagens
- `CONSUMER`: Consumo de mensagens

### 2. **Nomeie Spans de Forma Descritiva**
```java
// ❌ Ruim
@Traced("process")

// ✅ Bom
@Traced("process-payment")
```

### 3. **Use Atributos Estáticos para Metadados Fixos**
```java
@Traced(
    value = "send-email",
    attributes = {
        "messaging.system:smtp",
        "email.provider:sendgrid"
    }
)
```

### 4. **Use @SpanAttribute para Dados Dinâmicos**
```java
@Traced("process-payment")
public void processPayment(
    @SpanAttribute("payment.id") String paymentId,
    @SpanAttribute("payment.amount") BigDecimal amount
) { ... }
```

### 5. **Combine com Span.current() para Eventos**
```java
@Traced("complex-operation")
public void complexOperation() {
    Span span = Span.current();

    span.addEvent("Step 1: Validation");
    validate();

    span.addEvent("Step 2: Processing");
    process();

    span.addEvent("Step 3: Finalization");
    finalize();
}
```

---

## 🔧 Configuração

### Dependências (build.gradle)

```gradle
dependencies {
    // Spring AOP
    implementation 'org.springframework:spring-aop'
    implementation 'org.aspectj:aspectjweaver:1.9.22.1'

    // OpenTelemetry
    implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
}
```

### Application Configuration (application.yml)

```yaml
spring:
  aop:
    proxy-target-class: true

management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
```

---

## 📊 Resultado no Grafana Tempo

Todos os spans criados com `@Traced` aparecem no Grafana Tempo com:
- ✅ Nome correto do span
- ✅ SpanKind correto (INTERNAL, SERVER, CONSUMER, etc.)
- ✅ Atributos estáticos e dinâmicos
- ✅ Eventos (`addEvent`)
- ✅ Exceções registradas
- ✅ Status (OK ou ERROR)
- ✅ Hierarquia de spans (parent-child)
- ✅ Contexto propagado corretamente

---

## 🎓 Conclusão

Nossa implementação customizada de AOP para tracing combina o **melhor dos dois mundos**:

1. **Simplicidade** do `@WithSpan` (apenas anotar métodos)
2. **Poder** do `Tracer` manual (controle total)

Resultado: **código limpo, rastreável e totalmente observável**! 🎉

---

## 📚 Referências

- [Spring AOP Documentation](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [AspectJ](https://www.eclipse.org/aspectj/)
- [OpenTelemetry Java SDK](https://opentelemetry.io/docs/languages/java/)
- [OpenTelemetry Tracing API](https://opentelemetry.io/docs/languages/java/instrumentation/)
