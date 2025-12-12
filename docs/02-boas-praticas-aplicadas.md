# Boas Práticas de OpenTelemetry Aplicadas

Este documento resume as boas práticas de OpenTelemetry aplicadas no projeto `java-otel-lgtm`.

## 📋 Resumo das Mudanças

### 1. **Nomenclatura de Spans** ✅

Todos os nomes de spans foram padronizados seguindo a convenção OpenTelemetry:
- Formato: `{namespace}.{operation}.{detail}` (usando pontos, não espaços)
- Uso de **snake_case** para componentes multi-palavra
- **Baixa cardinalidade** - sem valores dinâmicos (IDs, timestamps)

**Constantes definidas em `SpanName.java`:**

```java
// Order Operations
public static final String ORDER_CREATE = "order.create";
public static final String ORDER_FETCH = "order.fetch";
public static final String ORDER_LIST_ALL = "order.list.all";
public static final String ORDER_LIST_BY_CUSTOMER = "order.list.by_customer";
public static final String ORDER_UPDATE_STATUS = "order.update.status";
public static final String ORDER_CANCEL = "order.cancel";

// External API Operations
public static final String EXTERNAL_API_GET_POST_WITH_AUTHOR = "external_api.get.post_with_author";
public static final String EXTERNAL_API_GET_USER_POSTS = "external_api.get.user_posts";
public static final String EXTERNAL_API_LIST_POSTS = "external_api.list.posts";
public static final String EXTERNAL_API_LIST_USERS = "external_api.list.users";
```

### 2. **Nomenclatura de Atributos** ✅

Atributos padronizados seguindo convenções semânticas:
- Formato: `{namespace}.{attribute}` (usando pontos)
- Uso de **snake_case**
- **Namespaces padrão:** `user.*`, `customer.*`, `order.*`, `post.*`, `event.*`, `error.*`

**Principais atributos definidos em `AttributeName.java`:**

```java
// User/Customer
USER_ID("user.id")
CUSTOMER_ID("customer.id")
CUSTOMER_NAME("customer.name")

// Order
ORDER_ID("order.id")
ORDER_STATUS("order.status")
ORDER_TOTAL_AMOUNT("order.total_amount")
ORDER_ITEMS_COUNT("order.items_count")
ORDER_PAYMENT_METHOD("order.payment_method")
ORDERS_COUNT("orders.count")

// External API
POST_ID("post.id")
POST_TITLE("post.title")
POSTS_COUNT("posts.count")
EXTERNAL_USER_NAME("external.user.name")

// Events
EVENT_TYPE("event.type")
```

### 3. **Proteção de PII (Personally Identifiable Information)** 🔒

**NUNCA** expostos em spans ou atributos:
- ❌ Email (`customerEmail`, `user.email`)
- ❌ Telefone
- ❌ Documentos (CPF, CNPJ, etc.)
- ❌ Senhas, tokens, API keys

**Implementações:**

#### `Order.java`
```java
private String customerEmail; // Note: NOT exposed in telemetry (PII)

@Override
public Map<String, String> attributes() {
    // Email is intentionally NOT included
    attrs.put(AttributeName.CUSTOMER_ID.getKey(), customerId);
    attrs.put(AttributeName.CUSTOMER_NAME.getKey(), customerName);
    // ... other attributes, but NOT email
}
```

#### `OrderService.java`
```java
// Email removed from @SpanAttribute parameters
public Order createOrder(
        @SpanAttribute("customer.id") String customerId,
        @SpanAttribute("customer.name") String customerName,
        // ❌ customerEmail removed - it's PII
        CreateOrderRequest request) {
```

#### `ExternalApiService.java`
```java
// User email NOT added to spans
SpanWrap.addAttributes(Map.of(
    AttributeName.EXTERNAL_USER_NAME.getKey(), user.getName()
    // ❌ user.getEmail() is NOT added - it's PII
));
```

### 4. **Interface TelemetryEvent** ✅

Implementada em objetos de domínio para expor atributos de forma consistente:

#### `Order.java`
```java
public class Order implements TelemetryEvent {
    @Override
    public Map<String, String> attributes() {
        Map<String, String> attrs = new HashMap<>();

        if (id != null) attrs.put(AttributeName.ORDER_ID.getKey(), id);
        if (customerId != null) attrs.put(AttributeName.CUSTOMER_ID.getKey(), customerId);
        if (status != null) attrs.put(AttributeName.ORDER_STATUS.getKey(), status.name());
        if (totalAmount != null) attrs.put(AttributeName.ORDER_TOTAL_AMOUNT.getKey(), totalAmount.toString());
        // ... mais atributos (exceto email - PII)

        return attrs;
    }
}
```

#### `OrderEvent.java`
```java
public class OrderEvent implements TelemetryEvent {
    private String customerEmail; // Note: NOT exposed in telemetry (PII)

    @Override
    public Map<String, String> attributes() {
        // Email is intentionally NOT included
        attrs.put(AttributeName.ORDER_ID.getKey(), orderId);
        attrs.put(AttributeName.EVENT_TYPE.getKey(), eventType.toString());
        // ... other attributes
    }
}
```

### 5. **Uso de SpanWrap** ✅

Utilizado para adicionar atributos em runtime de forma declarativa:

```java
// Atributos calculados em runtime
SpanWrap.addAttributes(Map.of(
    AttributeName.ORDER_ITEMS_COUNT.getKey(), String.valueOf(orderItems.size()),
    AttributeName.ORDER_TOTAL_AMOUNT.getKey(), totalAmount.toString()
));

// Atributos de objetos TelemetryEvent
SpanWrap.addAttributes(order); // Extrai automaticamente todos os atributos
```

### 6. **SpanKind e Auto-instrumentação** ✅

#### Spans INTERNAL (manual com @TraceSpan)
Aplicado em use cases e lógica de negócio:

```java
@TraceSpan(SpanName.ORDER_CREATE)
public Order createOrder(...) {
    // Lógica de negócio interna
}

@TraceSpan(SpanName.EXTERNAL_API_GET_POST_WITH_AUTHOR)
public EnrichedPost getPostWithAuthor(...) {
    // Coordenação de chamadas externas
}
```

#### Spans AUTO-INSTRUMENTADOS (não adicionar @TraceSpan)
- ⚡ **SERVER** - Controllers REST (já instrumentado pelo Spring Boot)
- ⚡ **CLIENT** - FeignClient/RestTemplate (já instrumentado automaticamente)
- ⚡ **PRODUCER/CONSUMER** - RabbitMQ (já instrumentado automaticamente)

### 7. **Cardinalidade** ✅

#### 🔴 Span Names: BAIXA cardinalidade (ESTRITO)
```java
// ✅ CORRETO - Nome estático
@TraceSpan(SpanName.ORDER_CREATE)

// ❌ ERRADO - Valor dinâmico
@TraceSpan("order.create." + orderId) // NUNCA faça isso!
```

#### 🟡 Attributes: ALTA cardinalidade PERMITIDA
```java
// ✅ IDs únicos são permitidos e úteis para debugging
SpanWrap.addAttributes(Map.of(
    AttributeName.ORDER_ID.getKey(), orderId,        // ✅ Único por pedido
    AttributeName.CUSTOMER_ID.getKey(), customerId,  // ✅ Único por cliente
    AttributeName.ORDER_STATUS.getKey(), "PENDING"   // ✅ Baixa cardinalidade
));
```

### 8. **Eventos nos Spans** ✅

Uso de `span.addEvent()` para marcar pontos importantes na execução:

```java
span.addEvent("Starting order creation");
span.addEvent("Calculating order items");
span.addEvent("Order total calculated");
span.addEvent("Saving order to database");
span.addEvent("Order saved to database");
span.addEvent("Publishing order created event");
```

### 9. **Correção de Imports** ✅

Todos os imports foram corrigidos para apontar para os pacotes corretos:
- `org.example.javaotellgtm.traces.*` (anteriormente apontavam para pacotes inexistentes)

## 📊 Estrutura Final

```
src/main/java/org/example/javaotellgtm/traces/
├── annotation/
│   ├── TraceSpan.java           # Anotação para criar spans
│   └── SpanAttribute.java       # Anotação para parâmetros como atributos
├── aspect/
│   └── TracingAspect.java       # AOP para processar @TraceSpan
├── constants/
│   ├── SpanName.java            # Constantes de nomes de spans
│   └── AttributeName.java       # Constantes de nomes de atributos
├── contract/
│   └── TelemetryEvent.java      # Interface para objetos com telemetria
├── processor/
│   └── SpanWrap.java            # Utilitário para adicionar atributos
└── interceptor/
    └── TelemetryEnrichmentInterceptor.java
```

## ✅ Checklist de Conformidade

- [x] Nomes de spans seguem padrão `namespace.operation.detail`
- [x] Uso de snake_case em nomes
- [x] Spans com baixa cardinalidade (sem IDs dinâmicos)
- [x] Atributos com namespace adequado
- [x] PII (email) removido de todos os spans
- [x] TelemetryEvent implementado em Order e OrderEvent
- [x] SpanWrap usado para atributos runtime
- [x] Constantes centralizadas em SpanName e AttributeName
- [x] @TraceSpan usado apenas para operações INTERNAL
- [x] Imports corrigidos para pacotes corretos
- [x] Documentação de código com avisos sobre PII

## 🎯 Benefícios

1. **Consistência**: Nomes padronizados facilitam queries e dashboards
2. **Segurança**: PII protegido, em conformidade com LGPD/GDPR
3. **Manutenibilidade**: Constantes centralizadas evitam typos
4. **Performance**: Baixa cardinalidade em spans permite agregação eficiente
5. **Debugging**: Atributos com alta cardinalidade ajudam no troubleshooting
6. **Rastreabilidade**: Eventos marcam pontos importantes na execução

## 📚 Referências

- [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/)
- [OpenTelemetry Java](https://opentelemetry.io/docs/languages/java/)
- [Documentação interna: 01-distributed-tracing.md](./01-distributed-tracing.md)
- [Cursor Rules: opentelemetry.mdc](../.cursor/rules/opentelemetry.mdc)
