# 🎯 Abordagens de Instrumentação OpenTelemetry

## 📋 Três Formas de Instrumentar

### 1. @Observed (Micrometer - O que implementamos)
### 2. @WithSpan e @SpanAttribute (OpenTelemetry nativo)
### 3. Span.current() (API programática)

---

## 🔍 Comparação Detalhada

### 1️⃣ @Observed - Micrometer Observation API

**Origem:** Spring Boot / Micrometer
**Nível:** Abstração alta (vendor-agnostic)

```java
@Observed(
    name = "order.create",
    contextualName = "create-order",
    lowCardinalityKeyValues = {"operation", "create"}
)
public Order createOrder(CreateOrderRequest request) {
    // código
}
```

**Características:**
- ✅ Vendor-agnostic (não depende de OpenTelemetry)
- ✅ Funciona com múltiplos backends (Zipkin, Jaeger, OpenTelemetry, etc)
- ✅ Gera **métricas + traces** automaticamente
- ✅ Integração nativa com Spring Boot Actuator
- ✅ Melhor para aplicações Spring Boot genéricas
- ❌ Menos controle sobre atributos dinâmicos
- ❌ Não tem acesso direto ao Span

**Quando usar:**
- ✅ Aplicações Spring Boot padrão
- ✅ Quando precisa de vendor-neutrality
- ✅ Quando quer métricas + traces juntos
- ✅ Para começar rápido com observabilidade

---

### 2️⃣ @WithSpan + @SpanAttribute - OpenTelemetry Nativo

**Origem:** OpenTelemetry Java Agent
**Nível:** Abstração média (específico OpenTelemetry)

```java
@WithSpan(value = "create-order", kind = SpanKind.INTERNAL)
public Order createOrder(
    @SpanAttribute("customer.id") String customerId,
    CreateOrderRequest request
) {
    Span.current().setAttribute("order.total", totalAmount.toString());
    // código
}
```

**Características:**
- ✅ API oficial do OpenTelemetry
- ✅ Mais controle sobre atributos e tipo de span
- ✅ Suporta `SpanKind` (INTERNAL, SERVER, CLIENT, PRODUCER, CONSUMER)
- ✅ Atributos dinâmicos via `@SpanAttribute`
- ✅ Acesso ao span atual via `Span.current()`
- ✅ Melhor para instrumentação específica OpenTelemetry
- ❌ Vendor lock-in (só funciona com OpenTelemetry)
- ❌ Não gera métricas automaticamente

**Quando usar:**
- ✅ Quando já está comprometido com OpenTelemetry
- ✅ Precisa de controle fino sobre spans
- ✅ Precisa de diferentes tipos de span (SpanKind)
- ✅ Quer adicionar atributos dinâmicos facilmente
- ✅ Para bibliotecas e frameworks

---

### 3️⃣ Span.current() - API Programática

**Origem:** OpenTelemetry Java SDK
**Nível:** Controle total (programático)

```java
public Order createOrder(CreateOrderRequest request) {
    Span span = Span.current();
    span.setAttribute("customer.id", request.getCustomerId());
    span.setAttribute("customer.email", request.getCustomerEmail());
    span.addEvent("Processing order");

    try {
        // código
        span.setAttribute("order.id", order.getId());
        span.setStatus(StatusCode.OK);
    } catch (Exception e) {
        span.recordException(e);
        span.setStatus(StatusCode.ERROR, "Order creation failed");
        throw e;
    }
}
```

**Características:**
- ✅ Controle total sobre o span
- ✅ Adicionar eventos customizados
- ✅ Controlar status manualmente
- ✅ Gravar exceções com contexto
- ✅ Criar sub-spans programaticamente
- ❌ Mais verboso
- ❌ Precisa de span criado previamente (por @WithSpan ou interceptor)
- ❌ Mais propenso a erros

**Quando usar:**
- ✅ Dentro de métodos já instrumentados
- ✅ Adicionar atributos dinâmicos complexos
- ✅ Adicionar eventos em pontos específicos
- ✅ Controle fino sobre status e exceções
- ✅ Para debugging avançado

---

## 🎨 Comparação Prática

### Cenário: Criar um pedido e adicionar atributos

#### Opção 1: @Observed
```java
@Observed(
    name = "order.create",
    lowCardinalityKeyValues = {"operation", "create"}
)
public Order createOrder(CreateOrderRequest request) {
    // Não tem acesso ao span
    // Atributos estáticos apenas
    Order order = processOrder(request);
    return order;
}
```
**Pros:** Simples, limpo
**Cons:** Sem atributos dinâmicos

---

#### Opção 2: @WithSpan + @SpanAttribute
```java
@WithSpan(value = "create-order", kind = SpanKind.INTERNAL)
public Order createOrder(
    @SpanAttribute("customer.id") String customerId,
    CreateOrderRequest request
) {
    // Atributos de parâmetros automáticos
    // Acesso ao span via Span.current()

    Span.current().setAttribute("request.items.count", request.getItems().size());

    Order order = processOrder(request);

    Span.current().setAttribute("order.id", order.getId());
    Span.current().setAttribute("order.total", order.getTotalAmount().toString());

    return order;
}
```
**Pros:** Atributos automáticos de parâmetros, acesso ao span
**Cons:** Ainda é OpenTelemetry-specific

---

#### Opção 3: Apenas Span.current()
```java
// Precisa de um @WithSpan ou @Observed antes
@WithSpan("create-order")
public Order createOrder(CreateOrderRequest request) {
    Span span = Span.current();

    span.setAttribute("customer.id", request.getCustomerId());
    span.setAttribute("customer.email", request.getCustomerEmail());
    span.addEvent("Starting order validation");

    // Validação
    validateOrder(request);
    span.addEvent("Order validated");

    Order order = processOrder(request);
    span.addEvent("Order processed");

    span.setAttribute("order.id", order.getId());
    span.setAttribute("order.total", order.getTotalAmount().toString());

    span.addEvent("Order creation completed", Attributes.of(
        AttributeKey.stringKey("order.id"), order.getId(),
        AttributeKey.longKey("items.count"), (long) order.getItems().size()
    ));

    return order;
}
```
**Pros:** Controle total, eventos customizados
**Cons:** Muito verboso

---

## 🏆 Melhor Abordagem: Híbrida!

### Combinação Recomendada

```java
@WithSpan(value = "create-order", kind = SpanKind.INTERNAL)
public Order createOrder(
    @SpanAttribute("customer.id") String customerId,
    @SpanAttribute("customer.email") String customerEmail,
    CreateOrderRequest request
) {
    Span span = Span.current();

    // Atributos estáticos via @SpanAttribute (automático)
    // Atributos dinâmicos via Span.current()
    span.setAttribute("request.items.count", request.getItems().size());

    span.addEvent("Calculating order total");
    BigDecimal total = calculateTotal(request);

    span.setAttribute("order.total.calculated", total.toString());

    Order order = saveOrder(request, total);

    span.setAttribute("order.id", order.getId());
    span.addEvent("Order created successfully");

    return order;
}
```

**Benefícios:**
- ✅ Atributos de parâmetros automáticos (@SpanAttribute)
- ✅ Atributos dinâmicos quando necessário (Span.current())
- ✅ Eventos para marcos importantes
- ✅ Código ainda relativamente limpo
- ✅ Máxima flexibilidade

---

## 📊 Tabela de Decisão

| Necessidade | @Observed | @WithSpan | Span.current() |
|-------------|-----------|-----------|----------------|
| Simplicidade máxima | ✅ Melhor | ⚠️ Bom | ❌ Verboso |
| Vendor-neutral | ✅ Sim | ❌ Não | ❌ Não |
| Métricas automáticas | ✅ Sim | ❌ Não | ❌ Não |
| Atributos de parâmetros | ❌ Não | ✅ Sim | ❌ Manual |
| Atributos dinâmicos | ❌ Difícil | ✅ Fácil | ✅ Fácil |
| Eventos customizados | ❌ Não | ✅ Via current() | ✅ Sim |
| Controle de SpanKind | ❌ Não | ✅ Sim | ✅ Sim |
| Spring Boot native | ✅ Sim | ⚠️ Precisa config | ⚠️ Precisa config |

---

## 🎯 Recomendação por Camada

### Controllers (HTTP Endpoints)
```java
// Use @Observed para simplicidade
@Observed(name = "http.endpoint.create-order")
public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
    // Spring já adiciona spans HTTP automáticos
}
```

### Services (Lógica de Negócio)
```java
// Use @WithSpan + Span.current() para flexibilidade
@WithSpan(value = "create-order", kind = SpanKind.INTERNAL)
public Order createOrder(
    @SpanAttribute("customer.id") String customerId,
    CreateOrderRequest request
) {
    Span.current().setAttribute("order.total", total.toString());
    // ...
}
```

### Message Consumers
```java
// Use @WithSpan com CONSUMER kind
@RabbitListener(queues = "order.queue")
@WithSpan(value = "handle-order-created", kind = SpanKind.CONSUMER)
public void handleOrderCreated(
    @SpanAttribute("order.id") String orderId,
    OrderEvent event
) {
    Span.current().addEvent("Processing order event");
    // ...
}
```

### Message Publishers
```java
// Use @WithSpan com PRODUCER kind
@WithSpan(value = "publish-order-event", kind = SpanKind.PRODUCER)
public void publishOrderEvent(
    @SpanAttribute("event.type") String eventType,
    OrderEvent event
) {
    Span.current().setAttribute("destination.queue", queueName);
    // ...
}
```

---

## 🔧 Configuração Necessária

### Para @WithSpan funcionar, adicione:

**build.gradle:**
```gradle
dependencies {
    implementation 'io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.0.0'
}
```

**Nenhuma configuração adicional necessária** - O Spring Boot auto-configura!

---

## 📝 Exemplo Completo

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    @WithSpan(value = "create-order", kind = SpanKind.INTERNAL)
    public Order createOrder(
        @SpanAttribute("customer.id") String customerId,
        @SpanAttribute("customer.email") String customerEmail,
        CreateOrderRequest request
    ) {
        Span span = Span.current();

        // Evento: início do processamento
        span.addEvent("Starting order creation");

        // Atributo dinâmico
        span.setAttribute("items.count", request.getItems().size());

        // Lógica de negócio
        List<OrderItem> items = processItems(request.getItems());
        BigDecimal total = calculateTotal(items);

        // Mais atributos
        span.setAttribute("order.total", total.toString());
        span.addEvent("Order total calculated", Attributes.of(
            AttributeKey.stringKey("total"), total.toString()
        ));

        // Salvar
        Order order = orderRepository.save(buildOrder(request, items, total));

        // Atributo final
        span.setAttribute("order.id", order.getId());
        span.addEvent("Order saved to database");

        // Publicar evento
        publishOrderEvent(order);
        span.addEvent("Order event published");

        return order;
    }
}
```

---

## 🎓 Conclusão

- **@Observed**: Use para simplicidade e vendor-neutrality
- **@WithSpan**: Use para controle sobre spans OpenTelemetry
- **Span.current()**: Use dentro de métodos @WithSpan para atributos dinâmicos
- **Híbrido**: Combine @WithSpan + Span.current() para melhor resultado!
