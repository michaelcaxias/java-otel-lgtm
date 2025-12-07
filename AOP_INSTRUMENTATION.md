# 🎯 Instrumentação AOP com Spring Boot OpenTelemetry

## 📋 O que mudou?

A aplicação foi refatorada para usar **anotações AOP (@Observed)** do Spring Boot OpenTelemetry ao invés de instrumentação manual com `Tracer`. Isso traz vários benefícios:

### ✅ Vantagens da Abordagem AOP

1. **Código mais limpo** - Removemos toda a manipulação manual de spans
2. **Menos código boilerplate** - Não é necessário criar/iniciar/finalizar spans manualmente
3. **Tratamento automático de erros** - O AOP automaticamente marca spans com erros quando exceções ocorrem
4. **Melhor separação de concerns** - A lógica de negócio fica separada da instrumentação
5. **Manutenção facilitada** - Mudanças na instrumentação não afetam a lógica de negócio
6. **Padrão Spring** - Segue as melhores práticas do ecossistema Spring

## 🔧 Mudanças Realizadas

### 1. Removido `Tracer` manual

**Antes:**
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final Tracer tracer;

    public Order createOrder(CreateOrderRequest request) {
        var span = tracer.nextSpan().name("create-order");
        try (var ws = tracer.withSpan(span.start())) {
            span.tag("customer.id", request.getCustomerId());
            span.tag("order.total", totalAmount.toString());
            // ... lógica de negócio ...
            return order;
        } finally {
            span.end();
        }
    }
}
```

**Depois:**
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    // Sem Tracer!

    @Observed(
        name = "order.create",
        contextualName = "create-order",
        lowCardinalityKeyValues = {"operation", "create"}
    )
    public Order createOrder(CreateOrderRequest request) {
        // ... apenas lógica de negócio ...
        return order;
    }
}
```

### 2. Anotações @Observed nos Services

#### OrderService
- `@Observed` em `createOrder()` - Criação de pedidos
- `@Observed` em `getOrder()` - Busca de pedido
- `@Observed` em `getAllOrders()` - Listagem de pedidos
- `@Observed` em `getOrdersByCustomerId()` - Listagem por cliente
- `@Observed` em `updateOrderStatus()` - Atualização de status
- `@Observed` em `cancelOrder()` - Cancelamento

#### MessagePublisher
- `@Observed` em `publishOrderEvent()` - Publicação de eventos de pedido
- `@Observed` em `publishNotification()` - Publicação de notificações

#### OrderEventConsumer
- `@Observed` em `handleOrderCreated()` - Consumer de pedidos criados
- `@Observed` em `handlePaymentEvent()` - Consumer de eventos de pagamento
- `@Observed` em `handleShippingEvent()` - Consumer de eventos de envio
- `@Observed` em `handleNotification()` - Consumer de notificações

### 3. Anotações @Observed nos Controllers

#### OrderController
- `@Observed` em todos os endpoints HTTP
- Cada endpoint tem seu próprio span com contexto relevante

#### SimulationController
- `@Observed` em endpoints de simulação
- Útil para diferenciar tráfego de teste

### 4. Configuração AOP

Adicionado no `application.yml`:
```yaml
spring:
  aop:
    proxy-target-class: true
```

Isso habilita proxies CGLIB para permitir AOP em classes concretas.

## 📊 Atributos da Anotação @Observed

### `name`
Nome da métrica/observação. Usado para agrupar observações similares.
```java
@Observed(name = "order.create")
```

### `contextualName`
Nome contextual que aparece no trace. Mais descritivo que o `name`.
```java
@Observed(contextualName = "create-order")
```

### `lowCardinalityKeyValues`
Tags/atributos de baixa cardinalidade (valores limitados). Ideal para agregações.
```java
@Observed(lowCardinalityKeyValues = {"operation", "create", "type", "order"})
```

**Exemplo completo:**
```java
@Observed(
    name = "order.create",                          // Nome da métrica
    contextualName = "create-order",                // Nome no trace
    lowCardinalityKeyValues = {"operation", "create"} // Tags
)
public Order createOrder(CreateOrderRequest request) {
    // ...
}
```

## 🎯 Padrões de Nomenclatura

### Services (Operações de Negócio)
- Pattern: `{entity}.{action}`
- Exemplos:
  - `order.create`
  - `order.update-status`
  - `order.cancel`

### Message Publish
- Pattern: `message.publish.{type}`
- Exemplos:
  - `message.publish.order-event`
  - `message.publish.notification`

### Message Consume
- Pattern: `message.consume.{type}`
- Exemplos:
  - `message.consume.order-created`
  - `message.consume.payment-event`
  - `message.consume.shipping-event`

### HTTP Endpoints
- Pattern: `http.server.requests`
- Context: `{action}-endpoint`
- Exemplos:
  - `create-order-endpoint`
  - `update-order-status-endpoint`

### Simulation
- Pattern: `simulation.{action}`
- Exemplos:
  - `simulation.create-sample-order`
  - `simulation.generate-traffic`

## 🔍 Como os Spans são Criados

### 1. Interceptação AOP
O Spring AOP intercepta chamadas aos métodos anotados com `@Observed`.

### 2. Criação Automática de Span
Um novo span é criado automaticamente com:
- Nome do span = `contextualName`
- Tags = `lowCardinalityKeyValues`
- Timestamp de início

### 3. Execução do Método
O método original é executado normalmente.

### 4. Tratamento de Erros
Se uma exceção é lançada:
- O span é marcado automaticamente com erro
- A exceção é propagada normalmente
- O span é finalizado com status de erro

### 5. Finalização
Após a execução (sucesso ou erro):
- O span é finalizado
- A duração é calculada
- O span é enviado ao backend OpenTelemetry

## 📈 Propagação de Contexto

O contexto de trace é propagado automaticamente através de:

### 1. Chamadas Síncronas
```
Controller → Service → Repository
    |          |          |
  Span A    Span B    Span C (todos conectados)
```

### 2. Mensagens RabbitMQ
```
Publisher → RabbitMQ → Consumer
    |                      |
  Span A    (context)    Span B (filho de A)
```

O contexto é propagado através dos headers da mensagem automaticamente.

### 3. HTTP Requests
Headers como `traceparent` são automaticamente adicionados e extraídos.

## 🎨 Visualização no Grafana

### Trace Completo de um Pedido

```
create-order-endpoint (HTTP)
├── create-order (Service)
│   ├── publish-order-event (Publisher)
│   └── [MongoDB save]
└── [retorno HTTP]

handle-order-created (Consumer)
├── publish-notification (Publisher)
└── [processamento]

handle-notification (Consumer)
└── [envio email]
```

Todos conectados pelo mesmo trace ID!

## 💡 Benefícios Práticos

### 1. Debug Facilitado
```java
// Não é mais necessário:
span.tag("order.id", orderId);
span.tag("status", status);

// O AOP já captura:
// - Nome do método
// - Classe
// - Duração
// - Exceções
```

### 2. Código Mais Testável
```java
// Antes: precisava mockar Tracer
@Mock private Tracer tracer;

// Depois: sem dependências de observabilidade
// Testes mais simples e rápidos
```

### 3. Manutenção Simplificada
- Mudanças na instrumentação não afetam lógica de negócio
- Adicionar observabilidade = adicionar anotação
- Remover observabilidade = remover anotação

## 🔧 Configurações Adicionais

### application.yml
```yaml
spring:
  aop:
    proxy-target-class: true  # Habilita CGLIB proxies

management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # 100% dos traces (dev/test)
                        # Use 0.1 (10%) em produção
```

### Customização de Tags

Se precisar adicionar tags dinâmicas:
```java
@Service
public class OrderService {

    @Observed(name = "order.create")
    public Order createOrder(CreateOrderRequest request) {
        // Use MDC ou CurrentTraceContext para tags dinâmicas
        return order;
    }
}
```

## 📚 Referências

- [Spring Boot Actuator - Micrometer Observation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.observability)
- [Micrometer Observation API](https://micrometer.io/docs/observation)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Spring AOP](https://docs.spring.io/spring-framework/reference/core/aop.html)

## 🎯 Próximos Passos

1. ✅ Instrumentação AOP implementada
2. ⚡ Adicionar métricas customizadas com `@Timed`
3. 📊 Criar dashboards específicos para cada operação
4. 🔔 Configurar alertas baseados em SLOs
5. 📈 Implementar exemplares (exemplars) para correlação métrica→trace
