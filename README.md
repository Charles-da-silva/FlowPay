# FlowPay - Distribuição e Monitoramento de Atendimentos

Aplicação full stack desenvolvida para o desafio técnico da FlowPay. O sistema distribui atendimentos entre atendentes por categoria, respeita limite de atendimentos simultâneos e oferece um dashboard para acompanhamento em tempo real.

## Funcionalidades

- Cadastro, edição e exclusão de atendentes.
- Associação de atendentes a uma ou mais categorias:
  - Problemas com cartão
  - Contratação de empréstimo
  - Outros assuntos
- Criação de atendimentos por categoria.
- Distribuição automática para atendente elegível com menor carga.
- Limite de 3 atendimentos simultâneos por atendente.
- Fila de espera quando todos os atendentes elegíveis estão no limite.
- Redistribuição automática da fila quando um atendimento é finalizado.
- Dashboard com atualização em tempo real via Server-Sent Events (SSE).
- Visualização de atendimentos em andamento, fila, status e carga atual dos atendentes.

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
  frontend/   Dashboard React para operação e monitoramento
```

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

Dashboard disponível em:

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
2. O back-end procura atendentes que atendem aquela categoria e ainda possuem capacidade.
3. O atendimento é atribuído ao atendente elegível com menor número de atendimentos em andamento.
4. Se nenhum atendente elegível tiver capacidade, o atendimento entra na fila.
5. Quando um atendimento é finalizado, o sistema tenta redistribuir automaticamente o próximo item da fila da mesma categoria.

## Endpoints principais

### Atendentes

```http
GET    /api/attendants
POST   /api/attendants
PUT    /api/attendants/{id}
DELETE /api/attendants/{id}
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

O endpoint `/api/dashboard/stream` usa SSE para enviar atualizações em tempo real ao dashboard.

## Exemplos de payload

### Criar atendente

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

- O Flyway versiona a estrutura inicial do banco e dados de exemplo.
- O status do atendente é recalculado com base na quantidade de atendimentos em andamento.
- A exclusão de atendente é bloqueada quando há atendimentos em andamento para evitar inconsistência operacional.
- O dashboard combina chamadas REST com SSE para manter a tela atualizada em tempo real.
