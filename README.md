# Lume

Aplicação full-stack moderna construída com **React 18**, **Java 21 (Spring Boot 3)** e **PostgreSQL 16**, seguindo rigorosamente os princípios de **Clean Architecture**, **Clean Code**, **SOLID**, **Design Patterns** e **Microservices Patterns**.

---

## Sumário

1. [Arquitetura](#arquitetura)
2. [Padrões e Princípios Aplicados](#padrões-e-princípios-aplicados)
3. [Tecnologias](#tecnologias)
4. [Estrutura do Projeto](#estrutura-do-projeto)
5. [Pré-requisitos](#pré-requisitos)
6. [Como Executar](#como-executar)
7. [Endpoints da API](#endpoints-da-api)
8. [Testes e Cobertura](#testes-e-cobertura)
9. [Análise Assintótica (Big O)](#análise-assintótica-big-o)

---

## Arquitetura

O backend segue a **Clean Architecture** (Robert C. Martin), organizando o código em quatro camadas com regra de dependência unidirecional de fora para dentro:

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION                         │
│  Controllers REST (CQRS: Command + Query Controllers)   │
│  GlobalExceptionHandler, ApiResponse                    │
├─────────────────────────────────────────────────────────┤
│                    APPLICATION                          │
│  Use Cases (Ports), Command/Query Handlers (CQRS)       │
│  DTOs (ACL), Mappers, Commands, Queries                 │
├─────────────────────────────────────────────────────────┤
│                      DOMAIN                             │
│  Entidades, Exceções, Interfaces de Repositório         │
│  Factory, Strategy (Validação), PasswordEncoder         │
├─────────────────────────────────────────────────────────┤
│                   INFRASTRUCTURE                        │
│  JPA Entities, Repository Adapters, BCrypt Adapter      │
│  Bean Configuration (Singleton), CORS, OpenAPI          │
└─────────────────────────────────────────────────────────┘
```

A **regra de dependência** garante que camadas internas nunca conhecem camadas externas. O domínio é puro e livre de frameworks.

O frontend segue uma arquitetura baseada em **separação de concerns**: componentes de apresentação, custom hooks para lógica de estado, serviços para comunicação HTTP, e tipos centralizados.

---

## Padrões e Princípios Aplicados

### Clean Architecture

A solução implementa as quatro camadas da Clean Architecture com separação rigorosa de responsabilidades. A camada de **Domain** contém entidades puras sem dependências de frameworks. A camada de **Application** define os use cases através de ports (interfaces) e handlers. A camada de **Infrastructure** fornece implementações concretas (JPA, BCrypt). A camada de **Presentation** expõe os endpoints REST.

### SOLID

| Princípio | Aplicação na Solução |
|---|---|
| **SRP** (Single Responsibility) | Cada classe tem uma única responsabilidade: `CreateUserCommandHandler` apenas cria, `UserMapper` apenas mapeia, `EmailValidationStrategy` apenas valida e-mail |
| **OCP** (Open/Closed) | Novas validações são adicionadas criando novas implementações de `ValidationStrategy<T>` sem modificar código existente |
| **LSP** (Liskov Substitution) | Todas as implementações de `ValidationStrategy<T>` e `UserRepositoryPort` são substituíveis sem alterar o comportamento |
| **ISP** (Interface Segregation) | `UserCommandUseCase` e `UserQueryUseCase` são interfaces segregadas; controllers dependem apenas da interface que utilizam |
| **DIP** (Dependency Inversion) | O domínio define `PasswordEncoder` e `UserRepositoryPort` como abstrações; a infraestrutura implementa com `BCryptPasswordEncoderAdapter` e `UserRepositoryAdapter` |

### Design Patterns

| Pattern | Implementação |
|---|---|
| **Factory** | `UserFactory` centraliza a criação de entidades `User` com validação e codificação de senha. `User.Builder` implementa o padrão Builder para construção fluente |
| **Strategy** | `ValidationStrategy<T>` define a interface; `EmailValidationStrategy`, `NameValidationStrategy` e `PasswordValidationStrategy` são implementações intercambiáveis |
| **Singleton** | Todos os beans Spring são Singletons por padrão, configurados em `BeanConfig`. Uma única instância de cada handler, factory e adapter é compartilhada |
| **Adapter** | `UserRepositoryAdapter` adapta Spring Data JPA para `UserRepositoryPort`. `BCryptPasswordEncoderAdapter` adapta BCrypt para `PasswordEncoder` do domínio |
| **Facade** | `UserCommandService` e `UserQueryService` simplificam a interface para os controllers, delegando para handlers especializados |

### Microservices Patterns

| Pattern | Implementação |
|---|---|
| **CQRS** (Command Query Responsibility Segregation) | Operações de escrita (`CreateUserCommand`, `UpdateUserCommand`) e leitura (`GetUserByIdQuery`, `ListUsersQuery`) são completamente separadas em handlers, services e controllers distintos |
| **ACL** (Anti-Corruption Layer) | DTOs (`UserRequestDTO`, `UserResponseDTO`) isolam o modelo de domínio da representação externa. `UserMapper` e `UserPersistenceMapper` traduzem entre camadas, evitando contaminação |

### Clean Code

O código segue as práticas de Clean Code: nomes significativos e descritivos em todas as classes e métodos; funções pequenas com responsabilidade única; ausência de comentários desnecessários (o código é autoexplicativo); uso de Java Records (Java 21) para imutabilidade de DTOs e Commands; tratamento adequado de exceções com hierarquia clara (`DomainException` → `BusinessRuleException`, `ResourceNotFoundException`).

### Abstração, Acoplamento, Extensibilidade e Coesão

A solução maximiza **coesão** agrupando responsabilidades relacionadas (cada pacote tem um propósito claro) e minimiza **acoplamento** através de interfaces e inversão de dependência. A **extensibilidade** é garantida pelo padrão Strategy (novas validações) e pela Clean Architecture (novas funcionalidades não afetam o domínio). A **abstração** é aplicada em todos os contratos entre camadas via interfaces.

---

## Tecnologias

### Backend

| Tecnologia | Versão | Finalidade |
|---|---|---|
| Java | 21 | Linguagem principal (Records, Pattern Matching) |
| Spring Boot | 3.3.5 | Framework web e IoC Container |
| Spring Data JPA | 3.3.x | Persistência de dados |
| PostgreSQL | 16 | Banco de dados relacional |
| Flyway | 10.x | Migrações de banco de dados |
| SpringDoc OpenAPI | 2.6.0 | Documentação Swagger/OpenAPI |
| Spring Security Crypto | 6.x | BCrypt para hash de senhas |
| JaCoCo | 0.8.12 | Cobertura de testes (Code Coverage) |
| JUnit 5 | 5.10.x | Framework de testes |
| Mockito | 5.x | Mocking para testes unitários |
| Maven | 3.9.x | Build e gerenciamento de dependências |

### Frontend

| Tecnologia | Versão | Finalidade |
|---|---|---|
| React | 18.3 | Biblioteca de UI |
| TypeScript | 5.6 | Tipagem estática |
| Vite | 5.4 | Build tool |
| Tailwind CSS | 3.4 | Framework de estilos utilitários |
| React Router | 6.28 | Roteamento SPA |
| Axios | 1.7 | Cliente HTTP |
| React Hot Toast | 2.4 | Notificações |
| React Icons | 5.3 | Biblioteca de ícones |

---

## Estrutura do Projeto

```
Lume/
├── backend/
│   ├── src/main/java/com/lume/
│   │   ├── domain/                    # Camada de Domínio (núcleo puro)
│   │   │   ├── model/                 #   Entidades de domínio
│   │   │   ├── exception/             #   Exceções de domínio
│   │   │   ├── service/               #   Interfaces de serviço (DIP)
│   │   │   ├── validation/            #   Strategy Pattern (validações)
│   │   │   └── factory/               #   Factory Pattern (criação)
│   │   ├── application/               # Camada de Aplicação (use cases)
│   │   │   ├── command/               #   CQRS Commands
│   │   │   ├── query/                 #   CQRS Queries
│   │   │   ├── handler/command/       #   Command Handlers
│   │   │   ├── handler/query/         #   Query Handlers
│   │   │   ├── dto/request/           #   ACL - DTOs de entrada
│   │   │   ├── dto/response/          #   ACL - DTOs de saída
│   │   │   ├── mapper/                #   ACL - Mappers
│   │   │   └── port/input|output/     #   Ports (interfaces)
│   │   ├── infrastructure/            # Camada de Infraestrutura
│   │   │   ├── persistence/           #   JPA Entities, Adapters, Mappers
│   │   │   ├── config/                #   Bean Config (Singleton), CORS
│   │   │   └── security/              #   BCrypt Adapter
│   │   └── presentation/              # Camada de Apresentação
│   │       ├── controller/            #   REST Controllers (CQRS)
│   │       ├── advice/                #   Exception Handlers
│   │       └── response/              #   Respostas padronizadas
│   ├── src/main/resources/
│   │   ├── db/migration/              # Scripts Flyway
│   │   ├── application.yml            # Configuração principal
│   │   ├── application-dev.yml        # Perfil de desenvolvimento
│   │   └── application-test.yml       # Perfil de testes (H2)
│   ├── src/test/java/com/lume/
│   │   ├── domain/                    # Testes unitários do domínio
│   │   ├── application/               # Testes unitários dos handlers
│   │   ├── infrastructure/            # Testes dos mappers de persistência
│   │   └── presentation/              # Testes de integração (controllers)
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/                # Componentes React reutilizáveis
│   │   │   ├── common/                #   Loading, EmptyState
│   │   │   ├── layout/                #   Header, Footer, Layout
│   │   │   └── users/                 #   UserForm, UserTable
│   │   ├── hooks/                     # Custom Hooks (lógica de estado)
│   │   ├── pages/                     # Páginas da aplicação
│   │   ├── routes/                    # Configuração de rotas
│   │   ├── services/                  # Serviços HTTP (API)
│   │   ├── styles/                    # Estilos globais (Tailwind)
│   │   ├── types/                     # Tipos TypeScript
│   │   └── utils/                     # Utilitários (formatação)
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml
└── README.md
```

---

## Pré-requisitos

Para execução com **Docker** (recomendado): Docker e Docker Compose instalados.

Para desenvolvimento local: Java 21 (JDK), Maven 3.9+, Node.js 22+ com pnpm, e PostgreSQL 16+.

---

## Como Executar

### Com Docker Compose (recomendado)

```bash
git clone https://github.com/FELIPEACASTRO/Lume.git
cd Lume
docker compose up -d
```

Após a inicialização, os serviços estarão disponíveis nos seguintes endereços:

| Serviço | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| PostgreSQL | localhost:5432 |

### Desenvolvimento Local

```bash
# 1. Subir apenas o PostgreSQL
docker compose up -d postgres

# 2. Backend
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Frontend (em outro terminal)
cd frontend
pnpm install
pnpm dev
```

---

## Endpoints da API

### Comandos (Escrita) - CQRS

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/users` | Criar novo usuário |
| `PUT` | `/api/users/{id}` | Atualizar usuário existente |
| `DELETE` | `/api/users/{id}` | Desativar usuário (soft delete) |

### Queries (Leitura) - CQRS

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/users` | Listar usuários (paginado) |
| `GET` | `/api/users/{id}` | Buscar usuário por ID |

### Utilitários

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/health` | Health check da API |

A documentação interativa completa está disponível via **Swagger UI** em `http://localhost:8080/api/swagger-ui.html`.

---

## Testes e Cobertura

### Executar Testes

```bash
cd backend

# Testes unitários
mvn test

# Testes de integração
mvn verify

# Gerar relatório de cobertura (JaCoCo)
mvn test jacoco:report
# Relatório em: target/site/jacoco/index.html
```

### Estrutura de Testes

| Tipo | Localização | Descrição |
|---|---|---|
| **Unitário** | `domain/UserTest` | Entidade User, Builder, validações de domínio |
| **Unitário** | `domain/ValidationStrategyTest` | Strategy Pattern (Email, Name, Password) |
| **Unitário** | `domain/UserFactoryTest` | Factory Pattern com mock de PasswordEncoder |
| **Unitário** | `application/CreateUserCommandHandlerTest` | Handler de criação com mocks |
| **Unitário** | `application/UpdateUserCommandHandlerTest` | Handler de atualização com mocks |
| **Unitário** | `application/DeleteUserCommandHandlerTest` | Handler de exclusão com mocks |
| **Unitário** | `application/QueryHandlersTest` | Handlers de consulta com mocks |
| **Unitário** | `application/UserMapperTest` | Mapper ACL entre camadas |
| **Unitário** | `infrastructure/UserPersistenceMapperTest` | Mapper entre domínio e JPA |
| **Integração** | `presentation/UserControllerIT` | Fluxo completo Controller → Service → Repository → H2 |

### Cobertura de Código (JaCoCo)

O plugin JaCoCo está configurado para gerar relatórios de cobertura automaticamente na fase de testes. O threshold mínimo configurado é de **70% de cobertura de linhas**. Classes de configuração e a classe principal são excluídas da análise.

---

## Análise Assintótica (Big O)

A tabela abaixo documenta a complexidade computacional das operações principais, considerando n como o número total de registros no banco de dados e p como o tamanho da página.

| Operação | Complexidade | Justificativa |
|---|---|---|
| Criar usuário | O(log n) | Verificação de unicidade de e-mail via índice B-tree + inserção |
| Buscar por ID | O(log n) | Busca por chave primária (índice B-tree) |
| Buscar por e-mail | O(log n) | Busca por índice no campo email |
| Listar paginado | O(p + log n) | OFFSET/LIMIT com índice; p itens retornados por página |
| Atualizar usuário | O(log n) | Busca por ID + verificação de e-mail + atualização |
| Desativar usuário | O(log n) | Busca por ID + atualização de flag |
| Validação (Strategy) | O(k) | k = número de caracteres do campo validado (regex matching) |
| Mapeamento (ACL) | O(1) | Conversão direta campo a campo |
| Hash de senha (BCrypt) | O(1) | Custo fixo do algoritmo (fator 12) |

---

## Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `lume_db` | Nome do banco de dados |
| `DB_USERNAME` | `lume_user` | Usuário do banco |
| `DB_PASSWORD` | `lume_pass` | Senha do banco |
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil ativo do Spring |
| `VITE_API_URL` | `/api` | URL base da API no frontend |
| `OPENAI_API_KEY` | vazio | Credencial do provider OpenAI |
| `GEMINI_API_KEY` | vazio | Credencial do provider Gemini |
| `DEEPSEEK_API_KEY` | vazio | Credencial do provider DeepSeek |
| `ANTHROPIC_API_KEY` | vazio | Credencial do provider Anthropic / Claude |
| `XAI_API_KEY` | vazio | Credencial do provider xAI / Grok |
| `PERPLEXITY_API_KEY` | vazio | Credencial do provider Perplexity |
| `RUN_REAL_AI_TESTS` | `false` | Habilita smoke tests reais opt-in |

---

## Integração Multi-Provider de IA

O backend agora possui runtime real e testado para `OpenAI`, `Gemini`, `DeepSeek`, `Anthropic`, `xAI`, `Perplexity`, `Groq`, `OpenRouter`, `Together`, `Fireworks`, `DeepInfra` e `Mistral`, sempre por variáveis de ambiente. Nenhuma chave deve ser hardcoded no código ou salva em arquivos versionados.

### Configuração local

1. Copie `.env.example` para `.env`.
2. Preencha apenas as chaves dos providers que deseja ativar.
3. Suba o backend com `mvn spring-boot:run -Dspring-boot.run.profiles=dev`.
4. Consulte `GET /api/v1/providers`, `GET /api/v1/providers/status` e `GET /api/v1/providers/health` para validar readiness.
5. A nova API unificada por capability fica disponível em `/api/v1/chat`, `/api/v1/responses`, `/api/v1/search`, `/api/v1/web-grounded-chat` e `/api/v1/threat-intel/search`.

### Testes mockados

```bash
cd backend
mvn verify

cd ../frontend
pnpm lint
pnpm test
pnpm build
```

### Testes reais opcionais

```bash
set RUN_REAL_AI_TESTS=true
mvn -Dtest=AiRealSmokeIT test
```

Os testes reais só executam quando `RUN_REAL_AI_TESTS=true` e quando a env var do provider correspondente está presente.

### Exemplo de chamada unificada

```bash
curl -X POST http://localhost:8080/api/v1/inference/execute ^
  -H "Content-Type: application/json" ^
  -d "{\"providerCode\":\"openai\",\"prompt\":\"Responda apenas OK.\",\"fallbackProviderCodes\":[\"anthropic\"]}"
```

### Exemplo da API por capability

```bash
curl -X POST http://localhost:8080/api/v1/chat ^
  -H "Content-Type: application/json" ^
  -d "{\"providerCode\":\"openrouter\",\"modelCode\":\"openrouter:meta-llama/llama-3.3-8b-instruct:free\",\"prompt\":\"Responda apenas OK.\",\"freeTierOnly\":true}"
```

```bash
curl -X POST http://localhost:8080/api/v1/search ^
  -H "Content-Type: application/json" ^
  -d "{\"providerCode\":\"exa\",\"query\":\"latest enterprise ai platform patterns\",\"limit\":5}"
```

```bash
curl -X POST http://localhost:8080/api/v1/threat-intel/search ^
  -H "Content-Type: application/json" ^
  -d "{\"providerCode\":\"darkowl\",\"query\":\"example market\",\"limit\":5,\"justification\":\"Incidente interno sob análise\"}"
```

### Exemplo SSE

```bash
curl -N -X POST http://localhost:8080/api/v1/inference/stream ^
  -H "Content-Type: application/json" ^
  -d "{\"providerCode\":\"google-gemini\",\"prompt\":\"Explique o status atual em poucas linhas.\"}"
```

### Adicionar um novo provider

1. Registrar o provider e os modelos em `ProviderCatalogService`.
2. Criar um novo adapter em `backend/src/main/java/com/lume/workspace/inference/adapter/`.
3. Garantir `healthCheck`, `estimateCost`, `sendPrompt` e `streamPrompt`.
4. Se a integração for capability-specific, expor também o binding em `AiCapabilityService`.
5. Cobrir o adapter com testes mockados e testes de contrato.
6. Atualizar `docs/ai-providers.md`.

### Checklist de segurança

- Não hardcodar API keys.
- Não versionar `.env`.
- Não logar `Authorization`, `x-api-key` ou `x-goog-api-key`.
- Não logar prompts brutos em `INFO/WARN/ERROR`.
- Threat-intel exige `SECURITY_COMPLIANCE_DARK_WEB_ENABLED=true` e justificativa por chamada.
- Validar readiness via startup logs e endpoints `/providers/status` e `/providers/health`.

Consulte também `docs/ai-providers.md` para a matriz detalhada de providers, aliases, modelos padrão e limites desta fase.

---

## Licença

Este projeto é privado e de uso exclusivo.
