# 📊 Status de Instrumentação OpenTelemetry

Este documento resume **o que é instrumentado** e **como** neste projeto.

## 🎯 Resumo Executivo

| Tipo | Método | Status | Spans Gerados |
|------|--------|--------|---------------|
| **HTTP Requests** | Auto (Spring Boot) | ✅ Ativo | SERVER |
| **HTTP Clients (Feign)** | Auto (Spring Boot) | ✅ Ativo | CLIENT |
| **RabbitMQ Producer** | Auto (Spring Boot) | ✅ Ativo | PRODUCER |
| **RabbitMQ Consumer** | Auto (Spring Boot) | ✅ Ativo | CONSUMER |
| **MongoDB** | Auto (Spring Boot) | ✅ Ativo | CLIENT |
| **Métodos de Negócio** | Manual (@Traced) | ✅ Ativo | INTERNAL |
| **Context Propagation** | Auto (W3C) | ✅ Ativo | - |

---

## 🤖 Auto-Instrumentação (Spring Boot)

### Dependência Base
```gradle
implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
```

### O que é Auto-Instrumentado

#### 1. **Controllers (HTTP Requests)**
```java
@RestController
public class OrderController {
    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable String id) {
        // ✅ Span SERVER criado automaticamente
        // ✅ Atributos: http.method, http.route, http.status_code
        return orderService.getOrder(id);
    }
}
```

**Spans gerados:**
- `SpanKind.SERVER`
- Nome: `GET /orders/{id}`
- Atributos: `http.method=GET`, `http.route=/orders/{id}`, `http.status_code=200`

---

#### 2. **Feign Client (HTTP Externo)**
```java
@FeignClient(name = "jsonplaceholder", url = "https://jsonplaceholder.typicode.com")
public interface JsonPlaceholderClient {
    @GetMapping("/posts/{id}")
    JsonPlaceholderPost getPostById(@PathVariable("id") Long id);
    // ✅ Span CLIENT criado automaticamente
    // ✅ Headers W3C (traceparent) enviados automaticamente
    // ✅ Contexto propagado para API externa
}
```

**Spans gerados:**
- `SpanKind.CLIENT`
- Nome: `GET`
- Atributos: `http.method=GET`, `http.url=https://...`, `http.status_code=200`

**Exemplo de Uso:**
```java
// Chamada no ExternalApiController
GET /api/external/posts/1/enriched

Trace completo:
├─ [SERVER] get-enriched-post-endpoint        ← Auto
│  └─ [INTERNAL] get-post-with-author         ← @Traced (nosso)
│     ├─ [CLIENT] GET /posts/1                ← Auto (Feign)
│     └─ [CLIENT] GET /users/1                ← Auto (Feign)
```

---

#### 3. **RabbitMQ Producer**
```java
@Service
public class MessagePublisher {
    public void publishOrderEvent(OrderEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, KEY, event);
        // ✅ Span PRODUCER criado automaticamente
        // ✅ Headers W3C injetados na mensagem RabbitMQ
        // ✅ Contexto propagado para consumer
    }
}
```

**Spans gerados:**
- `SpanKind.PRODUCER`
- Nome: `order.exchange send`
- Atributos: `messaging.system=rabbitmq`, `messaging.destination=order.exchange`

---

#### 4. **RabbitMQ Consumer**
```java
@Service
public class OrderEventConsumer {
    @RabbitListener(queues = "order.queue")
    public void handleOrder(OrderEvent event) {
        // ✅ Span CONSUMER criado automaticamente
        // ✅ Contexto extraído dos headers da mensagem
        // ✅ Span é filho do producer!
    }
}
```

**Spans gerados:**
- `SpanKind.CONSUMER`
- Nome: `order.queue receive`
- Atributos: `messaging.system=rabbitmq`, `messaging.source=order.queue`

**Trace completo:**
```
POST /api/orders
├─ [SERVER] POST /api/orders                  ← Auto
│  └─ [INTERNAL] create-order                 ← @Traced (nosso)
│     ├─ [CLIENT] MongoDB insert              ← Auto
│     └─ [PRODUCER] order.exchange send       ← Auto
│        │
│        └─ [CONSUMER] order.queue receive    ← Auto (MESMO TRACE!)
│           └─ [INTERNAL] handle-order        ← @Traced (nosso)
```

---

#### 5. **MongoDB**
```java
@Service
public class OrderService {
    public Order getOrder(String id) {
        Order order = orderRepository.findById(id).orElseThrow();
        // ✅ Span CLIENT criado automaticamente para MongoDB
        return order;
    }
}
```

**Spans gerados:**
- `SpanKind.CLIENT`
- Nome: `findById`
- Atributos: `db.system=mongodb`, `db.operation=findById`, `db.name=orders_db`

---

## 🎨 Instrumentação Manual (AOP Custom)

### Dependências
```gradle
implementation 'org.springframework:spring-aop'
implementation 'org.aspectj:aspectjweaver:1.9.22.1'
```

### Anotação @Traced

Implementação própria de AOP que usa `Tracer` internamente.

```java
@Traced(
    value = "create-order",              // Nome do span (opcional)
    kind = SpanKind.INTERNAL,            // Tipo do span
    attributes = {"operation:create"}    // Atributos estáticos
)
public Order createOrder(
    @SpanAttribute("customer.id") String customerId,  // Atributo dinâmico
    CreateOrderRequest request
) {
    Span span = Span.current();  // Acesso ao span atual
    span.addEvent("Creating order");
    // ...
}
```

**Spans gerados:**
- `SpanKind.INTERNAL` (ou outro conforme especificado)
- Nome: Valor de `value` ou `ClassName.methodName`
- Atributos: Estáticos da anotação + parâmetros com `@SpanAttribute`

---

## 🌐 Propagação de Contexto

### Automática (W3C Trace Context)

```yaml
# application.yml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
```

#### Como Funciona

1. **HTTP Request** → Header `traceparent` extraído automaticamente
2. **Service Call** → Contexto mantido via `Context.current()`
3. **RabbitMQ** → Header `traceparent` injetado/extraído automaticamente
4. **Feign Client** → Header `traceparent` enviado automaticamente
5. **MongoDB** → Contexto propagado automaticamente

**Resultado:** UM único trace do começo ao fim!

---

## 📊 Comparação de Complexidade

### Sem Auto-Instrumentação (Hipotético)

```java
// 😰 Controller
@GetMapping("/orders/{id}")
public Order getOrder(@PathVariable String id) {
    Span span = tracer.spanBuilder("GET /orders/" + id)
        .setSpanKind(SpanKind.SERVER).startSpan();
    try (Scope scope = span.makeCurrent()) {
        span.setAttribute("http.method", "GET");
        // ... 10+ linhas
        return orderService.getOrder(id);
    } finally {
        span.end();
    }
}

// 😰 Feign Client
@GetMapping("/posts/{id}")
default Post getPost(@PathVariable Long id) {
    Span span = tracer.spanBuilder("GET /posts/" + id)
        .setSpanKind(SpanKind.CLIENT).startSpan();
    try (Scope scope = span.makeCurrent()) {
        span.setAttribute("http.url", "https://...");
        // ... 15+ linhas
        return getPostInternal(id);
    } finally {
        span.end();
    }
}

// 😰 RabbitMQ Producer
public void publish(Event event) {
    Span span = tracer.spanBuilder("send-message")
        .setSpanKind(SpanKind.PRODUCER).startSpan();
    try (Scope scope = span.makeCurrent()) {
        // Injetar contexto manualmente
        // ... 20+ linhas
        rabbitTemplate.send(message);
    } finally {
        span.end();
    }
}
```

**Código total:** ~100+ linhas de tracing manual

---

### Com Auto-Instrumentação (Atual)

```java
// ✅ Controller
@GetMapping("/orders/{id}")
public Order getOrder(@PathVariable String id) {
    return orderService.getOrder(id);  // Span SERVER automático!
}

// ✅ Feign Client
@FeignClient(url = "https://api.com")
public interface ApiClient {
    @GetMapping("/posts/{id}")
    Post getPost(@PathVariable Long id);  // Span CLIENT automático!
}

// ✅ RabbitMQ Producer
public void publish(Event event) {
    rabbitTemplate.send(event);  // Span PRODUCER automático!
}
```

**Código total:** 0 linhas de tracing! 🎉

**Redução:** 100+ linhas → 0 linhas = **100% menos código**

---

## 🎯 Níveis de Instrumentação

### Nível 1: Spring Boot Starter (ATUAL) ✅

```gradle
implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
```

**Instrumenta:**
- ✅ HTTP (Spring MVC)
- ✅ Feign Client
- ✅ RestTemplate / WebClient
- ✅ RabbitMQ
- ✅ MongoDB
- ✅ Context Propagation (W3C)

**Não Instrumenta:**
- ❌ JDBC genérico
- ❌ Redis
- ❌ Kafka (precisa dependência extra)
- ❌ gRPC

---

### Nível 2: Java Agent (MAIS COMPLETO) 🚀

```bash
# Download
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Executar
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=java-otel-lgtm \
     -Dotel.traces.exporter=otlp \
     -Dotel.exporter.otlp.endpoint=http://localhost:4318 \
     -jar build/libs/java-otel-lgtm-0.0.1-SNAPSHOT.jar
```

**Instrumenta TUDO do Nível 1 +**
- ✅ JDBC (PostgreSQL, MySQL, etc)
- ✅ Redis (Jedis, Lettuce)
- ✅ Kafka
- ✅ gRPC
- ✅ Hibernate/JPA
- ✅ Elasticsearch
- ✅ 100+ bibliotecas

**Vantagens:**
- Zero configuração no código
- Instrumenta bibliotecas de terceiros
- Atualização sem recompilação

**Desvantagens:**
- Overhead de startup
- Menos controle granular
- Pode gerar muitos spans

---

## 📈 Estatísticas do Projeto

### Spans Criados por Request

#### Request Simples (GET /orders/{id})
```
Total: 2 spans
├─ 1 span AUTO (SERVER - HTTP Request)
└─ 1 span AUTO (CLIENT - MongoDB)
```

#### Request com RabbitMQ
```
Total: 5+ spans
├─ 1 span AUTO (SERVER - HTTP Request)
├─ 1 span @Traced (INTERNAL - create order)
├─ 1 span AUTO (CLIENT - MongoDB)
├─ 1 span AUTO (PRODUCER - RabbitMQ)
└─ 1+ span AUTO (CONSUMER - RabbitMQ)
   └─ 1+ span @Traced (INTERNAL - handle event)
```

#### Request com API Externa
```
Total: 4 spans
├─ 1 span @Traced (SERVER - controller)
└─ 1 span @Traced (INTERNAL - service)
   ├─ 1 span AUTO (CLIENT - Feign /posts)
   └─ 1 span AUTO (CLIENT - Feign /users)
```

### Linhas de Código de Tracing

| Componente | Manual (hipotético) | Atual | Redução |
|------------|---------------------|-------|---------|
| Controllers | ~200 linhas | 0 | 100% ✅ |
| Feign Client | ~150 linhas | 0 | 100% ✅ |
| RabbitMQ | ~300 linhas | 0 | 100% ✅ |
| MongoDB | ~100 linhas | 0 | 100% ✅ |
| Services | ~400 linhas | ~50 (@Traced) | 87.5% ✅ |
| **TOTAL** | **~1150 linhas** | **~50 linhas** | **95.7%** ✅ |

---

## 🎓 Conclusão

### O que você TEM:

✅ **Auto-instrumentação Spring Boot**
- HTTP, Feign, RabbitMQ, MongoDB
- Propagação automática de contexto
- Zero configuração manual

✅ **AOP Custom (@Traced)**
- Spans para lógica de negócio
- Controle granular
- Atributos customizados

✅ **Context Propagation**
- W3C Trace Context
- Trace único ponta a ponta
- Headers automáticos

### O que você pode ADICIONAR:

🔵 **Java Agent** (quando precisar de mais cobertura)
- JDBC, Redis, Kafka, etc
- 100+ bibliotecas
- Zero código

### Recomendação:

🎯 **Continue com Spring Boot Starter** (atual)
- Suficiente para 90% dos casos
- Menos overhead que Java Agent
- Mais controle que Java Agent
- Adicione Java Agent só se precisar de JDBC/Redis/Kafka

---

## 📚 Documentação Relacionada

- [AUTO_INSTRUMENTATION.md](AUTO_INSTRUMENTATION.md) - Guia completo de auto-instrumentação
- [README.md](README.md) - Documentação geral do projeto
- [api-tests.http](api-tests.http) - Exemplos de requisições

---

**🎉 Você tem 95.7% menos código de tracing graças à auto-instrumentação!** 🚀
