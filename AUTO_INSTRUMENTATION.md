# 🤖 Auto-Instrumentação OpenTelemetry

Este documento explica como funciona a **auto-instrumentação** do OpenTelemetry neste projeto.

## 📋 Índice

- [O que é Auto-Instrumentação?](#o-que-é-auto-instrumentação)
- [Configuração no Projeto](#configuração-no-projeto)
- [O que é Auto-Instrumentado](#o-que-é-auto-instrumentado)
- [Demonstração com API Externa](#demonstração-com-api-externa)
- [Visualização no Grafana](#visualização-no-grafana)
- [Comparação: Manual vs Auto](#comparação-manual-vs-auto)

---

## 🎓 O que é Auto-Instrumentação?

**Auto-instrumentação** significa que o OpenTelemetry **cria spans automaticamente** para certas operações, **sem você escrever código de tracing**.

### Dois Níveis de Auto-Instrumentação

#### 1️⃣ **Spring Boot Starter (Este Projeto)** ✅
```gradle
implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
```

**O que instrumenta automaticamente:**
- ✅ **HTTP Requests** (Spring MVC Controllers)
- ✅ **HTTP Clients** (RestTemplate, WebClient, **Feign Client**)
- ✅ **RabbitMQ** (Producer e Consumer)
- ✅ **MongoDB** (Database queries)
- ✅ **Propagação de Contexto** (W3C Trace Context)

**Limitações:**
- ❌ NÃO instrumenta JDBC genérico
- ❌ NÃO instrumenta Redis
- ❌ NÃO instrumenta Kafka (precisa de dependência extra)
- ❌ Menos bibliotecas que o Java Agent

#### 2️⃣ **Java Agent (Mais Completo)** 🚀
```bash
java -javaagent:opentelemetry-javaagent.jar -jar app.jar
```

**O que instrumenta automaticamente:**
- ✅ Tudo do Spring Boot Starter +
- ✅ **JDBC** (PostgreSQL, MySQL, etc)
- ✅ **Redis** (Jedis, Lettuce)
- ✅ **Kafka**
- ✅ **gRPC**
- ✅ **Hibernate/JPA**
- ✅ **100+ bibliotecas**

---

## ⚙️ Configuração no Projeto

### Dependências (build.gradle)

```gradle
dependencies {
    // ✅ Habilita auto-instrumentação Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'

    // ✅ Exportação OTLP (traces + métricas)
    implementation 'io.opentelemetry:opentelemetry-exporter-otlp'

    // ✅ Feign Client (auto-instrumentado!)
    implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'

    // ✅ RabbitMQ (auto-instrumentado!)
    implementation 'org.springframework.boot:spring-boot-starter-amqp'

    // ✅ MongoDB (auto-instrumentado!)
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
}
```

### Configuração (application.yml)

```yaml
# OpenTelemetry Configuration
management:
  tracing:
    enabled: true          # ✅ Habilita tracing
    sampling:
      probability: 1.0     # ✅ 100% de amostragem (dev)

  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
    metrics:
      endpoint: http://localhost:4318/v1/metrics

# Resource attributes (identificação do serviço)
otel:
  service:
    name: java-otel-lgtm
  resource:
    attributes:
      service.name: java-otel-lgtm
      deployment.environment: development
```

### Habilitação Feign Client

```java
@SpringBootApplication
@EnableFeignClients  // ✅ Habilita Feign (auto-instrumentado)
public class JavaOtelLgtmApplication {
    public static void main(String[] args) {
        SpringApplication.run(JavaOtelLgtmApplication.class, args);
    }
}
```

---

## 🔍 O que é Auto-Instrumentado

### 1. **HTTP Requests (Controllers)** ✅

**Você escreve:**
```java
@GetMapping("/posts/{id}")
public ResponseEntity<Post> getPost(@PathVariable Long id) {
    // seu código
}
```

**OpenTelemetry cria automaticamente:**
- ✅ Span com `SpanKind.SERVER`
- ✅ Atributo `http.method = GET`
- ✅ Atributo `http.route = /posts/{id}`
- ✅ Atributo `http.status_code = 200`
- ✅ Atributo `http.target = /posts/1`

### 2. **Feign Client (HTTP Externo)** ✅

**Você escreve:**
```java
@FeignClient(name = "jsonplaceholder", url = "https://jsonplaceholder.typicode.com")
public interface JsonPlaceholderClient {

    @GetMapping("/posts/{id}")
    JsonPlaceholderPost getPostById(@PathVariable("id") Long id);
}
```

**OpenTelemetry cria automaticamente:**
- ✅ Span com `SpanKind.CLIENT`
- ✅ Atributo `http.method = GET`
- ✅ Atributo `http.url = https://jsonplaceholder.typicode.com/posts/1`
- ✅ Atributo `http.status_code = 200`
- ✅ **Headers W3C** (`traceparent`, `tracestate`) enviados automaticamente
- ✅ **Contexto propagado** para API externa!

### 3. **RabbitMQ Producer** ✅

**Você escreve:**
```java
rabbitTemplate.convertAndSend(EXCHANGE, KEY, event);
```

**OpenTelemetry cria automaticamente:**
- ✅ Span com `SpanKind.PRODUCER`
- ✅ Atributo `messaging.system = rabbitmq`
- ✅ Atributo `messaging.destination = order.exchange`
- ✅ Atributo `messaging.routing_key = order.created`
- ✅ **Headers W3C** injetados na mensagem RabbitMQ

### 4. **RabbitMQ Consumer** ✅

**Você escreve:**
```java
@RabbitListener(queues = "order.queue")
public void handleOrder(OrderEvent event) {
    // seu código
}
```

**OpenTelemetry cria automaticamente:**
- ✅ Span com `SpanKind.CONSUMER`
- ✅ Atributo `messaging.system = rabbitmq`
- ✅ Atributo `messaging.source = order.queue`
- ✅ **Contexto extraído** dos headers da mensagem
- ✅ **Span como filho** do producer!

### 5. **MongoDB** ✅

**Você escreve:**
```java
orderRepository.findById(orderId);
```

**OpenTelemetry cria automaticamente:**
- ✅ Span com `SpanKind.CLIENT`
- ✅ Atributo `db.system = mongodb`
- ✅ Atributo `db.operation = findById`
- ✅ Atributo `db.name = orders_db`

---

## 🌐 Demonstração com API Externa

### Implementação

Este projeto demonstra auto-instrumentação com a **JSONPlaceholder API** (API pública gratuita).

#### Feign Client

```java
@FeignClient(name = "jsonplaceholder", url = "https://jsonplaceholder.typicode.com")
public interface JsonPlaceholderClient {

    @GetMapping("/posts/{id}")
    JsonPlaceholderPost getPostById(@PathVariable("id") Long id);

    @GetMapping("/users/{id}")
    JsonPlaceholderUser getUserById(@PathVariable("id") Long id);
}
```

**✨ Zero código de tracing! Tudo auto-instrumentado!**

#### Service com Múltiplas Chamadas

```java
@Service
@RequiredArgsConstructor
public class ExternalApiService {

    private final JsonPlaceholderClient jsonPlaceholderClient;

    @Traced(value = "get-post-with-author", kind = SpanKind.INTERNAL)
    public EnrichedPost getPostWithAuthor(Long postId) {
        // ✨ Span CLIENT criado automaticamente!
        JsonPlaceholderPost post = jsonPlaceholderClient.getPostById(postId);

        // ✨ Outro span CLIENT criado automaticamente!
        JsonPlaceholderUser user = jsonPlaceholderClient.getUserById(post.getUserId());

        return new EnrichedPost(post, user);
    }
}
```

#### Controller

```java
@RestController
@RequestMapping("/api/external")
@RequiredArgsConstructor
public class ExternalApiController {

    private final ExternalApiService externalApiService;

    @GetMapping("/posts/{id}/enriched")
    @Traced(value = "get-enriched-post-endpoint", kind = SpanKind.SERVER)
    public ResponseEntity<EnrichedPost> getEnrichedPost(@PathVariable Long id) {
        return ResponseEntity.ok(externalApiService.getPostWithAuthor(id));
    }
}
```

### Teste

```bash
# Teste a auto-instrumentação
curl http://localhost:8080/api/external/posts/1/enriched
```

---

## 📊 Visualização no Grafana

### Trace Completo (1 Requisição = 4 Spans)

```
GET /api/external/posts/1/enriched

Trace ID: 4bf92f3577b34da6a3ce929d0e0e4736
├─ [SERVER] get-enriched-post-endpoint (controller)      ← @Traced (nosso AOP)
│  └─ [INTERNAL] get-post-with-author (service)          ← @Traced (nosso AOP)
│     ├─ [CLIENT] GET https://.../posts/1                ← Auto-instrumentado!
│     └─ [CLIENT] GET https://.../users/1                ← Auto-instrumentado!
```

### Atributos dos Spans AUTO-INSTRUMENTADOS

#### Span: GET /posts/1 (CLIENT)
```yaml
span.kind: CLIENT
http.method: GET
http.url: https://jsonplaceholder.typicode.com/posts/1
http.status_code: 200
http.response_content_length: 292
net.peer.name: jsonplaceholder.typicode.com
net.peer.port: 443
```

#### Span: GET /users/1 (CLIENT)
```yaml
span.kind: CLIENT
http.method: GET
http.url: https://jsonplaceholder.typicode.com/users/1
http.status_code: 200
http.response_content_length: 509
net.peer.name: jsonplaceholder.typicode.com
net.peer.port: 443
```

### Query no Grafana Tempo

```
# Buscar traces com chamadas externas
{span.kind="client"} && {http.url=~"jsonplaceholder.*"}

# Buscar traces do endpoint específico
{name="get-enriched-post-endpoint"}
```

---

## ⚖️ Comparação: Manual vs Auto

### ❌ SEM Auto-Instrumentação (Manual)

```java
// 😰 Muito código manual!
@FeignClient(name = "jsonplaceholder", url = "https://jsonplaceholder.typicode.com")
public interface JsonPlaceholderClient {

    @GetMapping("/posts/{id}")
    default JsonPlaceholderPost getPostById(Long id) {
        Span span = tracer.spanBuilder("GET /posts/" + id)
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.url", "https://jsonplaceholder.typicode.com/posts/" + id);

            JsonPlaceholderPost result = this.getPostByIdInternal(id);

            span.setAttribute("http.status_code", 200);
            span.setStatus(StatusCode.OK);
            return result;

        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            span.end();
        }
    }

    JsonPlaceholderPost getPostByIdInternal(Long id);
}
```

**Problemas:**
- 😰 20+ linhas de código de tracing
- 😰 Difícil manutenção
- 😰 Repetitivo para cada método
- 😰 Propagação de contexto manual

### ✅ COM Auto-Instrumentação

```java
// 🎉 Zero código de tracing!
@FeignClient(name = "jsonplaceholder", url = "https://jsonplaceholder.typicode.com")
public interface JsonPlaceholderClient {

    @GetMapping("/posts/{id}")
    JsonPlaceholderPost getPostById(@PathVariable("id") Long id);
}
```

**Vantagens:**
- ✅ **1 linha** vs 20+ linhas
- ✅ **Zero manutenção** de código de tracing
- ✅ **Automático** para todos os métodos
- ✅ **Propagação de contexto** automática (W3C headers)
- ✅ **Atributos padrão** (http.method, http.url, etc)

---

## 📚 Resumo

### O que VOCÊ precisa fazer:

```java
// 1. Adicionar dependência
implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'

// 2. Criar Feign Client
@FeignClient(name = "api", url = "https://api.com")
public interface ApiClient {
    @GetMapping("/endpoint")
    Data getData();
}

// 3. Usar!
apiClient.getData();  // ✨ Spans criados automaticamente!
```

### O que OpenTelemetry faz AUTOMATICAMENTE:

- ✅ Cria span CLIENT
- ✅ Adiciona atributos HTTP
- ✅ Injeta headers W3C (traceparent)
- ✅ Propaga contexto
- ✅ Captura exceções
- ✅ Define status do span
- ✅ Finaliza span

### Benefícios:

| Métrica | Manual | Auto-Instrumentação |
|---------|--------|---------------------|
| **Código de tracing** | 20+ linhas/método | 0 linhas ✅ |
| **Manutenção** | Alta | Zero ✅ |
| **Cobertura** | Parcial | 100% ✅ |
| **Atributos** | Manual | Padrão ✅ |
| **Propagação** | Manual | Automática ✅ |
| **Erros** | Fácil esquecer | Impossível ✅ |

---

## 🎯 Próximos Passos

### Para mais auto-instrumentação:

1. **Adicionar Java Agent** (mais completo):
   ```bash
   java -javaagent:opentelemetry-javaagent.jar \
        -Dotel.service.name=java-otel-lgtm \
        -Dotel.traces.exporter=otlp \
        -Dotel.exporter.otlp.endpoint=http://localhost:4318 \
        -jar app.jar
   ```

2. **Instrumentar JDBC**:
   ```gradle
   // Adicionar se usar JDBC diretamente
   implementation 'io.opentelemetry.instrumentation:opentelemetry-jdbc'
   ```

3. **Instrumentar Redis**:
   ```gradle
   // Adicionar se usar Redis
   implementation 'io.opentelemetry.instrumentation:opentelemetry-lettuce-5.1'
   ```

---

## 🔗 Links Úteis

- [Spring Boot OpenTelemetry](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.micrometer-tracing)
- [OpenTelemetry Java Agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [Supported Libraries](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md)
- [JSONPlaceholder API](https://jsonplaceholder.typicode.com)

---

**✨ Auto-instrumentação = Menos código, mais observabilidade!** 🚀
