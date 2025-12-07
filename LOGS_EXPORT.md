# 📋 Exportação de Logs para Grafana LGTM

## ❌ Problema Atual

**Spring Boot 4.0 NÃO exporta logs automaticamente via OTLP!**

### O que funciona:
- ✅ **Traces** → Exportados via OTLP → Visíveis no Grafana Tempo
- ✅ **Métricas** → Exportadas via OTLP → Visíveis no Grafana Mimir
- ✅ **Logs no console** → Incluem traceId e spanId

### O que NÃO funciona:
- ❌ **Logs NÃO são enviados** ao Grafana Loki automaticamente
- ❌ Spring Boot não tem suporte nativo para OTLP logs ainda

## 📊 Situação Atual

### Você vê no console:
```
2024-12-07 15:30:45.123  INFO [java-otel-lgtm,abc123def456,789ghi012jkl] --- [nio-8080-exec-1] o.e.j.service.OrderService : Creating new order...
```

### Mas NO GRAFANA:
- ✅ **Traces aparecem** → Você vê os spans no Tempo
- ✅ **Métricas aparecem** → Você vê as métricas no Mimir
- ❌ **Logs NÃO aparecem** → Loki está vazio

## 🔧 Soluções Possíveis

### Opção 1: Logback OTLP Appender (Recomendado para Spring Boot)

#### 1.1 Adicionar dependência no `build.gradle`:

```gradle
dependencies {
    // ... existentes ...

    // Logback OTLP Appender para exportar logs
    implementation 'io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.10.0-alpha'
}
```

#### 1.2 Criar `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Console Appender com traceId/spanId -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}] --- [%15.15t] %-40.40logger{39} : %m%n</pattern>
        </encoder>
    </appender>

    <!-- OTLP Appender para enviar logs ao Grafana LGTM -->
    <appender name="OTLP" class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
        <captureExperimentalAttributes>true</captureExperimentalAttributes>
        <captureKeyValuePairAttributes>true</captureKeyValuePairAttributes>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="OTLP"/>
    </root>

    <logger name="org.example.javaotellgtm" level="INFO"/>
    <logger name="org.springframework.amqp" level="INFO"/>
</configuration>
```

#### 1.3 Configurar endpoint OTLP no `application.yml`:

```yaml
otel:
  logs:
    exporter: otlp
  exporter:
    otlp:
      endpoint: http://localhost:4318
      protocol: http/protobuf
```

---

### Opção 2: Promtail (Sidecar para Loki)

#### 2.1 Adicionar Promtail ao `compose.yaml`:

```yaml
services:
  # ... serviços existentes ...

  promtail:
    image: grafana/promtail:latest
    volumes:
      - ./promtail-config.yaml:/etc/promtail/config.yaml
      - /var/log:/var/log
    command: -config.file=/etc/promtail/config.yaml
    depends_on:
      - grafana-lgtm
```

#### 2.2 Criar `promtail-config.yaml`:

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://grafana-lgtm:3100/loki/api/v1/push

scrape_configs:
  - job_name: spring-boot-logs
    static_configs:
      - targets:
          - localhost
        labels:
          job: java-otel-lgtm
          __path__: /var/log/spring-boot/*.log
```

#### 2.3 Configurar app para escrever logs em arquivo:

```yaml
logging:
  file:
    name: /var/log/spring-boot/application.log
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}] --- [%15.15t] %-40.40logger{39} : %m%n"
```

---

### Opção 3: Fluent Bit (Alternativa ao Promtail)

Similar ao Promtail, mas com mais recursos de processamento.

---

## ✅ Solução Mais Simples (Recomendada)

**Use Logback OTLP Appender** (Opção 1) porque:

1. ✅ Integração nativa com Spring Boot
2. ✅ Logs exportados diretamente via OTLP
3. ✅ Mesma stack (OTLP) para traces, métricas e logs
4. ✅ Sem containers adicionais
5. ✅ TraceId/SpanId automáticos nos logs

---

## 📝 Implementação Passo a Passo (Opção 1)

### 1. Adicionar dependência:

```gradle
implementation 'io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.10.0-alpha'
```

### 2. Criar `logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%X{traceId:-},%X{spanId:-}] --- [%t] %logger{36} : %m%n</pattern>
        </encoder>
    </appender>

    <appender name="OTLP" class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
        <captureExperimentalAttributes>true</captureExperimentalAttributes>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="OTLP"/>
    </root>
</configuration>
```

### 3. Atualizar `application.yml`:

```yaml
otel:
  service:
    name: java-otel-lgtm
  exporter:
    otlp:
      endpoint: http://localhost:4318
      protocol: http/protobuf
  logs:
    exporter: otlp
```

### 4. Reiniciar aplicação

```bash
./gradlew bootRun
```

### 5. Verificar no Grafana

1. Acesse http://localhost:3000
2. Vá em **Explore**
3. Selecione **Loki**
4. Query: `{service_name="java-otel-lgtm"}`
5. Veja os logs correlacionados com traces!

---

## 🎯 Verificação

### Como saber se está funcionando:

#### No Console (sempre funciona):
```
2024-12-07 15:30:45.123  INFO [abc123,def456] --- [main] OrderService : Creating order...
```

#### No Grafana Loki (precisa da configuração):

1. Vá em **Explore** → **Loki**
2. Use query: `{service_name="java-otel-lgtm"}`
3. Você deve ver os logs
4. Clique em um log → Veja o botão "Tempo" para ver o trace relacionado!

---

## 🔗 Correlação Logs ↔ Traces

Com a configuração correta, você poderá:

1. ✅ Ver um **trace** no Tempo
2. ✅ Copiar o **traceId**
3. ✅ Buscar no Loki: `{service_name="java-otel-lgtm"} |= "traceId-aqui"`
4. ✅ Ver todos os **logs** daquele trace
5. ✅ Clicar no botão **Tempo** no log → Ir direto para o trace!

---

## 📚 Resumo

| Componente | Status Atual | Solução |
|------------|--------------|---------|
| **Traces** | ✅ Funcionando | Já configurado |
| **Métricas** | ✅ Funcionando | Já configurado |
| **Logs (console)** | ✅ Com traceId | Já configurado |
| **Logs (Grafana)** | ❌ NÃO enviados | Adicionar Logback OTLP Appender |

**Próxima ação:** Implementar Opção 1 (Logback OTLP Appender) para ter observabilidade completa! 🚀
