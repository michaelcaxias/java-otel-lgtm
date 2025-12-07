# 🔗 Span Links - Conectando Traces Relacionados

Este documento explica a implementação de **Span Links** no OpenTelemetry para conectar toda a jornada de um pedido através de traces separados.

## 📋 Índice

- [O que são Span Links?](#o-que-são-span-links)
- [Arquitetura](#arquitetura)
- [Implementação](#implementação)
- [Fluxo Completo](#fluxo-completo)
- [Visualização no Grafana](#visualização-no-grafana)
- [Como Funciona](#como-funciona)

---

## 🎓 O que são Span Links?

**Span Links** são uma feature do OpenTelemetry que permite **conectar traces relacionados** que não têm relação pai-filho direta.

### Diferença entre Parent-Child e Links

```
Parent-Child (Hierárquico):
┌─────────────────────────┐
│ HTTP Request (Trace A)  │
│  └─ Service Method      │  ← Mesmo trace
│     └─ Database Query   │  ← Mesmo trace
└─────────────────────────┘

Span Links (Relacionados mas separados):
┌─────────────────────────┐
│ Producer (Trace A)      │
│  └─ publish-event       │ ← [captura traceId/spanId]
└─────────────────────────┘
         │
         │ (RabbitMQ)
         ▼
┌─────────────────────────┐
│ Consumer (Trace B)      │ ← Novo trace
│  🔗 LINK → Trace A      │ ← Link para o producer!
└─────────────────────────┘
```

### Quando usar Span Links?

✅ **Use Span Links quando:**
- Mensagens assíncronas (RabbitMQ, Kafka, SQS)
- Processamento em batch
- Eventos distribuídos
- Workflows de longa duração

❌ **NÃO use Span Links para:**
- Chamadas síncronas (use parent-child)
- Mesmo processo/thread (use parent-child)
- Mesma transação (use parent-child)

---

## 🏗️ Arquitetura

### Componentes Implementados

```
┌─────────────────────────────────────────────────────────┐
│                    OrderEvent (DTO)                      │
│  + traceId: String                                       │
│  + spanId: String                                        │
│  + traceFlags: String                                    │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│                 MessagePublisher                         │
│  - Captura SpanContext do producer                       │
│  - Adiciona traceId/spanId ao OrderEvent                 │
└───────────────────────┬─────────────────────────────────┘
                        │
                        │ (RabbitMQ Message)
                        ▼
┌─────────────────────────────────────────────────────────┐
│                  TracingAspect                           │
│  - Detecta OrderEvent nos parâmetros                     │
│  - Cria span com link usando SpanLinkHelper              │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────┐
│                 SpanLinkHelper                           │
│  - Cria SpanContext a partir de traceId/spanId           │
│  - Adiciona link ao novo span                            │
└─────────────────────────────────────────────────────────┘
```

---

## 🛠️ Implementação

### 1. OrderEvent com Contexto de Tracing

```java
@Data
@Builder
public class OrderEvent implements Serializable {
    private String orderId;
    private String customerId;
    // ... outros campos ...

    // ✅ Campos para Span Link
    private String traceId;      // TraceId do producer
    private String spanId;       // SpanId do producer
    private String traceFlags;   // Flags (sampling, etc)
}
```

### 2. MessagePublisher - Captura Contexto

```java
@Traced(value = "publish-order-event", kind = SpanKind.PRODUCER)
public void publishOrderEvent(OrderEvent event) {
    Span span = Span.current();
    SpanContext spanContext = span.getSpanContext();

    // ✅ Capturar contexto para criar link no consumer
    event.setTraceId(spanContext.getTraceId());
    event.setSpanId(spanContext.getSpanId());
    event.setTraceFlags(spanContext.getTraceFlags().asHex());

    // Publicar mensagem com contexto
    rabbitTemplate.convertAndSend(EXCHANGE, KEY, event);
}
```

### 3. SpanLinkHelper - Utilitário

```java
@UtilityClass
public class SpanLinkHelper {

    public static Span createSpanWithLink(
            Tracer tracer,
            String spanName,
            SpanKind spanKind,
            String linkedTraceId,
            String linkedSpanId,
            String linkedTraceFlags) {

        SpanContext linkedContext = createSpanContext(
            linkedTraceId, linkedSpanId, linkedTraceFlags
        );

        return tracer.spanBuilder(spanName)
                .setSpanKind(spanKind)
                .setParent(Context.current())
                .addLink(linkedContext)  // ← SPAN LINK!
                .startSpan();
    }
}
```

### 4. TracingAspect - Detecção Automática

```java
@Aspect
public class TracingAspect {

    private Span createSpanWithLinkIfApplicable(
            String spanName, Traced traced, Object[] args) {

        // Procurar OrderEvent nos argumentos
        for (Object arg : args) {
            if (arg instanceof OrderEvent event) {
                if (event.getTraceId() != null) {
                    // ✅ Criar span com link!
                    return SpanLinkHelper.createSpanWithLink(
                        tracer, spanName, traced.kind(),
                        event.getTraceId(),
                        event.getSpanId(),
                        event.getTraceFlags()
                    );
                }
            }
        }

        // Span normal sem link
        return tracer.spanBuilder(spanName)
                .setSpanKind(traced.kind())
                .startSpan();
    }
}
```

---

## 🔄 Fluxo Completo

### Exemplo: Criação de Pedido

#### 1️⃣ HTTP Request chega no Controller

```java
POST /api/orders
{
  "customerId": "CUST-123",
  "items": [...]
}
```

**Trace A criado:**
```
TraceId: abc123...
SpanId: xyz789...
```

#### 2️⃣ OrderService cria o pedido

```java
@Traced("create-order")
public Order createOrder(...) {
    // Span dentro de Trace A
    // ...
    publishOrderEvent(order, ORDER_CREATED);
}
```

#### 3️⃣ MessagePublisher captura contexto

```java
@Traced("publish-order-event", kind = PRODUCER)
public void publishOrderEvent(OrderEvent event) {
    SpanContext ctx = Span.current().getSpanContext();

    event.setTraceId(ctx.getTraceId());    // ← abc123...
    event.setSpanId(ctx.getSpanId());      // ← xyz789...
    event.setTraceFlags(ctx.getTraceFlags().asHex());

    rabbitTemplate.send(event);  // ← Mensagem com contexto!
}
```

#### 4️⃣ Consumer recebe mensagem

```java
@RabbitListener(queues = ORDER_QUEUE)
@Traced("handle-order-created", kind = CONSUMER)
public void handleOrderCreated(OrderEvent event) {
    // TracingAspect detecta OrderEvent automaticamente
    // Cria Trace B com LINK para Trace A!

    // Trace B:
    //   TraceId: def456... (novo!)
    //   Link → TraceId: abc123..., SpanId: xyz789...
}
```

---

## 📊 Visualização no Grafana

### Trace do Producer (Trace A)

```
Trace ID: abc123456789...
├─ create-order-endpoint (SERVER) [200ms]
│  └─ create-order (INTERNAL) [180ms]
│     ├─ save-order (DB) [50ms]
│     └─ publish-order-event (PRODUCER) [30ms] ← SpanId: xyz789...
```

### Trace do Consumer (Trace B)

```
Trace ID: def456789012...
└─ handle-order-created (CONSUMER) [150ms]
   ├─ 🔗 LINK → Trace abc123..., Span xyz789...  ← VISÍVEL!
   ├─ process-notification (INTERNAL) [100ms]
   └─ send-email (PRODUCER) [50ms]
```

### Navegação no Grafana

No Grafana Tempo, você pode:

1. **Clicar no link** para pular entre traces relacionados
2. **Ver contexto completo** de toda a jornada
3. **Identificar latências** entre producer e consumer
4. **Debugar problemas** em toda a cadeia

---

## 🎯 Como Funciona

### Passo a Passo Técnico

```
1. Producer cria span
   ├─ Span gerado com TraceId e SpanId
   └─ SpanContext capturado

2. Contexto adicionado à mensagem
   ├─ event.traceId = spanContext.getTraceId()
   ├─ event.spanId = spanContext.getSpanId()
   └─ event.traceFlags = spanContext.getTraceFlags()

3. Mensagem enviada ao broker
   └─ RabbitMQ transporta JSON com contexto

4. Consumer recebe mensagem
   ├─ TracingAspect detecta OrderEvent
   ├─ SpanLinkHelper cria SpanContext
   │  └─ SpanContext.createFromRemoteParent(...)
   └─ Novo span criado com link
      └─ spanBuilder.addLink(linkedContext)

5. Grafana exibe conexão
   └─ Link visível entre os dois traces!
```

### Estrutura do JSON no RabbitMQ

```json
{
  "orderId": "ORDER-123",
  "customerId": "CUST-456",
  "eventType": "ORDER_CREATED",
  "timestamp": "2025-12-07T15:30:00",
  "traceId": "abc123456789...",      ← Contexto do producer
  "spanId": "xyz789012345...",        ← Contexto do producer
  "traceFlags": "01"                  ← Sampling enabled
}
```

---

## ✅ Benefícios

### 1. **Rastreabilidade Completa**

```
Antes (sem links):
❌ Trace A: HTTP → Service → Publish
❌ Trace B: Consumer → Process
   (Não conectados - difícil correlacionar)

Depois (com links):
✅ Trace A: HTTP → Service → Publish
✅ Trace B: Consumer → Process
   🔗 LINK conecta os dois!
```

### 2. **Debugging Facilitado**

- Identificar **exatamente** qual producer causou processamento
- Ver **latência total** entre produção e consumo
- Rastrear **falhas** através de toda a cadeia

### 3. **Métricas Precisas**

- Tempo entre publish e consume
- Taxa de sucesso producer → consumer
- Correlação de erros entre traces

### 4. **Observabilidade Nativa**

- Funciona automaticamente com `@Traced`
- Nenhuma configuração manual necessária
- Compatível 100% com OpenTelemetry

---

## 📈 Exemplo Real

### Cenário: Pedido com Múltiplos Eventos

```
1. HTTP POST /api/orders
   └─ Trace A (create-order)
      └─ publish ORDER_CREATED
         [traceId: AAA, spanId: 111]

2. Consumer ORDER_CREATED
   └─ Trace B 🔗 LINK → (AAA, 111)
      └─ send confirmation email

3. Update status to PAYMENT_PROCESSING
   └─ Trace C (update-status)
      └─ publish PAYMENT_PROCESSING
         [traceId: CCC, spanId: 333]

4. Consumer PAYMENT_PROCESSING
   └─ Trace D 🔗 LINK → (CCC, 333)
      └─ process payment

5. Update status to SHIPPED
   └─ Trace E (update-status)
      └─ publish ORDER_SHIPPED
         [traceId: EEE, spanId: 555]

6. Consumer ORDER_SHIPPED
   └─ Trace F 🔗 LINK → (EEE, 555)
      └─ generate shipping label
```

**Resultado**: Toda a jornada do pedido é rastreável através de links! 🎉

---

## 🔧 Configuração Necessária

### Dependências (já incluídas)

```gradle
implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
implementation 'org.springframework:spring-aop'
implementation 'org.aspectj:aspectjweaver'
```

### Configuração (já feita)

```yaml
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

## 🎓 Conclusão

Com **Span Links**, você tem **observabilidade completa** de ponta a ponta:

- ✅ Rastreamento através de boundaries assíncronos
- ✅ Conexões visíveis no Grafana
- ✅ Zero configuração manual (automático via `@Traced`)
- ✅ Compatível com OpenTelemetry padrão

**Resultado**: Debugging e observabilidade em nível enterprise! 🚀

---

## 📚 Referências

- [OpenTelemetry Span Links](https://opentelemetry.io/docs/concepts/signals/traces/#span-links)
- [W3C Trace Context](https://www.w3.org/TR/trace-context/)
- [Distributed Tracing Best Practices](https://opentelemetry.io/docs/concepts/signals/traces/)
