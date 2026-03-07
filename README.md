# Lume

Aplicação full-stack moderna composta por **Frontend React**, **Backend Java 21 (Spring Boot 3)** e banco de dados **PostgreSQL**.

---

## Arquitetura

```
Lume/
├── backend/          # API REST - Java 21, Spring Boot 3, JPA, Flyway
├── frontend/         # SPA - React 18, TypeScript, Tailwind CSS, Vite
├── docker-compose.yml
└── README.md
```

## Tecnologias

### Backend

| Tecnologia | Versão | Descrição |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.3.5 | Framework web |
| Spring Data JPA | 3.3.x | Persistência de dados |
| PostgreSQL | 16 | Banco de dados relacional |
| Flyway | 10.x | Migrações de banco |
| SpringDoc OpenAPI | 2.6.0 | Documentação Swagger |
| Lombok | 1.18.x | Redução de boilerplate |
| Maven | 3.9.x | Gerenciamento de dependências |

### Frontend

| Tecnologia | Versão | Descrição |
|---|---|---|
| React | 18.3 | Biblioteca de UI |
| TypeScript | 5.6 | Tipagem estática |
| Vite | 5.4 | Build tool |
| Tailwind CSS | 3.4 | Framework de estilos |
| React Router | 6.28 | Roteamento SPA |
| Axios | 1.7 | Cliente HTTP |
| React Hot Toast | 2.4 | Notificações |
| React Icons | 5.3 | Ícones |

---

## Pré-requisitos

- **Docker** e **Docker Compose** (recomendado)
- Ou, para desenvolvimento local:
  - **Java 21** (JDK)
  - **Maven 3.9+**
  - **Node.js 22+** e **pnpm**
  - **PostgreSQL 16+**

---

## Início Rápido com Docker

```bash
# Clonar o repositório
git clone https://github.com/FELIPEACASTRO/Lume.git
cd Lume

# Subir toda a stack
docker compose up -d

# Acessar a aplicação
# Frontend: http://localhost:3000
# Backend API: http://localhost:8080/api
# Swagger UI: http://localhost:8080/api/swagger-ui.html
```

---

## Desenvolvimento Local

### Banco de Dados

```bash
# Subir apenas o PostgreSQL via Docker
docker compose up -d postgres
```

### Backend

```bash
cd backend

# Executar a aplicação
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# A API estará disponível em http://localhost:8080/api
```

### Frontend

```bash
cd frontend

# Instalar dependências
pnpm install

# Executar em modo de desenvolvimento
pnpm dev

# O frontend estará disponível em http://localhost:5173
```

---

## Endpoints da API

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/users` | Listar usuários (paginado) |
| `GET` | `/api/users/{id}` | Buscar usuário por ID |
| `POST` | `/api/users` | Criar novo usuário |
| `PUT` | `/api/users/{id}` | Atualizar usuário |
| `DELETE` | `/api/users/{id}` | Desativar usuário (soft delete) |
| `GET` | `/api/health` | Health check da API |

A documentação completa da API está disponível via **Swagger UI** em:
`http://localhost:8080/api/swagger-ui.html`

---

## Estrutura do Projeto

### Backend

```
backend/
├── src/main/java/com/lume/
│   ├── config/           # Configurações (CORS, OpenAPI)
│   ├── controller/       # Controllers REST
│   ├── dto/              # Data Transfer Objects
│   ├── exception/        # Exceções e handlers globais
│   ├── model/            # Entidades JPA
│   ├── repository/       # Repositórios Spring Data
│   ├── service/          # Camada de serviço (regras de negócio)
│   └── LumeApplication.java
├── src/main/resources/
│   ├── db/migration/     # Scripts Flyway
│   ├── application.yml
│   ├── application-dev.yml
│   └── application-test.yml
├── Dockerfile
└── pom.xml
```

### Frontend

```
frontend/
├── public/               # Arquivos estáticos
├── src/
│   ├── assets/           # Imagens e recursos
│   ├── components/       # Componentes reutilizáveis
│   │   ├── common/       # Componentes genéricos
│   │   ├── layout/       # Header, Footer, Layout
│   │   └── users/        # Componentes de usuários
│   ├── hooks/            # Custom hooks
│   ├── pages/            # Páginas da aplicação
│   ├── routes/           # Configuração de rotas
│   ├── services/         # Serviços HTTP (API)
│   ├── styles/           # Estilos globais
│   ├── types/            # Tipos TypeScript
│   ├── utils/            # Utilitários
│   ├── App.tsx
│   └── main.tsx
├── Dockerfile
├── nginx.conf
└── package.json
```

---

## Variáveis de Ambiente

Copie o arquivo `.env.example` para `.env` e ajuste conforme necessário:

```bash
cp .env.example .env
```

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `lume_db` | Nome do banco de dados |
| `DB_USERNAME` | `lume_user` | Usuário do banco |
| `DB_PASSWORD` | `lume_pass` | Senha do banco |
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil ativo do Spring |
| `VITE_API_URL` | `/api` | URL base da API no frontend |

---

## Licença

Este projeto é privado e de uso exclusivo.
