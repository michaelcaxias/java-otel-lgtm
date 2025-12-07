# 🎯 Funcionalidades da API

## 📦 Arquitetura Geral

Esta API demonstra um sistema de gerenciamento de pedidos (e-commerce) com:
- **Comunicação assíncrona** via RabbitMQ
- **Persistência** em MongoDB
- **Observabilidade completa** com OpenTelemetry
- **Visualização** através do Grafana LGTM

## 🔄 Fluxo de Operações

### 1. Criação de Pedido
**Endpoint:** `POST /api/orders`

**O que acontece:**
1. ✅ Pedido é validado
2. 💾 Salvo no MongoDB
3. 📤 Evento `ORDER_CREATED` publicado no RabbitMQ (exchange: `order.exchange`, routing key: `order.created`)
4. 📊 Span `create-order` enviado ao OpenTelemetry com tags:
   - `customer.id`
   - `customer.email`
   - `order.total`
   - `order.id`

**Consumer que processa:**
- `OrderEventConsumer.handleOrderCreated()` recebe o evento
- Simula processamento (500ms)
- Publica notificação de email no RabbitMQ
- Gera span `handle-order-created`

### 2. Processamento de Pagamento
**Endpoint:** `PATCH /api/orders/{id}/status` com `status: PAYMENT_PROCESSING`

**O que acontece:**
1. 🔄 Status atualizado no MongoDB
2. 📤 Evento `PAYMENT_PROCESSING` publicado (routing key: `payment.processing`)
3. 📊 Span `update-order-status` enviado

**Consumer que processa:**
- `OrderEventConsumer.handlePaymentEvent()` recebe o evento
- Simula validação de pagamento (1000ms)
- 90% de taxa de sucesso (aleatório)
- Gera span `handle-payment-event` com tag `payment.status`

### 3. Confirmação de Pagamento
**Endpoint:** `PATCH /api/orders/{id}/status` com `status: PAYMENT_CONFIRMED`

**O que acontece:**
1. 🔄 Status atualizado
2. 📤 Evento `PAYMENT_CONFIRMED` publicado (routing key: `payment.confirmed`)
3. 📊 Span enviado ao OpenTelemetry

### 4. Preparação do Pedido
**Endpoint:** `PATCH /api/orders/{id}/status` com `status: PREPARING`

**O que acontece:**
1. 🔄 Status atualizado
2. 📤 Evento `ORDER_PREPARING` publicado
3. Pedido entra em fase de preparação/separação

### 5. Envio do Pedido
**Endpoint:** `PATCH /api/orders/{id}/status` com `status: SHIPPED`

**O que acontece:**
1. 🔄 Status atualizado
2. 📤 Evento `ORDER_SHIPPED` publicado (routing key: `order.shipped`)
3. 📊 Span enviado

**Consumer que processa:**
- `OrderEventConsumer.handleShippingEvent()` recebe o evento
- Gera número de rastreamento (700ms)
- Publica notificação de envio
- Gera span `handle-shipping-event` com tag `tracking.number`

### 6. Entrega do Pedido
**Endpoint:** `PATCH /api/orders/{id}/status` com `status: DELIVERED`

**O que acontece:**
1. 🔄 Status final atualizado
2. 📤 Evento `ORDER_DELIVERED` publicado
3. Pedido concluído

## 📬 Sistema de Notificações

**Como funciona:**
1. Eventos de pedido disparam publicação de notificações
2. Mensagens vão para `notification.queue` via exchange `notification.exchange`
3. Consumer `OrderEventConsumer.handleNotification()` processa
4. Simula envio de email (300ms)
5. Gera span `handle-notification`

**Tipos de notificações enviadas:**
- ✉️ Confirmação de pedido criado
- ✉️ Pedido enviado com tracking

## 🏷️ Exchanges e Queues RabbitMQ

### Exchanges
- **order.exchange** (TopicExchange)
  - Recebe todos os eventos de pedidos
  - Roteia para diferentes filas baseado em routing keys

- **notification.exchange** (TopicExchange)
  - Recebe eventos de notificação
  - Roteia para fila de notificações

### Queues
- **order.queue**
  - Recebe: eventos `ORDER_CREATED`
  - Processa: criação de pedidos

- **payment.queue**
  - Recebe: eventos `PAYMENT_PROCESSING` e `PAYMENT_CONFIRMED`
  - Processa: validação de pagamentos

- **shipping.queue**
  - Recebe: eventos `ORDER_SHIPPED`
  - Processa: geração de tracking

- **notification.queue**
  - Recebe: notificações de email
  - Processa: envio de emails

## 📊 Spans OpenTelemetry

### Spans HTTP (Automáticos)
- `GET /api/orders`
- `POST /api/orders`
- `PATCH /api/orders/{id}/status`
- `POST /api/orders/{id}/cancel`

### Spans Customizados

#### `create-order`
**Tags:**
- `customer.id`: ID do cliente
- `customer.email`: Email do cliente
- `order.total`: Valor total do pedido
- `order.id`: ID do pedido criado

#### `update-order-status`
**Tags:**
- `order.id`: ID do pedido
- `new.status`: Novo status

#### `publish-order-event`
**Tags:**
- `event.type`: Tipo do evento
- `order.id`: ID do pedido
- `customer.id`: ID do cliente

#### `publish-notification`
**Tags:**
- `notification.email`: Email destino
- `notification.subject`: Assunto

#### `handle-order-created`
**Tags:**
- `order.id`: ID do pedido
- `event.type`: ORDER_CREATED

#### `handle-payment-event`
**Tags:**
- `order.id`: ID do pedido
- `event.type`: Tipo de evento de pagamento
- `payment.amount`: Valor do pagamento
- `payment.status`: Status (confirmed/failed)

#### `handle-shipping-event`
**Tags:**
- `order.id`: ID do pedido
- `event.type`: ORDER_SHIPPED
- `tracking.number`: Número de rastreamento gerado

#### `handle-notification`
**Tags:**
- `notification.email`: Email destino
- `notification.subject`: Assunto

## 🎮 Endpoints de Simulação

### `POST /api/simulation/create-sample-order`
Cria pedido aleatório com:
- Nome de cliente aleatório (8 opções)
- 1 a 3 produtos aleatórios (10 produtos disponíveis)
- Quantidades aleatórias
- Preços aleatórios

### `POST /api/simulation/simulate-order-flow/{orderId}`
Executa fluxo completo automaticamente:
1. Aguarda 2s → PAYMENT_PROCESSING
2. Aguarda 3s → PAYMENT_CONFIRMED
3. Aguarda 2s → PREPARING
4. Aguarda 4s → SHIPPED
5. Aguarda 5s → DELIVERED

Total: ~16 segundos para completar o fluxo

### `POST /api/simulation/generate-traffic?orderCount=N`
Gera N pedidos com:
- Dados aleatórios
- 50% de chance de iniciar fluxo automático
- Delay aleatório entre pedidos (200-700ms)
- Ideal para gerar traces no OpenTelemetry

## 🔍 Observabilidade - O que você verá

### No Grafana (Tempo)
1. **Trace completo do pedido** desde HTTP request até conclusão
2. **Propagação de contexto** através do RabbitMQ
3. **Spans de diferentes serviços** conectados
4. **Duração de cada operação**
5. **Tags customizadas** para filtrar e debugar

### No RabbitMQ Management
1. **Queues ativas** com mensagens
2. **Exchanges** e seus bindings
3. **Mensagens sendo consumidas** em tempo real
4. **Taxa de publicação/consumo**

### No MongoDB
1. **Coleção `orders`** com todos os pedidos
2. **Histórico de status** via campo `updatedAt`
3. **Dados completos** de clientes e itens

## 💡 Cenários de Uso

### Cenário 1: Debug de Pedido Lento
1. Cliente reclama que pedido está demorando
2. Busca pedido por ID
3. Vê trace no Grafana
4. Identifica que pagamento está lento (span `handle-payment-event`)
5. Investiga consumer de pagamentos

### Cenário 2: Análise de Performance
1. Gera tráfego com 100 pedidos
2. Vê distribuição de duração dos spans
3. Identifica gargalos
4. Otimiza operações lentas

### Cenário 3: Monitoramento de Filas
1. Acessa RabbitMQ Management
2. Vê que `payment.queue` tem muitas mensagens
3. Identifica que consumer está lento
4. Escala consumers ou otimiza processamento

## 🚀 Operações Avançadas

### Propagação de Contexto
O OpenTelemetry propaga automaticamente o **trace context** através:
- HTTP headers (automático)
- Mensagens RabbitMQ (através do MessageConverter)
- Permitindo visualização end-to-end

### Anotações @Observed
Classes anotadas com `@Observed`:
- `OrderService`
- `MessagePublisher`
- `OrderEventConsumer`
- `OrderController`
- `SimulationController`

Isso adiciona spans automáticos para todos os métodos públicos.

### Tracer Manual
Uso do `Tracer` do Micrometer para:
- Criar spans customizados
- Adicionar tags relevantes
- Marcar erros com `span.error(exception)`
- Controlar início/fim com `span.start()` e `span.end()`

## 📈 Métricas Disponíveis

Via actuator endpoint `/actuator/metrics`:
- `http.server.requests` - Requisições HTTP
- `rabbitmq.published` - Mensagens publicadas
- `rabbitmq.consumed` - Mensagens consumidas
- JVM metrics (heap, threads, GC)
- Custom metrics (se configuradas)

## 🎯 Próximos Passos Sugeridos

1. Adicionar circuit breaker (Resilience4j)
2. Implementar retry policies
3. Adicionar dead letter queues
4. Criar dashboards personalizados no Grafana
5. Adicionar métricas de negócio customizadas
6. Implementar correlação de logs com trace IDs
