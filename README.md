# FlowPay - Distribuição e Monitoramento de Atendimentos

Aplicação full stack desenvolvida para o desafio técnico da FlowPay. O sistema distribui atendimentos entre agentes por categoria, respeita limite de atendimentos simultâneos e oferece um painel de monitoramento de fila em tempo real.

## Funcionalidades

- Cadastro, edição, exclusão, pausa e retorno de agentes.
- Associação de agentes a uma ou mais categorias:
  - Problemas com cartão
  - Contratação de empréstimo
  - Outros assuntos
- Criação de atendimentos por categoria.
- Distribuição automática para agente elegível.
- Limite de 3 atendimentos simultâneos por agente.
- Fila de espera quando todos os agentes elegíveis estão no limite.
- Redistribuição automática da fila quando um atendimento é finalizado.
- Painel com atualização em tempo real via Server-Sent Events (SSE).
- Resumo do dia com Service Level, total de atendimentos e clientes que entraram em espera.
- Indicadores operacionais de agentes logados, disponíveis, ocupados, clientes na fila e clientes em atendimento.
- Controle de pausa para agentes disponíveis, com contador de tempo em pausa.
- Histórico dos agentes com atendimentos do dia, tempo médio de atendimento, número de pausas e tempo total em pausa.
- Validação de nomes para aceitar apenas letras e espaços.
- Layout responsivo para desktop e smartphone.

## Stack

### Back-end

- Java 21
- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven

### Front-end

- React
- TypeScript
- Vite
- Axios
- Tailwind CSS

### Infra local

- Docker Compose para PostgreSQL

## Estrutura do projeto

```text
flowpay-system/
  backend/    API REST, regra de distribuição, persistência e SSE
  frontend/   Painel React para operação e monitoramento
```

## Organização do back-end

O back-end segue uma separação por responsabilidades:

```text
application/  Casos de uso, DTOs e mapeadores
domain/       Entidades, enums e repositórios
interfaces/   Controllers REST e tratamento de erros
shared/       Exceções e recursos compartilhados
```

Essa estrutura se aproxima de uma organização em camadas/clean architecture. Em Java, os pacotes usam nomes em minúsculo por convenção, por exemplo `interfaces.controller`.

## Como rodar localmente

### Pré-requisitos

- Java 21+
- Maven
- Node.js
- Docker Desktop

### 1. Subir o banco de dados

```bash
cd backend
docker compose up -d postgres
```

O PostgreSQL ficará disponível em:

```text
localhost:5433
database: flowpay
user: postgres
password: postgres
```

### 2. Rodar o back-end

```bash
cd backend
mvn spring-boot:run
```

API disponível em:

```text
http://localhost:8080
```

### 3. Rodar o front-end

Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

Painel disponível em:

```text
http://localhost:5173
```

## Configurações

O back-end aceita variáveis de ambiente para banco e CORS:

```text
DB_URL=jdbc:postgresql://localhost:5433/flowpay
DB_USERNAME=postgres
DB_PASSWORD=postgres
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

O front-end usa `http://localhost:8080` por padrão. Para alterar:

```text
VITE_API_BASE_URL=http://localhost:8080
```

## Regras de distribuição

1. O atendimento é criado com uma categoria.
2. O back-end procura agentes que atendem aquela categoria e ainda possuem capacidade.
3. Agentes em pausa ou inativos não recebem novos atendimentos.
4. Se houver agentes sem atendimento em andamento, o atendimento vai para quem está disponível há mais tempo.
5. Se todos os agentes elegíveis já estiverem atendendo, mas ainda houver capacidade, o atendimento vai para quem recebeu menos atendimentos no dia.
6. Se nenhum agente elegível tiver capacidade, o atendimento entra na fila e o Service Level do dia é impactado.
7. Quando um atendimento é finalizado, o sistema tenta redistribuir automaticamente o próximo item da fila da mesma categoria.

## Endpoints principais

### Agentes

```http
GET    /api/attendants
POST   /api/attendants
PUT    /api/attendants/{id}
DELETE /api/attendants/{id}
PATCH  /api/attendants/{id}/pause
PATCH  /api/attendants/{id}/resume
```

### Atendimentos

```http
GET   /api/service-requests
GET   /api/service-requests/queue
POST  /api/service-requests
PATCH /api/service-requests/{id}/finish
```

### Dashboard

```http
GET /api/dashboard/summary
GET /api/dashboard/stream
```

O endpoint `/api/dashboard/stream` usa SSE para enviar atualizações em tempo real ao painel.

## Exemplos de payload

### Criar agente

```json
{
  "name": "João Silva",
  "categories": ["CARD_ISSUES", "OTHER_SUBJECTS"]
}
```

### Criar atendimento

```json
{
  "customerName": "Maria Santos",
  "category": "LOAN_CONTRACTING"
}
```

## Testes e build

### Back-end

```bash
cd backend
mvn test
```

### Front-end

```bash
cd frontend
npm run build
```

## Observações técnicas

- O Flyway versiona a estrutura inicial do banco, dados de exemplo e evoluções de métricas/pausas.
- O status do agente é recalculado com base na quantidade de atendimentos em andamento.
- A exclusão de agente é bloqueada quando há atendimentos em andamento para evitar inconsistência operacional.
- As pausas são registradas em tabela própria para permitir histórico diário.
- O painel combina chamadas REST com SSE para manter a tela atualizada em tempo real.
