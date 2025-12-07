# Java OpenTelemetry + RabbitMQ + MongoDB - Order Management API

API de gerenciamento de pedidos com Spring Boot que demonstra operações interessantes usando RabbitMQ, MongoDB e OpenTelemetry para observabilidade completa com traces e spans.

## 🚀 Tecnologias

- **Java 25** + Spring Boot 4.0.0
- **RabbitMQ** - Message broker para comunicação assíncrona
- **MongoDB** - Banco de dados NoSQL para persistência
- **OpenTelemetry** - Instrumentação para traces, métricas e logs
- **Grafana LGTM** - Stack completo de observabilidade (Loki, Grafana, Tempo, Mimir)

## 📋 Funcionalidades

### Sistema de Pedidos (Orders)
- Criar pedidos com múltiplos itens
- Consultar pedidos por ID ou cliente
- Atualizar status do pedido
- Cancelar pedidos
- Fluxo completo de pedido com eventos assíncronos

### Eventos RabbitMQ
- **Order Created** - Pedido criado
- **Payment Processing** - Processamento de pagamento
- **Payment Confirmed** - Pagamento confirmado
- **Order Preparing** - Pedido em preparação
- **Order Shipped** - Pedido enviado
- **Order Delivered** - Pedido entregue
- **Notifications** - Emails de notificação

### Consumers Assíncronos
- Consumer de pedidos
- Consumer de pagamentos
- Consumer de envio/shipping
- Consumer de notificações por email

### Observabilidade com OpenTelemetry
- **AOP Customizado** - Implementação própria de tracing com `@Traced`
- Traces distribuídos através de toda a aplicação
- Spans customizados com tags relevantes
- Propagação de contexto entre serviços
- Integração com Grafana LGTM
- Ver [CUSTOM_AOP_TRACING.md](CUSTOM_AOP_TRACING.md) e [TRACING_EVOLUTION.md](TRACING_EVOLUTION.md)

## 🛠️ Como Executar

### 1. Iniciar os serviços (RabbitMQ, MongoDB, Grafana)

```bash
docker compose up -d
```

Isso iniciará:
- **Grafana LGTM**: http://localhost:3000
- **RabbitMQ Management**: http://localhost:15672 (user: myuser, pass: secret)
- **MongoDB**: localhost:27017

### 2. Compilar e executar a aplicação

```bash
./gradlew bootRun
```

A API estará disponível em: http://localhost:8080

## 📡 Endpoints da API

### Gerenciamento de Pedidos

#### Criar Pedido
```bash
POST /api/orders
Content-Type: application/json

{
  "customerId": "CUST-001",
  "customerName": "João Silva",
  "customerEmail": "joao.silva@email.com",
  "shippingAddress": "Rua das Flores, 123",
  "paymentMethod": "CREDIT_CARD",
  "items": [
    {
      "productId": "PROD-001",
      "productName": "Notebook",
      "quantity": 1,
      "unitPrice": 2500.00
    },
    {
      "productId": "PROD-002",
      "productName": "Mouse",
      "quantity": 2,
      "unitPrice": 50.00
    }
  ]
}
```

#### Consultar Pedido
```bash
GET /api/orders/{orderId}
```

#### Listar Todos os Pedidos
```bash
GET /api/orders
```

#### Listar Pedidos por Cliente
```bash
GET /api/orders/customer/{customerId}
```

#### Atualizar Status do Pedido
```bash
PATCH /api/orders/{orderId}/status
Content-Type: application/json

{
  "status": "PAYMENT_PROCESSING"
}
```

Status disponíveis:
- `PENDING`
- `PAYMENT_PROCESSING`
- `PAYMENT_CONFIRMED`
- `PREPARING`
- `SHIPPED`
- `DELIVERED`
- `CANCELLED`

#### Cancelar Pedido
```bash
POST /api/orders/{orderId}/cancel
```

### Simulações (para testes)

#### Criar Pedido de Exemplo
```bash
POST /api/simulation/create-sample-order
```

#### Simular Fluxo Completo de Pedido
```bash
POST /api/simulation/simulate-order-flow/{orderId}
```

Isso automaticamente passará o pedido por todos os status:
1. PAYMENT_PROCESSING (2s)
2. PAYMENT_CONFIRMED (3s)
3. PREPARING (2s)
4. SHIPPED (4s)
5. DELIVERED (5s)

#### Gerar Tráfego de Teste
```bash
POST /api/simulation/generate-traffic?orderCount=10
```

Cria múltiplos pedidos e inicia fluxos aleatórios para gerar traces no OpenTelemetry.

## 🔍 Observabilidade

### Acessar Grafana

1. Acesse http://localhost:3000
2. Login padrão: `admin` / `admin`
3. Explore os traces em **Explore** > **Tempo**
4. Visualize métricas em **Explore** > **Mimir**
5. Veja logs em **Explore** > **Loki**

### Traces Gerados

A aplicação gera spans customizados para:
- **HTTP Requests** - Requisições REST
- **Order Creation** - Criação de pedidos
- **Status Updates** - Atualizações de status
- **Message Publishing** - Publicação de mensagens no RabbitMQ
- **Message Consumption** - Consumo de mensagens
- **Payment Processing** - Processamento de pagamentos
- **Shipping** - Envio de pedidos
- **Notifications** - Envio de emails

### Tags nos Spans

Os spans incluem tags relevantes como:
- `order.id`
- `customer.id`
- `customer.email`
- `event.type`
- `order.total`
- `payment.status`
- `tracking.number`

## 🐰 RabbitMQ

### Exchanges
- `order.exchange` - Exchange principal de pedidos
- `notification.exchange` - Exchange de notificações

### Queues
- `order.queue` - Fila de pedidos criados
- `payment.queue` - Fila de eventos de pagamento
- `shipping.queue` - Fila de eventos de envio
- `notification.queue` - Fila de notificações

### Routing Keys
- `order.created` - Pedido criado
- `payment.processing` - Processamento de pagamento
- `payment.confirmed` - Pagamento confirmado
- `order.shipped` - Pedido enviado
- `notification.email` - Notificação por email

Acesse o RabbitMQ Management Console em http://localhost:15672 para visualizar filas, exchanges e mensagens.

## 📊 Exemplo de Fluxo Completo

1. Cliente cria um pedido via API
2. Pedido é salvo no MongoDB
3. Evento `ORDER_CREATED` é publicado no RabbitMQ
4. Consumer processa e envia email de confirmação
5. Status muda para `PAYMENT_PROCESSING`
6. Evento `PAYMENT_PROCESSING` é publicado
7. Consumer simula validação de pagamento
8. Status muda para `PAYMENT_CONFIRMED`
9. Status muda para `PREPARING`
10. Status muda para `SHIPPED`
11. Evento `ORDER_SHIPPED` é publicado
12. Consumer gera número de rastreamento e envia email
13. Status muda para `DELIVERED`

Durante todo o fluxo, traces são enviados ao OpenTelemetry, permitindo visualização completa da jornada do pedido no Grafana.

## 🧪 Testando a Aplicação

### Teste Rápido

```bash
# 1. Criar pedido de exemplo
curl -X POST http://localhost:8080/api/simulation/create-sample-order

# Resposta: { "id": "675433f3c8b8a123456789ab", ... }

# 2. Simular fluxo completo (use o ID retornado)
curl -X POST http://localhost:8080/api/simulation/simulate-order-flow/675433f3c8b8a123456789ab

# 3. Verificar o pedido
curl http://localhost:8080/api/orders/675433f3c8b8a123456789ab

# 4. Ver traces no Grafana
# Acesse http://localhost:3000 e explore os traces
```

### Gerar Tráfego para Análise

```bash
# Gerar 20 pedidos com fluxos aleatórios
curl -X POST "http://localhost:8080/api/simulation/generate-traffic?orderCount=20"
```

Depois vá ao Grafana e explore os traces distribuídos!

## 🎯 Arquitetura

```
┌─────────────┐      ┌──────────────┐      ┌───────────┐
│   Client    │─────▶│  REST API    │─────▶│  MongoDB  │
└─────────────┘      └──────────────┘      └───────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │  RabbitMQ    │
                     └──────────────┘
                            │
                ┌───────────┼───────────┐
                ▼           ▼           ▼
         ┌──────────┐ ┌──────────┐ ┌──────────┐
         │  Order   │ │ Payment  │ │ Shipping │
         │ Consumer │ │ Consumer │ │ Consumer │
         └──────────┘ └──────────┘ └──────────┘
                            │
                            ▼
                  ┌──────────────────┐
                  │  OpenTelemetry   │
                  │     (OTLP)       │
                  └──────────────────┘
                            │
                            ▼
                     ┌─────────────┐
                     │   Grafana   │
                     │    LGTM     │
                     └─────────────┘
```

## 🎯 Instrumentação AOP Customizada

Este projeto implementa uma **solução própria de AOP** para tracing automático usando OpenTelemetry.

### Anotação @Traced

```java
@Traced(
    value = "operation-name",
    kind = SpanKind.INTERNAL,
    attributes = {"key:value", "operation:create"}
)
public Order createOrder(
    @SpanAttribute("customer.id") String customerId,
    CreateOrderRequest request) {
    // Span criado automaticamente com Tracer
    // Gerenciamento completo do ciclo de vida
    // Exceções registradas automaticamente
    return order;
}
```

### Benefícios

- ✅ **57% menos código** comparado ao Tracer manual
- ✅ **80% menos complexidade** comparado ao Tracer manual
- ✅ **Controle total** sobre spans e atributos
- ✅ **Atributos estáticos** na anotação
- ✅ **Gerenciamento automático** de exceções e status
- ✅ **Span Links automáticos** para rastreabilidade ponta a ponta
- ✅ **Totalmente compatível** com OpenTelemetry nativo

### Span Links - Rastreabilidade Distribuída

A aplicação implementa **Span Links** para conectar traces através de boundaries assíncronos:

```
Producer (Trace A):                Consumer (Trace B):
├─ create-order                    └─ handle-order-created
   └─ publish-event                   🔗 LINK → publish-event
      [captura traceId/spanId]           (rastreável no Grafana!)
```

- ✅ Rastreamento completo do producer ao consumer
- ✅ Detecção automática de OrderEvent
- ✅ Links visíveis no Grafana Tempo
- ✅ Zero configuração manual necessária

### Documentação Completa

- 📖 [CUSTOM_AOP_TRACING.md](CUSTOM_AOP_TRACING.md) - Guia completo da implementação AOP
- 📊 [TRACING_EVOLUTION.md](TRACING_EVOLUTION.md) - Evolução e comparação das abordagens
- 🔗 [SPAN_LINKS.md](SPAN_LINKS.md) - Span Links para rastreabilidade ponta a ponta
- 🔍 [COMPARISON.md](COMPARISON.md) - Comparação detalhada entre métodos

## 📝 Licença

Este projeto é um exemplo educacional para demonstração de OpenTelemetry com RabbitMQ e MongoDB.
