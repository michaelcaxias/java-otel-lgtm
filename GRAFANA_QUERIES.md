# 📊 Queries Úteis para Grafana

## 🔍 Queries no Tempo (Traces)

### Buscar todos os traces da aplicação
```
service.name="java-otel-lgtm"
```

### Buscar traces de criação de pedidos
```
service.name="java-otel-lgtm" && span.name="create-order"
```

### Buscar traces por ID de pedido específico
```
service.name="java-otel-lgtm" && order.id="675433f3c8b8a123456789ab"
```

### Buscar traces por cliente
```
service.name="java-otel-lgtm" && customer.email="joao.silva@email.com"
```

### Buscar traces de pagamento
```
service.name="java-otel-lgtm" && span.name="handle-payment-event"
```

### Buscar traces com erros
```
service.name="java-otel-lgtm" && status=error
```

### Buscar traces lentos (mais de 1 segundo)
```
service.name="java-otel-lgtm" && duration>1s
```

### Buscar traces de envio com tracking
```
service.name="java-otel-lgtm" && span.name="handle-shipping-event"
```

### Buscar por tipo de evento
```
service.name="java-otel-lgtm" && event.type="PAYMENT_CONFIRMED"
```

### Buscar notificações enviadas
```
service.name="java-otel-lgtm" && span.name="handle-notification"
```

## 📈 Queries no Prometheus/Mimir (Métricas)

### Taxa de requisições HTTP
```promql
rate(http_server_requests_seconds_count{application="java-otel-lgtm"}[5m])
```

### Taxa de requisições por endpoint
```promql
sum by (uri) (rate(http_server_requests_seconds_count{application="java-otel-lgtm"}[5m]))
```

### Latência P95 das requisições
```promql
histogram_quantile(0.95,
  sum by (le) (rate(http_server_requests_seconds_bucket{application="java-otel-lgtm"}[5m]))
)
```

### Latência P99 das requisições
```promql
histogram_quantile(0.99,
  sum by (le) (rate(http_server_requests_seconds_bucket{application="java-otel-lgtm"}[5m]))
)
```

### Taxa de erros HTTP (4xx, 5xx)
```promql
sum(rate(http_server_requests_seconds_count{application="java-otel-lgtm", status=~"4.*|5.*"}[5m]))
```

### Uso de memória JVM
```promql
jvm_memory_used_bytes{application="java-otel-lgtm"}
```

### Threads ativas JVM
```promql
jvm_threads_live_threads{application="java-otel-lgtm"}
```

### Garbage Collection
```promql
rate(jvm_gc_pause_seconds_count{application="java-otel-lgtm"}[5m])
```

## 📝 Queries no Loki (Logs)

### Todos os logs da aplicação
```logql
{service_name="java-otel-lgtm"}
```

### Logs de erro
```logql
{service_name="java-otel-lgtm"} |= "ERROR"
```

### Logs relacionados a pedidos
```logql
{service_name="java-otel-lgtm"} |= "order"
```

### Logs com trace ID específico
```logql
{service_name="java-otel-lgtm"} |= "675433f3c8b8a123456789ab"
```

### Logs de pagamento
```logql
{service_name="java-otel-lgtm"} |= "payment"
```

### Contagem de erros por minuto
```logql
sum(count_over_time({service_name="java-otel-lgtm"} |= "ERROR" [1m]))
```

## 🎯 Dashboards Sugeridos

### Dashboard 1: Overview da Aplicação

**Painéis:**
1. **Total de Requisições** (Stat)
   - Query: `sum(rate(http_server_requests_seconds_count[5m]))`

2. **Latência Média** (Stat)
   - Query: `histogram_quantile(0.50, sum by (le) (rate(http_server_requests_seconds_bucket[5m])))`

3. **Taxa de Erro** (Stat)
   - Query: `sum(rate(http_server_requests_seconds_count{status=~"5.*"}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) * 100`

4. **Requisições por Segundo** (Graph)
   - Query: `sum by (uri) (rate(http_server_requests_seconds_count[5m]))`

5. **Distribuição de Latência** (Graph)
   - Query P50: `histogram_quantile(0.50, sum by (le) (rate(http_server_requests_seconds_bucket[5m])))`
   - Query P95: `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[5m])))`
   - Query P99: `histogram_quantile(0.99, sum by (le) (rate(http_server_requests_seconds_bucket[5m])))`

### Dashboard 2: Pedidos e Eventos

**Painéis:**
1. **Pedidos Criados** (Stat)
   - Trace Query: `service.name="java-otel-lgtm" && span.name="create-order"`

2. **Eventos Publicados** (Graph)
   - Trace Query: `service.name="java-otel-lgtm" && span.name="publish-order-event"`
   - Group by: `event.type`

3. **Tempo de Processamento de Pedidos** (Graph)
   - Trace Query: `service.name="java-otel-lgtm" && span.name="create-order"`
   - Show: Duration distribution

4. **Eventos por Tipo** (Pie Chart)
   - Trace Query: `service.name="java-otel-lgtm" && span.name="publish-order-event"`
   - Group by: `event.type`

### Dashboard 3: RabbitMQ e Mensageria

**Painéis:**
1. **Mensagens Publicadas** (Graph)
   - Trace Query: `service.name="java-otel-lgtm" && span.name="publish-order-event"`

2. **Tempo de Processamento por Consumer** (Graph)
   - Queries separadas:
     - `span.name="handle-order-created"`
     - `span.name="handle-payment-event"`
     - `span.name="handle-shipping-event"`
     - `span.name="handle-notification"`

3. **Taxa de Sucesso de Pagamentos** (Stat)
   - Trace Query: `service.name="java-otel-lgtm" && span.name="handle-payment-event" && payment.status="confirmed"`
   - Vs total: `span.name="handle-payment-event"`

4. **Notificações Enviadas** (Stat)
   - Trace Query: `service.name="java-otel-lgtm" && span.name="handle-notification"`

### Dashboard 4: Performance e Recursos

**Painéis:**
1. **Uso de Memória JVM** (Graph)
   - Query: `jvm_memory_used_bytes / jvm_memory_max_bytes * 100`

2. **Threads JVM** (Graph)
   - Query: `jvm_threads_live_threads`

3. **Garbage Collection** (Graph)
   - Query: `rate(jvm_gc_pause_seconds_sum[5m])`

4. **CPU Usage** (Graph)
   - Query: `process_cpu_usage`

## 🔔 Alertas Sugeridos

### Alerta 1: Alta Taxa de Erro
```promql
sum(rate(http_server_requests_seconds_count{status=~"5.*"}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m]))
> 0.05
```
**Threshold:** > 5% de erros
**Ação:** Investigar logs e traces

### Alerta 2: Latência Alta
```promql
histogram_quantile(0.95,
  sum by (le) (rate(http_server_requests_seconds_bucket[5m]))
) > 2
```
**Threshold:** P95 > 2 segundos
**Ação:** Analisar spans lentos

### Alerta 3: Memória Alta
```promql
jvm_memory_used_bytes{area="heap"}
/
jvm_memory_max_bytes{area="heap"}
> 0.9
```
**Threshold:** > 90% do heap usado
**Ação:** Verificar memory leaks

### Alerta 4: Falhas de Pagamento
```promql
count(traces{service.name="java-otel-lgtm", span.name="handle-payment-event", payment.status="failed"})
```
**Threshold:** > 10 falhas em 5 minutos
**Ação:** Investigar integração de pagamento

## 💡 Dicas de Uso

### Como encontrar o Trace ID nos logs
1. Os logs incluem o trace ID no formato: `[traceId,spanId]`
2. Copie o trace ID
3. No Grafana Explore > Tempo
4. Cole o trace ID no campo de busca

### Como correlacionar Logs com Traces
1. Veja um trace interessante no Tempo
2. Copie o trace ID
3. Vá para Explore > Loki
4. Use query: `{service_name="java-otel-lgtm"} |= "TRACE_ID_AQUI"`

### Como investigar pedido específico
1. Tenha o ID do pedido (MongoDB ID)
2. No Tempo, busque: `order.id="ID_DO_PEDIDO"`
3. Veja todos os spans relacionados
4. Analise a timeline completa

### Como ver propagação de contexto
1. Busque um trace de criação de pedido
2. Expanda o trace completo
3. Veja spans de:
   - HTTP request (`POST /api/orders`)
   - Service method (`create-order`)
   - Message publish (`publish-order-event`)
   - Message consume (`handle-order-created`)
   - Notification (`handle-notification`)
4. Todos conectados pelo mesmo trace ID!

## 📚 Recursos Adicionais

- [Grafana Tempo Query Language](https://grafana.com/docs/tempo/latest/query-editor/)
- [PromQL Basics](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [LogQL Guide](https://grafana.com/docs/loki/latest/logql/)
- [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/)
