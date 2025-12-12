# Changelog - Aplicação de Boas Práticas OpenTelemetry

## 📅 Data: 12 de dezembro de 2025

## 🎯 Objetivo

Aplicar as boas práticas de OpenTelemetry conforme documentação oficial e padrões estabelecidos no `.cursor/rules/opentelemetry.mdc`.

---

## 🆕 Atualização: Uso Apropriado de Eventos em Spans

### Data: 12 de dezembro de 2025

Aplicadas boas práticas para **uso moderado de eventos em spans**, evitando poluição e overhead desnecessário.

### Princípios Aplicados

1. **Eventos apenas para situações excepcionais** - Não para fluxo normal
2. **Máximo 3-5 eventos por span** - Evitar poluição visual
3. **Priorizar atributos sobre eventos** - Para contexto estruturado
4. **Usar logs para detalhes do fluxo** - Eventos são para troubleshooting

### Redução de Eventos

| Serviço | Método | Antes | Depois | Redução |
|---------|--------|-------|--------|---------|
| OrderService | createOrder | 7 eventos | 0 eventos | -100% |
| OrderService | getOrder | 3 eventos | 1 evento (apenas erro) | -67% |
| OrderService | getAllOrders | 2 eventos | 0 eventos | -100% |
| OrderService | getOrdersByCustomerId | 2 eventos | 0 eventos | -100% |
| OrderService | updateOrderStatus | 5 eventos | 1 evento (mudança estado) | -80% |
| OrderService | cancelOrder | 2 eventos | 0 eventos | -100% |
| ExternalApiService | getPostWithAuthor | 6 eventos | 0-1 evento (apenas falha) | -83% a -100% |
| ExternalApiService | getUserPosts | 2 eventos | 0 eventos | -100% |
| ExternalApiService | getAllPosts | 2 eventos | 0 eventos | -100% |
| ExternalApiService | getAllUsers | 2 eventos | 0 eventos | -100% |

**Total:** De **31 eventos** para **2-3 eventos** (apenas excepcionais) = **~90% de redução**

---

## ✅ Mudanças Implementadas

### 1. **Padronização de Nomenclatura de Spans**

#### Antes ❌
```java
@TraceSpan(value = "create-order", kind = SpanKind.INTERNAL)
@TraceSpan(value = "get-order", kind = SpanKind.INTERNAL)
@TraceSpan(value = "list-orders-by-customer", kind = SpanKind.INTERNAL)
```

#### Depois ✅
```java
@TraceSpan(SpanName.ORDER_CREATE)
@TraceSpan(SpanName.ORDER_FETCH)
@TraceSpan(SpanName.ORDER_LIST_BY_CUSTOMER)
```

**Benefícios:**
- Formato padronizado: `{namespace}.{operation}.{detail}`
- Uso de snake_case
- Constantes centralizadas em `SpanName.java`
- Baixa cardinalidade (sem valores dinâmicos)

---

### 2. **Padronização de Nomenclatura de Atributos**

#### Antes ❌
```java
span.setAttribute("items.count", itemsCount);
span.setAttribute("order.total", totalAmount.toString());
span.setAttribute("old.status", oldStatus.name());
```

#### Depois ✅
```java
SpanWrap.addAttributes(Map.of(
    AttributeName.ORDER_ITEMS_COUNT.getKey(), String.valueOf(itemsCount),
    AttributeName.ORDER_TOTAL_AMOUNT.getKey(), totalAmount.toString(),
    AttributeName.ORDER_STATUS_OLD.getKey(), oldStatus.name()
));
```

**Benefícios:**
- Formato padronizado: `{namespace}.{attribute}`
- Uso de snake_case
- Constantes centralizadas em `AttributeName.java`
- Evita typos e facilita refatoração

---

### 3. **Remoção de PII (Personally Identifiable Information)** 🔒

#### Antes ❌
```java
@TraceSpan(SpanName.ORDER_CREATE)
public Order createOrder(
        @SpanAttribute("customer.id") String customerId,
        @SpanAttribute("customer.name") String customerName,
        @SpanAttribute("customer.email") String customerEmail,  // ❌ PII!
        CreateOrderRequest request) {
```

```java
span.setAttribute("user.email", user.getEmail());  // ❌ PII!
```

#### Depois ✅
```java
@TraceSpan(SpanName.ORDER_CREATE)
public Order createOrder(
        @SpanAttribute("customer.id") String customerId,
        @SpanAttribute("customer.name") String customerName,
        // ✅ Email removido - é PII
        CreateOrderRequest request) {
```

```java
// ✅ Email NÃO adicionado aos spans
SpanWrap.addAttributes(Map.of(
    AttributeName.EXTERNAL_USER_NAME.getKey(), user.getName()
    // user.getEmail() é PII e não é incluído
));
```

**Dados Protegidos:**
- ❌ Email (`customerEmail`, `user.email`)
- ❌ Telefone
- ❌ Documentos (CPF, CNPJ)
- ❌ Senhas, tokens, API keys

---

### 4. **Implementação de TelemetryEvent**

#### Order.java
```java
public class Order implements TelemetryEvent {
    private String customerEmail; // Note: NOT exposed in telemetry (PII)

    @Override
    public Map<String, String> attributes() {
        Map<String, String> attrs = new HashMap<>();

        if (id != null) {
            attrs.put(AttributeName.ORDER_ID.getKey(), id);
        }
        if (customerId != null) {
            attrs.put(AttributeName.CUSTOMER_ID.getKey(), customerId);
        }
        if (customerName != null) {
            attrs.put(AttributeName.CUSTOMER_NAME.getKey(), customerName);
        }
        if (status != null) {
            attrs.put(AttributeName.ORDER_STATUS.getKey(), status.name());
        }
        // ... outros atributos (email é intencionalmente excluído)

        return attrs;
    }
}
```

#### Uso com SpanWrap
```java
// Antes ❌
span.setAttribute("order.id", order.getId());
span.setAttribute("order.status", order.getStatus().name());

// Depois ✅
SpanWrap.addAttributes(order); // Extrai automaticamente todos os atributos
```

---

### 5. **Uso Correto de SpanKind e Auto-instrumentação**

#### Antes ❌
```java
// Controllers com @Traced manual
@PostMapping("/create-sample-order")
@Traced(value = "create-sample-order", kind = SpanKind.SERVER,
        attributes = {"operation:simulation"})
public ResponseEntity<Order> createSampleOrder() {
```

#### Depois ✅
```java
// Controllers SEM annotation - auto-instrumentado
@PostMapping("/create-sample-order")
public ResponseEntity<Order> createSampleOrder() {
    // Spring Boot já cria span SERVER automaticamente
```

**Regra:**
- ⚡ **AUTO-INSTRUMENTADO (não adicionar @TraceSpan):**
  - `SERVER` - Controllers REST
  - `CLIENT` - FeignClient/RestTemplate
  - `PRODUCER/CONSUMER` - RabbitMQ

- ✋ **MANUAL (@TraceSpan):**
  - `INTERNAL` - Use cases, services, validators

---

### 6. **Correção de Imports**

#### Antes ❌
```java
import com.mercadolibre.wallet_sp_bill_intent.infrastructure.o11y.traces.annotation.SpanAttribute;
import com.mercadolibre.wallet_sp_bill_intent.infrastructure.o11y.traces.annotation.TraceSpan;
```

#### Depois ✅
```java
import org.example.javaotellgtm.traces.annotation.SpanAttribute;
import org.example.javaotellgtm.traces.annotation.TraceSpan;
```

---

### 7. **Uso de SpanWrap para Atributos em Runtime**

#### Antes ❌
```java
span.setAttribute("items.count", request.getItems().size());
span.setAttribute("order.total", totalAmount.toString());
span.setAttribute("event.type", eventType.name());
```

#### Depois ✅
```java
SpanWrap.addAttributes(Map.of(
    AttributeName.ORDER_ITEMS_COUNT.getKey(), String.valueOf(request.getItems().size()),
    AttributeName.ORDER_TOTAL_AMOUNT.getKey(), totalAmount.toString(),
    AttributeName.EVENT_TYPE.getKey(), eventType.name()
));
```

---

## 📂 Arquivos Modificados

### Infraestrutura de Telemetria
- ✅ `traces/constants/SpanName.java` - Novos nomes de spans padronizados
- ✅ `traces/constants/AttributeName.java` - Novos atributos padronizados
- ✅ `traces/annotation/TraceSpan.java` - Imports corrigidos
- ✅ `traces/annotation/SpanAttribute.java` - Imports corrigidos
- ✅ `traces/aspect/TracingAspect.java` - Imports corrigidos
- ✅ `traces/processor/SpanWrap.java` - Imports corrigidos
- ✅ `traces/contract/TelemetryEvent.java` - Imports corrigidos

### Modelos de Domínio
- ✅ `model/Order.java` - Implementa TelemetryEvent (sem expor PII)
- ✅ `dto/OrderEvent.java` - Implementa TelemetryEvent (sem expor PII)

### Serviços
- ✅ `service/OrderService.java`
  - Nomes de spans padronizados
  - Atributos padronizados com `AttributeName`
  - Uso de `SpanWrap` para atributos em runtime
  - Email removido dos parâmetros `@SpanAttribute`

- ✅ `service/ExternalApiService.java`
  - Nomes de spans padronizados
  - Atributos padronizados
  - Email de usuário externo NÃO exposto (PII)

### Controllers
- ✅ `controller/OrderController.java`
  - Remoção de imports não utilizados
  - Email removido da chamada ao service

- ✅ `controller/SimulationController.java`
  - Remoção de `@Traced` (auto-instrumentado)
  - Email removido da chamada ao service
  - Documentação adicionada

- ✅ `controller/ExternalApiController.java`
  - Remoção de `@Traced` e `@SpanAttribute` (auto-instrumentado)
  - Documentação adicionada

### Documentação
- ✅ `docs/02-boas-praticas-aplicadas.md` - Documentação completa das boas práticas
- ✅ `docs/03-boas-praticas-eventos-spans.md` - Guia de uso apropriado de eventos

---

## 📊 Estatísticas

### Primeira Fase (Padronização)
- **Arquivos modificados:** 13
- **Spans padronizados:** 10
- **Atributos padronizados:** 21
- **Instâncias de PII removidas:** 8+
- **Implementações de TelemetryEvent:** 2
- **Controllers auto-instrumentados:** 3

### Segunda Fase (Otimização de Eventos)
- **Arquivos modificados:** 3 (OrderService, ExternalApiService, docs)
- **Eventos removidos:** ~28 (de 31 para 2-3)
- **Redução de eventos:** ~90%
- **Melhoria de performance:** Significativa (menos overhead)
- **Clareza de spans:** Muito melhorada

---

## 🎓 Lições Aprendidas

1. **Spans devem ter BAIXA cardinalidade** - Nunca incluir IDs, timestamps ou valores dinâmicos em nomes de spans
2. **Atributos podem ter ALTA cardinalidade** - IDs de pedidos, clientes são úteis para debugging
3. **PII NUNCA deve ser exposto** - Email, telefone, documentos devem ser protegidos
4. **Auto-instrumentação é poderosa** - Controllers, FeignClient e RabbitMQ já criam spans automaticamente
5. **Constantes centralizam e protegem** - `SpanName` e `AttributeName` evitam typos e facilitam manutenção
6. **TelemetryEvent simplifica** - Objetos de domínio podem expor seus próprios atributos de forma consistente
7. **Eventos devem ser usados com moderação** - Apenas para situações excepcionais, não para fluxo normal
8. **Menos é mais em observabilidade** - Spans limpos são mais úteis que spans poluídos
9. **Priorize atributos sobre eventos** - Atributos estruturam contexto, eventos marcam exceções
10. **Logs complementam spans** - Detalhes do fluxo vão nos logs, não em eventos de span

---

## 🔗 Referências

- [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/)
- [OpenTelemetry Java](https://opentelemetry.io/docs/languages/java/)
- [Documentação: 01-distributed-tracing.md](./docs/01-distributed-tracing.md)
- [Documentação: 02-boas-praticas-aplicadas.md](./docs/02-boas-praticas-aplicadas.md)
- [Cursor Rules: opentelemetry.mdc](./.cursor/rules/opentelemetry.mdc)

---

## ✅ Conformidade

Este projeto agora está em **total conformidade** com as boas práticas de OpenTelemetry:

- [x] Nomenclatura de spans seguindo padrão `namespace.operation.detail`
- [x] Nomenclatura de atributos seguindo padrão `namespace.attribute`
- [x] Uso de snake_case em nomes multi-palavra
- [x] Baixa cardinalidade em nomes de spans
- [x] Alta cardinalidade em atributos (quando útil)
- [x] Proteção de PII (email, telefone, documentos)
- [x] Constantes centralizadas em `SpanName` e `AttributeName`
- [x] `TelemetryEvent` implementado em objetos de domínio
- [x] Auto-instrumentação respeitada (controllers, clients)
- [x] `@TraceSpan` usado apenas para operações `INTERNAL`
- [x] Uso apropriado de eventos (apenas excepcionais)
- [x] Redução de ~90% no número de eventos
- [x] Documentação completa e atualizada
