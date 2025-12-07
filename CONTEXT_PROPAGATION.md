# 🔄 Context Propagation - Trace Único Ponta a Ponta

Este documento explica como a **propagação automática de contexto** cria **UM único trace** através de toda a jornada do pedido, incluindo processamento assíncrono via RabbitMQ.

## 📋 Índice

- [O que é Context Propagation?](#o-que-é-context-propagation)
- [Como Funciona](#como-funciona)
- [Trace Único vs Span Links](#trace-único-vs-span-links)
- [Implementação](#implementação)
- [Visualização no Grafana](#visualização-no-grafana)
- [W3C Trace Context](#w3c-trace-context)

---

## 🎓 O que é Context Propagation?

**Context Propagation** é o mecanismo que permite que o **mesmo trace** continue através de diferentes processos, threads e até serviços diferentes.

### Trace Único Através do RabbitMQ

```
┌─────────────────────────────────────────────────────────────┐
│                  TRACE ÚNICO (TraceId: AAA)                  │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  HTTP Request                                                │
│    ├─ create-order-endpoint (SERVER)                        │
│    │  └─ create-order (INTERNAL)                            │
│    │     ├─ save-order (DB)                                 │
│    │     └─ publish-order-event (PRODUCER)                  │
│    │                   │                                      │
│    │                   │ (RabbitMQ Message)                  │
│    │                   │ Headers: traceparent=AAA-xxx        │
│    │                   ▼                                      │
│    └─ handle-order-created (CONSUMER) ← MESMO TRACE!        │
│       └─ send-email (PRODUCER)                              │
│                   │                                          │
│                   │ (RabbitMQ Message)                       │
│                   │ Headers: traceparent=AAA-yyy             │
│                   ▼                                          │
│       └─ handle-notification (CONSUMER) ← MESMO TRACE!      │
│                                                               │
└─────────────────────────────────────────────────────────────┘

✅ Tudo em UM único trace!
✅ Hierarquia completa visível
✅ Latência total calculada automaticamente
```

---

## 🔄 Como Funciona

### 1. **Producer: Injeção de Contexto**

Quando você publica uma mensagem no RabbitMQ:

```java
@Traced("publish-order-event", kind = PRODUCER)
public void publishOrderEvent(OrderEvent event) {
    // Spring Boot OpenTelemetry injeta headers AUTOMATICAMENTE
    rabbitTemplate.convertAndSend(EXCHANGE, KEY, event);
}
```

**O que acontece por trás dos panos:**

```
RabbitMQ Message:
├─ Headers:
│  ├─ traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
│  │                   └─ TraceId: 4bf92f3577b34da6a3ce929d0e0e4736
│  │                   └─ SpanId: 00f067aa0ba902b7
│  │                   └─ Flags: 01 (sampled)
│  └─ tracestate: (vendor-specific data)
└─ Body: {"orderId": "...", "customerId": "...", ...}
```

### 2. **Consumer: Extração de Contexto**

Quando o consumer recebe a mensagem:

```java
@RabbitListener(queues = ORDER_QUEUE)
@Traced("handle-order-created", kind = CONSUMER)
public void handleOrderCreated(OrderEvent event) {
    // Spring Boot OpenTelemetry extrai contexto AUTOMATICAMENTE
    // Span criado como FILHO do span do producer
}
```

**O que acontece:**

```
1. Spring Boot OpenTelemetry lê header "traceparent"
2. Restaura o contexto do trace original (TraceId: AAA)
3. Cria novo span como FILHO do span do producer
4. Continua o MESMO trace!
```

---

## 🆚 Trace Único vs Span Links

### Span Links (Anterior)

```
❌ DOIS traces separados:

Trace A (HTTP):          Trace B (Consumer):
├─ create-order          └─ handle-order-created
   └─ publish-event         🔗 LINK → Trace A

- Precisa clicar no link para navegar
- Latência total não é calculada
- Visualização fragmentada
```

### Context Propagation (Atual) ✅

```
✅ UM único trace:

Trace A (HTTP + RabbitMQ):
├─ create-order-endpoint
│  └─ create-order
│     ├─ save-order
│     └─ publish-order-event
└─ handle-order-created  ← Continua o mesmo trace!
   └─ send-email
      └─ handle-notification  ← Ainda o mesmo trace!

- Hierarquia completa em uma visualização
- Latência total automática
- Muito mais fácil de debugar
```

---

## 🛠️ Implementação

### Configuração (Automática!)

O Spring Boot OpenTelemetry faz **TUDO automaticamente**:

```java
@Configuration
public class RabbitMQConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);

        // ✅ Context propagation é AUTOMÁTICO!
        // Spring Boot OpenTelemetry injeta interceptors que:
        // 1. Adicionam headers W3C Trace Context ao publicar
        // 2. Extraem headers ao consumir
        // 3. Mantêm o mesmo TraceId

        return template;
    }
}
```

### Producer (Zero Configuração)

```java
@Traced("publish-order-event", kind = SpanKind.PRODUCER)
public void publishOrderEvent(OrderEvent event) {
    Span span = Span.current();

    span.setAttribute("order.id", event.getOrderId());
    span.setAttribute("event.type", event.getEventType().name());

    // ✅ Contexto propagado AUTOMATICAMENTE nos headers!
    rabbitTemplate.convertAndSend(EXCHANGE, KEY, event);
}
```

### Consumer (Zero Configuração)

```java
@RabbitListener(queues = ORDER_QUEUE)
@Traced("handle-order-created", kind = SpanKind.CONSUMER)
public void handleOrderCreated(OrderEvent event) {
    // ✅ Contexto extraído AUTOMATICAMENTE dos headers!
    // Span criado como FILHO do producer

    Span span = Span.current();
    span.addEvent("Processing order event");

    // Processar pedido...
}
```

---

## 📊 Visualização no Grafana

### Trace Completo

```
TraceId: 4bf92f3577b34da6a3ce929d0e0e4736
Duration: 2.5s

┌─ create-order-endpoint (SERVER) [150ms]
│  ├─ create-order (INTERNAL) [120ms]
│  │  ├─ calculate-items (INTERNAL) [20ms]
│  │  ├─ save-to-db (CLIENT) [50ms]
│  │  └─ publish-order-event (PRODUCER) [30ms]
│  │
│  └─ handle-order-created (CONSUMER) [2.2s]  ← Mesmo trace!
│     ├─ validate-order (INTERNAL) [100ms]
│     ├─ process-payment (CLIENT) [1.8s]
│     └─ send-notification (PRODUCER) [200ms]
│        │
│        └─ handle-notification (CONSUMER) [150ms]  ← Ainda mesmo trace!
│           └─ send-email (CLIENT) [100ms]

✅ Latência total: 2.5s (automática!)
✅ Caminho crítico identificado: process-payment (1.8s)
✅ Hierarquia completa visível
```

### Filtros Úteis no Grafana

```
# Ver todos os spans de um pedido específico
{resource.service.name="java-otel-lgtm"} | order.id="ORDER-123"

# Ver apenas producers e consumers
{resource.service.name="java-otel-lgtm" && span.kind=~"PRODUCER|CONSUMER"}

# Ver latências maiores que 1s
{resource.service.name="java-otel-lgtm"} | duration > 1s
```

---

## 🌐 W3C Trace Context

A propagação usa o padrão **W3C Trace Context**, garantindo compatibilidade universal.

### Header `traceparent`

Formato: `version-trace-id-parent-id-trace-flags`

```
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
             │  │                                │                  │
             │  └─ TraceId (32 hex chars)       │                  └─ Flags (01 = sampled)
             │                                   └─ SpanId (16 hex chars)
             └─ Version (00)
```

### Header `tracestate`

Opcional, para dados vendor-specific:

```
tracestate: vendor1=value1,vendor2=value2
```

---

## ✅ Benefícios

### 1. **Debugging Simplificado**

**Antes (Span Links):**
```
1. Ver trace do HTTP request
2. Clicar no link para trace do consumer
3. Clicar em outro link para próximo consumer
4. Correlacionar manualmente timestamps
```

**Depois (Context Propagation):**
```
1. Ver UM único trace completo
2. Tudo em uma visualização hierárquica
3. Latências calculadas automaticamente
```

### 2. **Rastreabilidade Completa**

```
POST /api/orders
  └─ OrderController.createOrder()
     └─ OrderService.createOrder()
        └─ MessagePublisher.publishOrderEvent()
           └─ OrderEventConsumer.handleOrderCreated()
              └─ MessagePublisher.publishNotification()
                 └─ OrderEventConsumer.handleNotification()

✅ Caminho completo em UM trace!
```

### 3. **Métricas Precisas**

- **Latência total**: Tempo do HTTP request até último consumer
- **Latência de fila**: Tempo entre publish e consume
- **Latência de processamento**: Tempo dentro do consumer
- **Gargalos**: Identificação automática do caminho crítico

### 4. **Zero Configuração**

- ✅ Nenhum código manual necessário
- ✅ Funciona automaticamente com `@Traced`
- ✅ Compatível com qualquer sistema que use W3C Trace Context

---

## 🔧 Requisitos

### Dependências (já incluídas)

```gradle
implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
implementation 'org.springframework.boot:spring-boot-starter-amqp'
```

### Configuração (application.yml)

```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # 100% sampling
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
```

---

## 🎯 Fluxo Completo - Exemplo Real

### Cenário: Criar Pedido e Enviar Email

```
1. Usuario faz POST /api/orders
   └─ TraceId: AAA criado

2. OrderController.createOrder()
   └─ Span: create-order-endpoint (TraceId: AAA)

3. OrderService.createOrder()
   └─ Span: create-order (TraceId: AAA, Parent: create-order-endpoint)

4. OrderService salva no MongoDB
   └─ Span: save-order (TraceId: AAA, Parent: create-order)

5. MessagePublisher.publishOrderEvent()
   └─ Span: publish-order-event (TraceId: AAA, Parent: create-order)
   └─ RabbitMQ Message Headers:
      ├─ traceparent: 00-AAA-xxx-01  ← Contexto injetado!
      └─ body: {...}

6. OrderEventConsumer.handleOrderCreated()
   └─ Span: handle-order-created (TraceId: AAA, Parent: publish-order-event) ← MESMO TRACE!

7. MessagePublisher.publishNotification()
   └─ Span: publish-notification (TraceId: AAA, Parent: handle-order-created)
   └─ RabbitMQ Message Headers:
      ├─ traceparent: 00-AAA-yyy-01  ← Ainda o mesmo TraceId!
      └─ body: {...}

8. OrderEventConsumer.handleNotification()
   └─ Span: handle-notification (TraceId: AAA, Parent: publish-notification) ← AINDA MESMO TRACE!

9. Email enviado
   └─ Span: send-email (TraceId: AAA, Parent: handle-notification)
```

**Resultado**: **UM único trace AAA** do HTTP request até o envio do email! 🎉

---

## 📚 Comparação: Antes vs Depois

### Antes (Span Links)

| Aspecto | Comportamento |
|---------|---------------|
| **Traces** | Múltiplos traces separados |
| **Navegação** | Clicar em links entre traces |
| **Latência Total** | Calculada manualmente |
| **Visualização** | Fragmentada |
| **Código Extra** | Campos traceId/spanId no DTO |
| **Complexidade** | Média (SpanLinkHelper, etc) |

### Depois (Context Propagation)

| Aspecto | Comportamento |
|---------|---------------|
| **Traces** | UM único trace ponta a ponta ✅ |
| **Navegação** | Hierarquia em uma visualização ✅ |
| **Latência Total** | Calculada automaticamente ✅ |
| **Visualização** | Unificada e clara ✅ |
| **Código Extra** | Zero! ✅ |
| **Complexidade** | Mínima (automático) ✅ |

---

## 🎓 Conclusão

Com **Context Propagation automática**, você tem:

- ✅ **UM trace único** do início ao fim
- ✅ **Zero configuração** manual
- ✅ **Visualização hierárquica** completa no Grafana
- ✅ **Latências calculadas** automaticamente
- ✅ **Debugging muito mais fácil**
- ✅ **Compatível com W3C Trace Context** (padrão universal)

**A solução perfeita para observabilidade em sistemas assíncronos!** 🚀

---

## 📚 Referências

- [W3C Trace Context](https://www.w3.org/TR/trace-context/)
- [OpenTelemetry Context Propagation](https://opentelemetry.io/docs/concepts/context-propagation/)
- [Spring Boot OpenTelemetry](https://docs.spring.io/spring-boot/reference/actuator/tracing.html)
- [RabbitMQ Tracing](https://opentelemetry.io/docs/specs/semconv/messaging/messaging-spans/)
