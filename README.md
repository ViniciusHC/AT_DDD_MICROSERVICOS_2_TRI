# Freela Marketplace

Projeto de referência para um marketplace de contratação de freelancers construído com arquitetura de microsserviços em Java e Spring.

A aplicação representa um cenário em que clientes contratam freelancers para a execução de trabalhos. O núcleo do sistema é o gerenciamento dos contratos firmados entre as partes. A partir desse domínio, outros serviços mantêm informações relacionadas a notificações, reputação e auditoria.

## Visão geral

O sistema é composto por seis aplicações Spring Boot:

- `eureka-server`: registro e descoberta dos serviços.
- `api-gateway`: ponto de entrada HTTP da aplicação.
- `contrato-service`: gerenciamento dos contratos entre clientes e freelancers.
- `notificacao-service`: armazenamento das notificações relacionadas aos contratos.
- `reputacao-service`: manutenção de informações agregadas sobre os freelancers.
- `auditoria-service`: registro de eventos relevantes do sistema.

A infraestrutura local utiliza PostgreSQL e Apache Kafka.

```text
                         +-------------------+
                         |      Cliente      |
                         +---------+---------+
                                   |
                                   | HTTP
                                   v
                         +-------------------+
                         |    API Gateway    |
                         |       :8080       |
                         +---------+---------+
                                   |
                     Service Discovery / Eureka
                                   |
                +------------------+------------------+
                |                  |                  |
                v                  v                  v
       +----------------+  +----------------+  +----------------+
       | contrato       |  | notificacao    |  | reputacao      |
       | service :8081  |  | service :8082  |  | service :8083  |
       +----------------+  +----------------+  +----------------+
                |
                |                       +----------------+
                +---------------------->| auditoria      |
                                        | service :8084  |
                                        +----------------+

                          +-------------------+
                          |       Kafka       |
                          |       :9092       |
                          +-------------------+

                          +-------------------+
                          |    PostgreSQL     |
                          |       :5432       |
                          +-------------------+
```

## Domínio

O domínio principal está no `contrato-service`.

Um contrato representa o vínculo entre um cliente e um freelancer para a execução de um trabalho. Cada contrato possui:

- identificador;
- cliente;
- freelancer;
- título do trabalho;
- valor;
- status;
- data de criação.

Os estados disponíveis são:

```text
ATIVO
ENTREGA_REGISTRADA
CONCLUIDO
CANCELADO
```

O fluxo de negócio previsto pelo modelo é:

```text
ATIVO
  |
  v
ENTREGA_REGISTRADA
  |
  v
CONCLUIDO
```

Um contrato ativo também pode ser cancelado.

O `contrato-service` utiliza uma organização inspirada em Domain-Driven Design, separando domínio, aplicação e infraestrutura.

```text
contrato-service
└── src/main/java/br/com/freela/contrato
    ├── application
    ├── domain
    │   ├── event
    │   ├── model
    │   ├── repository
    │   └── shared
    └── infrastructure
        ├── persistence
        └── web
```

O Aggregate `Contrato` concentra as regras relacionadas às mudanças de estado e produz eventos de domínio. Atualmente existe o evento `ContratoCriado`, que contém as principais informações do contrato no momento da criação.

## Serviços

### contrato-service

Responsável pelo ciclo de vida dos contratos.

Porta:

```text
8081
```

Banco:

```text
contrato_db
```

Principais recursos HTTP:

```text
POST /api/contratos
GET  /api/contratos
GET  /api/contratos/{id}
```

Exemplo de criação de contrato:

```json
{
  "clienteId": "11111111-1111-1111-1111-111111111111",
  "freelancerId": "22222222-2222-2222-2222-222222222222",
  "titulo": "Construção de API de pagamentos",
  "valor": 3500.00
}
```

### notificacao-service

Mantém notificações relacionadas aos acontecimentos do marketplace.

Porta:

```text
8082
```

Banco:

```text
notificacao_db
```

As notificações armazenam informações como contrato, destinatário, tipo, mensagem e momento de criação.

### reputacao-service

Mantém informações agregadas sobre a atividade dos freelancers.

Porta:

```text
8083
```

Banco:

```text
reputacao_db
```

Para cada freelancer são mantidos dados como quantidade de contratos concluídos e valor total dos contratos registrados.

Endpoint disponível para consulta:

```text
GET /api/reputacoes
```

### auditoria-service

Responsável pelo armazenamento de registros associados aos eventos do sistema.

Porta:

```text
8084
```

Banco:

```text
auditoria_db
```

Cada registro de auditoria pode armazenar:

- `eventId`;
- `aggregateId`;
- tipo do evento;
- `correlationId`;
- payload original;
- horário de recebimento.

Endpoint disponível para consulta:

```text
GET /api/auditoria
```

## API Gateway

O `api-gateway` é o ponto de entrada HTTP para os microsserviços.

Porta:

```text
8080
```

As rotas configuradas são:

| Caminho | Serviço |
|---|---|
| `/api/contratos/**` | `contrato-service` |
| `/api/notificacoes/**` | `notificacao-service` |
| `/api/reputacoes/**` | `reputacao-service` |
| `/api/auditoria/**` | `auditoria-service` |

O Gateway utiliza Eureka para localizar as instâncias dos serviços.

Também existe suporte ao header:

```text
X-Correlation-Id
```

Quando o header não é enviado pelo cliente, o Gateway gera automaticamente um UUID e o encaminha para o serviço de destino.

## Eureka Server

O Eureka Server mantém o registro das aplicações disponíveis no ambiente.

Porta:

```text
8761
```

Interface web:

```text
http://localhost:8761
```

Os microsserviços utilizam, por padrão:

```text
http://localhost:8761/eureka/
```

como endereço do service registry.

## PostgreSQL

O ambiente utiliza uma única instância PostgreSQL com bancos separados para cada serviço.

```text
Host:     localhost
Porta:    5432
Usuário:  freela
Senha:    freela
```

Bancos criados durante a inicialização:

```text
contrato_db
notificacao_db
reputacao_db
auditoria_db
```

O script de criação dos bancos está em:

```text
infra/postgres/init-databases.sql
```

Os serviços utilizam Hibernate com `ddl-auto: update` para criação e atualização das tabelas locais.

## Apache Kafka

O Apache Kafka é executado em modo KRaft, sem ZooKeeper.

Para aplicações executadas diretamente na máquina:

```text
localhost:9092
```

Para aplicações executadas dentro da rede Docker:

```text
kafka:19092
```

O broker possui listeners separados para comunicação interna e externa.

O ambiente também inclui o Kafka UI.

```text
http://localhost:8090
```

## Logs

Todos os serviços utilizam logs em nível `INFO` com um formato comum contendo data, nível, nome da aplicação, thread, logger e mensagem.

Exemplo:

```text
2026-09-14 14:42:18.431 INFO service=contrato-service thread=http-nio-8081-exec-1 logger=b.c.f.c.a.ContratoApplicationService - contrato.criacao.inicio clienteId=... freelancerId=...
```

O código registra pontos importantes do fluxo, incluindo:

```text
gateway.request.inicio
gateway.request.fim
http.contrato.criar
contrato.criacao.inicio
contrato.dominio.criado
contrato.persistence.save.inicio
contrato.persistence.save.sucesso
contrato.evento.pendente
contrato.criacao.sucesso
reputacao.atualizacao.inicio
reputacao.atualizacao.sucesso
auditoria.registro.inicio
auditoria.registro.sucesso
```

A presença do `correlationId` nas chamadas HTTP permite relacionar logs produzidos durante uma mesma requisição.

## Infraestrutura local

Os serviços de infraestrutura estão definidos em:

```text
infra/docker-compose.yml
```

Para iniciar o ambiente:

```bash
cd infra
docker compose up -d
```

Para verificar os containers:

```bash
docker compose ps
```

Para encerrar:

```bash
docker compose down
```

Os dados do PostgreSQL são mantidos em volume Docker.

Para remover também os dados persistidos:

```bash
docker compose down -v
```

## Execução das aplicações

A partir da raiz do projeto, cada módulo pode ser iniciado separadamente com Maven.

Eureka Server:

```bash
mvn -pl eureka-server spring-boot:run
```

API Gateway:

```bash
mvn -pl api-gateway spring-boot:run
```

Contrato Service:

```bash
mvn -pl contrato-service spring-boot:run
```

Notificação Service:

```bash
mvn -pl notificacao-service spring-boot:run
```

Reputação Service:

```bash
mvn -pl reputacao-service spring-boot:run
```

Auditoria Service:

```bash
mvn -pl auditoria-service spring-boot:run
```

## Portas

| Componente | Porta |
|---|---:|
| API Gateway | `8080` |
| contrato-service | `8081` |
| notificacao-service | `8082` |
| reputacao-service | `8083` |
| auditoria-service | `8084` |
| Eureka Server | `8761` |
| Kafka | `9092` |
| Kafka UI | `8090` |
| PostgreSQL | `5432` |

## Teste básico

Com a infraestrutura e as aplicações em execução, um contrato pode ser criado pelo Gateway:

```bash
curl -i -X POST http://localhost:8080/api/contratos \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-Id: teste-contrato-001' \
  -d '{
    "clienteId": "11111111-1111-1111-1111-111111111111",
    "freelancerId": "22222222-2222-2222-2222-222222222222",
    "titulo": "Construção de API de pagamentos",
    "valor": 3500.00
  }'
```

Consulta dos contratos:

```bash
curl http://localhost:8080/api/contratos
```

Consulta de um contrato específico:

```bash
curl http://localhost:8080/api/contratos/{id}
```

## Implementações Realizadas para o AT

Abaixo está o detalhamento completo de todas as implementações realizadas no projeto para atender aos requisitos do AT.

---

### 1. Comunicação Baseada em Eventos e Publicação Transacional (Transactional Outbox)

* **Publicação Transacional (Transactional Outbox Pattern):**
  * Para evitar perda de eventos e inconsistência entre o banco de dados relacional e o Kafka (problema de *Dual-Write*), o `contrato-service` utiliza a tabela `outbox_events`.
  * Toda mutação de estado no Aggregate `Contrato` (`ContratoCriado`, `EntregaRegistrada`, `ContratoConcluido`, `ContratoCancelado`) é persistida atomicamente na mesma transação `@Transactional` do banco `contrato_db`.
  * Um método assíncrono com `@Scheduled` (`OutboxPublisher`) consulta periodicamente os registros com status `PENDENTE`, publica as mensagens no Kafka via `ContratoProducer` e atualiza o status do outbox para `PUBLICADO`.
* **Desacoplamento:**
  * Não há chamadas HTTP diretas síncronas entre o `contrato-service` e os serviços auxiliares. Toda a integração é orientada a eventos via Kafka.

---

### 2. Integração dos Serviços Consumidores

Os serviços auxiliares reagem de forma autônoma aos eventos consumidos do tópico Kafka:

* **`notificacao-service` (`NotificacaoKafkaConsumer`):**
  * Consome o tópico `contratos.eventos` sob o consumer group `notificacao-group`.
  * Reage a todos os ciclos de vida do contrato, gerando notificações para as partes envolvidas:
    * `ContratoCriado` $\rightarrow$ notifica o Freelancer.
    * `EntregaRegistrada` $\rightarrow$ notifica o Cliente para validação da entrega.
    * `ContratoConcluido` $\rightarrow$ notifica o Freelancer sobre a conclusão do trabalho.
    * `ContratoCancelado` $\rightarrow$ notifica o Freelancer sobre o cancelamento.
* **`reputacao-service` (`ReputacaoKafkaConsumer`):**
  * Consome o tópico `contratos.eventos` sob o consumer group `reputacao-group`.
  * Reage ao evento `ContratoConcluido`, atualizando incrementalmente no `reputacao_db` o número de contratos concluídos e o valor financeiro total acumulado pelo freelancer.
* **`auditoria-service` (`AuditoriaKafkaConsumer`):**
  * Consome o tópico `contratos.eventos` sob o consumer group `auditoria-group`.
  * Registra imutavelmente todos os eventos recebidos no `auditoria_db`, armazenando `eventId`, `aggregateId`, `eventType`, `correlationId`, `payload` JSON completo e timestamp de recebimento.

---

### 3. Especificação das Mensagens e Contratos de Eventos

* **Tópico Principal:** `contratos.eventos`
* **Produtor:** `contrato-service`
* **Consumidores:** `notificacao-service`, `reputacao-service`, `auditoria-service`
* **Chave de Publicação (Partition Key):** `contratoId` (UUID do contrato em formato String).

#### Estrutura do Payload (`ContratoEventoDTO`):

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `eventId` | `UUID` | Sim | Identificador único global do evento para idempotência |
| `tipoEvento` | `String` | Sim | Nome do evento (`ContratoCriado`, `EntregaRegistrada`, `ContratoConcluido`, `ContratoCancelado`) |
| `contratoId` | `UUID` | Sim | Identificador único do contrato (Aggregate ID) |
| `occurredOn` | `Instant` | Sim | Data e hora UTC em que o evento ocorreu |
| `correlationId` | `String` | Sim | Identificador de correlação ponta a ponta da operação |
| `clienteId` | `UUID` | Condicional | Identificador do cliente contratante |
| `freelancerId` | `UUID` | Condicional | Identificador do freelancer contratado |
| `titulo` | `String` | Condicional | Título descritivo do contrato de trabalho |
| `valor` | `BigDecimal` | Condicional | Valor financeiro acordado no contrato |
| `novoStatus` | `String` | Não | Novo status assumido pelo contrato |

#### Exemplo de Mensagem JSON (`ContratoCriado`):

```json
{
  "eventId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "tipoEvento": "ContratoCriado",
  "contratoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "occurredOn": "2026-10-01T20:00:00Z",
  "correlationId": "req-corr-98765",
  "clienteId": "11111111-1111-1111-1111-111111111111",
  "freelancerId": "22222222-2222-2222-2222-222222222222",
  "titulo": "Desenvolvimento de Microsserviços",
  "valor": 4500.00,
  "novoStatus": "ATIVO"
}
```

---

### 4. Processamento Concorrente e Garantia de Ordenação

* **Estratégia de Particionamento:**
  * O `ContratoProducer` envia explicitamente o `contratoId.toString()` como a **chave da mensagem Kafka**.
  * O algoritmo do Kafka garante que todas as mensagens com a mesma ch ave sejam encaminhadas para a **mesma partição**.
* **Preservação da Ordem por Contrato:**
  * Dentro de uma mesma partição do Kafka, a ordem estrita dos registros é preservada.
  * O ciclo de vida (`ContratoCriado` $\rightarrow$ `EntregaRegistrada` $\rightarrow$ `ContratoConcluido`) é processado na sequência correta para um mesmo contrato.
* **Processamento:**
  * Contratos distintos possuem chaves diferentes, sendo distribuídos entre diferentes partições do tópico, o que permite o consumo paralelo entre múltiplas instâncias ou consumidores.

---

### 5. Tratamento de Mensagens Duplicadas e Idempotência

* **Padrão de Idempotência (Tabela de Eventos Processados):**
  * Os serviços consumidores (`notificacao-service`, `reputacao-service` e `auditoria-service`) possuem controle de idempotência baseado no identificador único do evento (`eventId`).
  * Antes de executar a regra de negócio, o serviço verifica se o `eventId` já existe na tabela `eventos_processados` (ou registro de auditoria).
  * Caso já exista:
    * O processamento é abortado.
    * Um log de aviso no formato `[servico].evento.duplicado.ignorado eventId=... contratoId=...` é registrado.
  * Caso não exista:
    * A operação de negócio é executada e o `eventId` é persistido na tabela `eventos_processados`.

---

### 6. Logs Padronizados da Aplicação

* Todos os microsserviços implementam logging contextual e estruturado utilizando SLF4J / Logback:
  * Início de requisições HTTP e consumo de eventos: `[fluxo].inicio`
  * Sucesso nas etapas de negócio e persistência: `[fluxo].sucesso`
  * Erros e exceções tratadas: `[fluxo].erro`
  * Detecção de duplicidades: `[fluxo].duplicado.ignorado`
* **Campos Obrigatórios nos Logs:**
  * `service`: Nome do microsserviço emissor.
  * `correlationId`: Identificador da transação ponta a ponta (injetado via `MDC`).
  * `contratoId` / `aggregateId`: Identificador do contrato.
  * `eventId`: Identificador do evento.
  * `tipoEvento`: Nome do evento de domínio.

---

### 7. Centralização de Logs (Graylog)

* **Arquitetura de Observabilidade de Logs:**
  * O Docker Compose inclui o **Graylog**, integrado com **OpenSearch** e **MongoDB**.
  * Cada microsserviço Spring Boot utiliza a biblioteca `logback-gelf` para streaming automático de logs via protocolo GELF UDP/TCP na porta `12201`.
* **Consulta Unificada:**
  * É possível pesquisar todas as mensagens correlacionadas de múltiplos serviços na interface do Graylog (`http://localhost:9000`) utilizando consultas.
---

### 8. Rastreamento Distribuído (Distributed Tracing com Zipkin)

* **Micrometer Tracing & OpenTelemetry/Brave:**
  * Cada aplicação possui as dependências do `spring-boot-starter-actuator` e `micrometer-tracing-bridge-brave` configuradas para exportação ao **Zipkin** (`http://localhost:9411`).
* **Propagação de Contexto:**
  * O contexto do trace (`traceId`, `spanId`) é propagado tanto nas chamadas HTTP via API Gateway quanto através dos cabeçalhos dos registros do Kafka.
  * A interface do Zipkin permite visualizar o histórico de spans completo da transação, demonstrando o tempo gasto em cada serviço e a passagem pelo Kafka.

---

### 9. Correlação das Operações Ponta a Ponta

A rastreabilidade completa de uma operação é mantida através do identificador de correlação:

```text
API Gateway (RequestLoggingFilter: gera ou repassa X-Correlation-Id)
    ↓ [HTTP Header]
contrato-service (ContratoController: recebe header e alimenta MDC)
    ↓ [Transação / Outbox]
contrato_db (Tabela outbox_events armazena correlationId no payload)
    ↓ [Assíncrono: OutboxPublisher]
Kafka (Mensagem publicada no tópico contratos.eventos)
    ↓ [Consumo de Mensagens]
serviços consumidores (Auditoria, Notificacao, Reputacao: extraem correlationId e alimentam MDC)
```
---

### 10. Tratamento de Falhas e Resiliência

* **Isolamento de Erros:**
  * O consumo de cada mensagem possui blocos estruturados de tratamento de exceções. Falhas no processamento de mensagens individuais são capturadas e registradas com nível `ERROR`, preservando o contexto e o identificador do evento.
---

### 11. Infraestrutura Completa (Docker Compose)

O arquivo `infra/docker-compose.yml` provê toda a stack necessária em containers orquestrados:

* **PostgreSQL 16:** Bancos dedicados `contrato_db`, `notificacao_db`, `reputacao_db` e `auditoria_db` (porta `5432`).
* **Apache Kafka 4.2 (KRaft Mode):** Broker de mensageria sem dependência do ZooKeeper (porta `9092` externa, `19092` interna).
* **Kafka UI:** Interface gráfica para inspeção de tópicos, partições e mensagens (porta `8090`).
* **OpenSearch 2.13:** Mecanismo de busca e armazenamento de logs indexados (porta `9200`).
* **MongoDB 6.0:** Banco de metadados e configurações do Graylog (porta `27017`).
* **Graylog 6.0:** Painel de centralização e busca de logs via GELF (porta `9000` web, `12201` GELF).
* **Zipkin:** Coletor e visualizador de traces distribuídos (porta `9411`).

---

## Tecnologias

```text
Java 21
Spring Boot 4.1
Spring Cloud
Spring Cloud Gateway
Netflix Eureka
Spring Data JPA
PostgreSQL 16
Apache Kafka 4
Docker Compose
Maven
```
