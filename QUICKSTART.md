# 🚀 Guia Rápido de Uso

## Iniciar a Aplicação

### Opção 1: Script Automático (Recomendado)
```bash
./start.sh
```

### Opção 2: Manual
```bash
# 1. Iniciar serviços (RabbitMQ, MongoDB, Grafana)
docker compose up -d

# 2. Compilar e executar a aplicação
./gradlew bootRun
```

## Testar a API

### Teste Rápido com cURL

```bash
# 1. Criar um pedido de exemplo
curl -X POST http://localhost:8080/api/simulation/create-sample-order

# Você receberá uma resposta com o ID do pedido, exemplo:
# {"id":"675433f3c8b8a123456789ab","customerId":"CUST-1234",...}

# 2. Simular o fluxo completo do pedido (substitua o ID)
curl -X POST http://localhost:8080/api/simulation/simulate-order-flow/675433f3c8b8a123456789ab

# 3. Verificar o status do pedido
curl http://localhost:8080/api/orders/675433f3c8b8a123456789ab

# 4. Gerar tráfego para ver mais traces
curl -X POST "http://localhost:8080/api/simulation/generate-traffic?orderCount=10"
```

### Criar Pedido Customizado

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
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
      }
    ]
  }'
```

## Visualizar Traces no Grafana

1. Acesse: http://localhost:3000
2. Login: `admin` / `admin`
3. Vá em **Explore** (ícone da bússola no menu lateral)
4. Selecione **Tempo** como data source
5. Clique em **Search** para ver todos os traces
6. Filtre por:
   - `service.name = "java-otel-lgtm"`
   - `span.name = "create-order"`
   - Tags customizadas como `order.id`, `customer.email`

## Visualizar Filas no RabbitMQ

1. Acesse: http://localhost:15672
2. Login: `myuser` / `secret`
3. Vá em **Queues** para ver as mensagens
4. Clique em uma fila para ver detalhes e mensagens

## Operações Interessantes

### 1. Criar e Acompanhar um Pedido Completo

```bash
# Criar pedido
ORDER_ID=$(curl -s -X POST http://localhost:8080/api/simulation/create-sample-order | jq -r '.id')

echo "Pedido criado: $ORDER_ID"

# Simular fluxo
curl -X POST "http://localhost:8080/api/simulation/simulate-order-flow/$ORDER_ID"

# Acompanhar status a cada 2 segundos
watch -n 2 "curl -s http://localhost:8080/api/orders/$ORDER_ID | jq '.status'"
```

### 2. Gerar Carga e Analisar Performance

```bash
# Gerar 50 pedidos com fluxos aleatórios
curl -X POST "http://localhost:8080/api/simulation/generate-traffic?orderCount=50"

# No Grafana, você verá:
# - Traces distribuídos de todos os pedidos
# - Propagação de contexto através do RabbitMQ
# - Duração de cada operação
# - Erros (se houver)
```

### 3. Listar Todos os Pedidos

```bash
curl http://localhost:8080/api/orders | jq '.'
```

### 4. Buscar Pedidos de um Cliente

```bash
curl http://localhost:8080/api/orders/customer/CUST-001 | jq '.'
```

## Fluxo de Status dos Pedidos

```
PENDING
   ↓
PAYMENT_PROCESSING
   ↓
PAYMENT_CONFIRMED
   ↓
PREPARING
   ↓
SHIPPED
   ↓
DELIVERED
```

Cada mudança de status:
1. Salva no MongoDB
2. Publica evento no RabbitMQ
3. Consumer processa de forma assíncrona
4. Gera spans no OpenTelemetry
5. Visível no Grafana

## Eventos RabbitMQ Gerados

- **order.created** → Envia email de confirmação
- **payment.processing** → Processa pagamento
- **payment.confirmed** → Confirma pagamento
- **order.shipped** → Gera tracking e envia email

## Spans OpenTelemetry Customizados

- `create-order` - Criação do pedido
- `update-order-status` - Atualização de status
- `publish-order-event` - Publicação no RabbitMQ
- `handle-order-created` - Processamento de pedido criado
- `handle-payment-event` - Processamento de pagamento
- `handle-shipping-event` - Processamento de envio
- `handle-notification` - Envio de notificação

## Verificar Logs da Aplicação

```bash
# Se iniciou com ./gradlew bootRun
# Os logs aparecem no console

# Para ver logs do Docker Compose
docker compose logs -f
```

## Parar a Aplicação

```bash
# Parar Spring Boot
# Ctrl+C no terminal

# Parar serviços Docker
docker compose down

# Parar e remover volumes (limpa dados)
docker compose down -v
```

## Troubleshooting

### Erro ao conectar no MongoDB
```bash
# Verificar se MongoDB está rodando
docker compose ps

# Reiniciar MongoDB
docker compose restart mongodb
```

### Erro ao conectar no RabbitMQ
```bash
# Verificar se RabbitMQ está rodando
docker compose ps

# Ver logs do RabbitMQ
docker compose logs rabbitmq

# Reiniciar RabbitMQ
docker compose restart rabbitmq
```

### Traces não aparecem no Grafana
```bash
# Verificar se Grafana LGTM está rodando
docker compose ps

# Verificar endpoint no application.yml
# management.otlp.tracing.endpoint deve ser http://localhost:4318/v1/traces

# Gerar mais tráfego
curl -X POST "http://localhost:8080/api/simulation/generate-traffic?orderCount=5"
```

## Próximos Passos

1. Explore os traces no Grafana e veja a propagação de contexto
2. Observe as mensagens sendo processadas no RabbitMQ
3. Veja os dados persistidos no MongoDB
4. Crie dashboards customizados no Grafana
5. Analise a performance de cada operação através dos spans

## Recursos Úteis

- 📖 [Documentação Spring Boot](https://spring.io/projects/spring-boot)
- 🐰 [Documentação RabbitMQ](https://www.rabbitmq.com/documentation.html)
- 📊 [Documentação OpenTelemetry](https://opentelemetry.io/docs/)
- 📈 [Documentação Grafana](https://grafana.com/docs/)
