# Instrumentação de Observabilidade (O11y) - Distributed Tracing

## Visão Geral

Este documento descreve como instrumentar código para distributed tracing usando **OpenTelemetry** na aplicação **wallet-sp-bill-intent**.

Você aprenderá a usar as anotações `@TraceSpan` e `@SpanAttribute` para criar spans automaticamente, além do processor `SpanWrap` para enriquecer spans com atributos de negócio.

---

## 1. Annotation @TraceSpan

### O que faz

A annotation `@TraceSpan` cria automaticamente um span OpenTelemetry quando o método é executado. O span rastreia a operação, mede sua duração e captura exceções automaticamente.

###Atributos

| Atributo | Tipo | Obrigatório | Descrição |
|----------|------|-------------|-----------|
| `value` | `String` | Não | Nome do span. Se vazio, gera automaticamente como `ClassName.methodName` |
| `kind` | `SpanKind` | Não | Tipo do span. Default: `INTERNAL` |

### SpanKind - Tipos de Span

O `SpanKind` indica o tipo de operação sendo rastreada:

| SpanKind | Quando usar | Instrumentação |
|----------|-------------|----------------|
| `SERVER` | Para **endpoints REST** que recebem requisições HTTP | ⚡ **Automática** - Já instrumentado pelo Fury |
| `CLIENT` | Para **chamadas HTTP externas** a outros serviços/APIs | ⚡ **Automática** - Já instrumentado pelo Fury |
| `INTERNAL` | Para **operações internas** da aplicação (use cases, validators, etc.) | ✋ **Manual** - Use `@TraceSpan` |
| `PRODUCER` | Para **publicação de mensagens** (BigQueue, Streams, etc.) | ⚡ **Automática** - Já instrumentado pelo Fury |
| `CONSUMER` | Para **consumo de mensagens** (BigQueue, Streams, etc.) | ⚡ **Automática** - Já instrumentado pelo Fury |

> ⚠️ **Importante:** `SERVER`, `CLIENT`, `PRODUCER` e `CONSUMER` já possuem instrumentação automática fornecida pelo Fury. **Não é necessário adicionar `@TraceSpan`** para esses cenários. Use apenas para operações `INTERNAL`.

### Como usar

#### Exemplo 1: Use Case (operação interna) - ✅ USE AQUI

```java
@Service
public class CreateBarcodeIntentUseCase {

    @TraceSpan(value = SpanName.INTENT_CREATE_BARCODE, kind = SpanKind.INTERNAL)
    public BillIntent execute(@SpanAttribute CreateBarcodeIntentRequest request, Context context) {
        // Span INTERNAL para rastrear a lógica de negócio
        return createIntent(request);
    }
}
```

#### Exemplo 2: Nome automático

```java
@Service
public class FetchIntentUseCase {

    @TraceSpan // Nome será "FetchIntentUseCase.retrieve", kind será INTERNAL (default)
    public BillIntent retrieve(String intentId) {
        return repository.findById(intentId);
    }
}
```

### O que acontece quando o método é executado

1. ✅ **Span é criado** com o nome e tipo configurados
2. ✅ **Método é executado** normalmente
3. ✅ **Duração é medida** automaticamente
4. ✅ **Status OK** é definido se não houver erros
5. ✅ **Exceções são capturadas** e registradas no span com status ERROR
6. ✅ **Span é encerrado** ao final da execução

---

## 2. Annotation @SpanAttribute

### O que faz

A annotation `@SpanAttribute` adiciona automaticamente parâmetros do método como atributos do span. Atributos são metadados importantes para filtrar e analisar traces.

### Atributos

| Atributo | Tipo | Obrigatório | Descrição |
|----------|------|-------------|-----------|
| `value` | `String` | Não | Nome da chave do atributo no span. Se vazio e o parâmetro implementa `TelemetryEvent`, extrai automaticamente os atributos do objeto |

### Tipos suportados

| Tipo do parâmetro | Como é adicionado |
|-------------------|-------------------|
| `String` | Diretamente como string |
| `Long`, `Integer` | Convertido para número |
| `Double`, `Float` | Convertido para decimal |
| `Boolean` | Como booleano |
| Objetos | Convertido via `toString()` |
| `null` | Ignorado automaticamente |

### Como usar

#### Exemplo 1: Atributos básicos

```java
@TraceSpan(SpanName.INTENT_CREATE_BARCODE)
public BillIntent execute(
    @SpanAttribute("bill_intent.site") String siteId,
    @SpanAttribute("user.id") String userId
) {
    // O span terá os atributos:
    // - bill_intent.site = valor do siteId
    // - user.id = valor do userId
    return createIntent(siteId, userId);
}
```

#### Exemplo 2: Múltiplos atributos

```java
@TraceSpan(SpanName.INTENT_PATCH)
public BillIntent patch(
    @SpanAttribute("bill_intent.id") String intentId,
    @SpanAttribute("parameter.id") String parameterId,
    @SpanAttribute("user.id") String userId
) {
    // Todos os 3 parâmetros serão atributos do span
    return patchIntent(intentId, parameterId);
}
```

#### Exemplo 3: Sem valor quando implementa TelemetryEvent

```java
// Objeto que implementa TelemetryEvent
public class BillIntent implements TelemetryEvent {
    @Override
    public Map<String, String> attributes() {
        return Map.of(
            AttributeName.INTENT_ID.getKey(), id,
            AttributeName.INTENT_SITE.getKey(), site.name()
        );
    }
}

// Uso sem especificar o nome do atributo
@TraceSpan(SpanName.INTENT_PATCH)
public void patch(@SpanAttribute BillIntent intent) {
    // Os atributos do BillIntent (id, site) são extraídos automaticamente
    // Não é necessário passar o nome porque BillIntent implementa TelemetryEvent
    updateIntent(intent);
}
```

### Quando usar @SpanAttribute

✅ **Use para:**
- IDs importantes (userId, intentId, productId, etc.)
- Informações de contexto (provider, flow, site)
- Dados que ajudam a filtrar traces

❌ **Não use para:**
- Informações sensíveis (senhas, tokens)
- PII (CPF, email, telefone)
- Objetos muito grandes (podem gerar overhead)
- Dados que mudam muito rapidamente

---

## 3. Processor SpanWrap

### O que faz

O `SpanWrap` é uma classe utilitária que permite **adicionar atributos ao span atual** de forma manual, diretamente no código. É útil quando você precisa enriquecer um span com informações que não estão disponíveis como parâmetros do método.

### Como funciona

O `SpanWrap` trabalha com o **span atual** no contexto do OpenTelemetry. Isso significa que ele adiciona atributos ao span que está ativo no momento da chamada (geralmente criado por `@TraceSpan`).

#### Características

- ✅ Adiciona atributos ao span ativo no momento da chamada
- ✅ Filtra automaticamente valores `null`
- ✅ Valida se existe um span válido antes de adicionar
- ✅ Não lança exceções (operação segura)
- ✅ Suporta objetos que implementam `TelemetryEvent`

### Métodos disponíveis

#### 1. `SpanWrap.addAttributes(Map<String, String> attributes)`

Adiciona um mapa de atributos ao span atual.

```java
@TraceSpan(SpanName.INTENT_CREATE_BARCODE)
public BillIntent execute(CreateBarcodeIntentRequest request, Context context) {
    final var intent = request.toDomain();

    // Adiciona atributos manualmente durante a execução
    SpanWrap.addAttributes(Map.of(
        AttributeName.INTENT_FLOW.getKey(), "barcode",
        AttributeName.INTENT_SITE.getKey(), intent.getSiteName()
    ));

    // Continua a lógica...
    return processIntent(intent);
}
```

#### 2. `SpanWrap.addAttributes(TelemetryEvent event)`

Adiciona atributos extraídos de um objeto que implementa `TelemetryEvent`.

```java
@TraceSpan(SpanName.INTENT_FETCH)
public BillIntent fetch(String intentId) {
    BillIntent intent = repository.findById(intentId);

    // Enriquece o span com os atributos do objeto BillIntent
    SpanWrap.addAttributes(intent);

    return intent;
}
```

### Quando usar SpanWrap

✅ **Use quando:**
- Precisa adicionar atributos **durante** a execução do método
- Atributos dependem de dados obtidos em tempo de execução
- Quer enriquecer o span com dados de objetos de domínio
- Precisa adicionar atributos condicionalmente

❌ **Não use quando:**
- Os atributos já estão disponíveis como parâmetros → use `@SpanAttribute`
- Quer criar um novo span → use `@TraceSpan`

---

## 4. Interface TelemetryEvent

### O que faz

`TelemetryEvent` é uma interface que você implementa em objetos de domínio para que eles possam fornecer seus atributos de telemetria automaticamente.

### Como funciona

Ao implementar esta interface, seu objeto define um método `attributes()` que retorna um mapa com os atributos relevantes para observabilidade.

```java
public interface TelemetryEvent {
    Map<String, String> attributes();
}
```

### Como usar

#### Implementação básica

```java
public class BillIntent implements TelemetryEvent {
    private String id;
    private Site site;
    private BillIntentFlow flow;

    @Override
    public Map<String, String> attributes() {
        Map<String, String> attrs = new HashMap<>();

        if (id != null) {
            attrs.put(AttributeName.INTENT_ID.getKey(), id);
        }
        if (site != null) {
            attrs.put(AttributeName.INTENT_SITE.getKey(), site.name());
        }
        if (flow != null) {
            attrs.put(AttributeName.INTENT_FLOW.getKey(), flow.name().toLowerCase());
        }

        return attrs;
    }
}
```

#### Usando com SpanWrap

```java
@TraceSpan(SpanName.INTENT_CREATE_BARCODE)
public void process(String intentId) {
    BillIntent intent = repository.findById(intentId);

    // Adiciona todos os atributos do BillIntent ao span atual
    SpanWrap.addAttributes(intent);

    // Processa o intent...
}
```

---

## 5. Enums de Constantes

### 5.1 AttributeName

Enum que centraliza os nomes das chaves de atributos usados nos spans. Garante consistência e evita typos.

**Como usar:**

```java
// Usando o enum
SpanWrap.addAttributes(Map.of(
    AttributeName.USER_ID.getKey(), userId,
    AttributeName.INTENT_ID.getKey(), intentId
));
```

**Principais categorias:**
- User: `USER_ID`
- HTTP: `HTTP_STATUS_CODE`, `HTTP_METHOD`
- Bill Intent: `INTENT_ID`, `INTENT_FLOW`, `INTENT_PROVIDER`, `INTENT_SITE`, `INTENT_STATUS`
- Validation: `BARCODE`, `DEBT_ID`, `PRODUCT_ID`, `ENTITY_ID`, `COMPANY_ID`
- Feature Flags: `FF_KEY`, `FF_RESULT_VALUE`, `FF_VARIANT_ID`
- Errors: `ERROR_TYPE`, `ERROR_CODE`, `ERROR_MESSAGE`, `ERROR_DESCRIPTION`

### 5.2 SpanName

Classe com constantes para nomes de spans padronizados.

**Como usar:**

```java
@TraceSpan(SpanName.INTENT_CREATE_BARCODE)
public BillIntent createBarcode(String barcode) {
    // ...
}
```

**Principais operações:**
- Bill Intent: `INTENT_CREATE_BARCODE`, `INTENT_CREATE_PRODUCT`, `INTENT_FETCH`, `INTENT_PATCH`
- Validation: `VALIDATE_BARCODE`, `VALIDATE_DEBT`, `VALIDATE_PRODUCT`
- Gateway: `GATEWAY_GET_UTILITY`, `GATEWAY_CONFIRM_UTILITY`

---

## 6. Padrões de Uso

> ⚠️ **Importante:** A instrumentação automática do Fury já cria spans para endpoints REST (`SERVER`), chamadas HTTP externas (`CLIENT`) e mensageria (`PRODUCER`/`CONSUMER`). **Foque em instrumentar operações internas (`INTERNAL`)** da sua aplicação.

### Use Cases (Lógica de negócio) - ✅ Principal uso

Use `SpanKind.INTERNAL` (ou omita, pois é o default) e adicione contexto durante a execução:

```java
@Service
public class CreateBarcodeIntentUseCase {

    @TraceSpan(SpanName.INTENT_CREATE_BARCODE)
    public BillIntent execute(
        @SpanAttribute CreateBarcodeIntentRequest request,
        Context context) {

        final var intent = request.toDomain();

        // Adiciona contexto adicional durante a execução
        SpanWrap.addAttributes(Map.of(
            AttributeName.INTENT_FLOW.getKey(), "barcode",
            AttributeName.INTENT_SITE.getKey(), intent.getSiteName()
        ));

        return processIntent(intent);
    }
}
```

### Domain Objects com Telemetria

Implemente `TelemetryEvent` para objetos que agregam contexto importante:

```java
public class BillIntent implements TelemetryEvent {
    private String id;
    private Site site;
    private BillIntentFlow flow;

    @Override
    public Map<String, String> attributes() {
        return Map.of(
            AttributeName.INTENT_ID.getKey(), id,
            AttributeName.INTENT_SITE.getKey(), site.name(),
            AttributeName.INTENT_FLOW.getKey(), flow.name().toLowerCase()
        );
    }
}

// Uso
BillIntent intent = repository.findById(intentId);
SpanWrap.addAttributes(intent);
```

---

## 7. Boas Práticas

### ✅ Faça

- **Foque em operações `INTERNAL`** - Controllers e clients HTTP já são instrumentados automaticamente
- Use `AttributeName` para nomes de atributos (evita typos e padroniza)
- Use `SpanName` para nomes de spans quando disponível
- Adicione atributos que ajudam no troubleshooting (IDs, status, flow)
- Implemente `TelemetryEvent` em domain objects relevantes
- Use `SpanWrap` para enriquecer spans automáticos com atributos específicos da aplicação
- Deixe exceções serem capturadas automaticamente pelo `@TraceSpan`

### ❌ Evite

- **Adicionar `@TraceSpan` com `SpanKind.SERVER` ou `CLIENT`** - já existe instrumentação automática
- Adicionar dados sensíveis (senhas, tokens)
- Adicionar PII (CPF, CNPJ, email, telefone, documentos)
- Criar spans em loops com alto volume
- Adicionar atributos muito grandes (limite ~1KB)
- Usar strings hardcoded para nomes de atributos
- Criar muitos spans em operações de baixo nível
- Duplicar spans que já são criados automaticamente

### 🎯 Guia de Decisão

| Situação | Use | Observação |
|----------|-----|------------|
| Instrumentar endpoint REST | ❌ Nada | Já instrumentado automaticamente |
| Instrumentar chamada HTTP externa | ❌ Nada | Já instrumentado automaticamente |
| Instrumentar use case | ✅ `@TraceSpan` | Principal uso - operações INTERNAL |
| Adicionar parâmetros como atributos | ✅ `@SpanAttribute` | Em qualquer método com span |
| Adicionar dados obtidos em runtime | ✅ `SpanWrap.addAttributes(Map)` | Enriquece span atual |
| Adicionar atributos de um objeto | ✅ `SpanWrap.addAttributes(TelemetryEvent)` | Enriquece span atual |
| Criar objeto com telemetria | ✅ Implementar `TelemetryEvent` | Domain objects importantes |

---

## 8. Acessando Traces e Spans

### Onde visualizar

As traces e spans da aplicação podem ser visualizadas em duas ferramentas:

#### Grafana - O11y Events

[**O11y - Events**](https://grafana-service.furycloud.io/d/d50f0009-2af7-4f75-8d91-5e7e0fad89ba/events?orgId=1)

- ✅ **Sampling rate: 100%** - Todas as requisições são capturadas
- ✅ Visualização completa de traces e spans
- ✅ Filtragem por atributos customizados
- ✅ Análise detalhada de performance

#### New Relic

- ⚠️ **Sampling rate: 1%** - Apenas 1% das requisições são capturadas
- Útil para análise geral de performance
- Menos adequado para troubleshooting específico

### Recomendação

Para análise detalhada e troubleshooting, **use o Grafana (O11y Events)** devido ao sampling de 100%. O New Relic é melhor para análise de tendências gerais.

---

## 9. Referências

### Documentação Oficial

- [OpenTelemetry Java](https://opentelemetry.io/docs/languages/java/) - Documentação oficial do OpenTelemetry para Java
- [Fury Instrumentação](https://furydocs.io/o11y-docs/0.40.0/guide/#/tracing/introduction) - Guia de instrumentação no ecossistema Fury
- [Fury O11y Events](https://furydocs.io/o11y-docs/0.40.0/guide/#/events/instrumentation/java) - Instrumentação de eventos no Fury

---

**Versão:** 1.0
**Data:** 2025-01-19
**Aplicação:** wallet-sp-bill-intent
