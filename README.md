# Clínica — Sistema de Gestão

[![CI](https://github.com/scheffler5/Spring_Boot_Clinica/actions/workflows/ci.yml/badge.svg)](https://github.com/scheffler5/Spring_Boot_Clinica/actions/workflows/ci.yml)

Sistema web de gestão para clínicas médicas: autenticação com CAPTCHA e JWT, agendamentos,
portal do paciente, portal do médico (marketplace de consultas) e chat em tempo real entre
paciente e médico.

> 📚 **Documentação relacionada**
> - [docs/API.md](docs/API.md) — referência REST completa de todos os endpoints.
> - [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — camadas, fluxos e modelo de dados.
> - [TESTING.md](docs/TESTING.md) — estratégia de testes (unitários + E2E), cobertura e bugs encontrados.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 26 + Spring Boot 4.0 |
| Persistência (relacional) | Spring Data JPA + Hibernate 7 + PostgreSQL 16 |
| Persistência (chat) | Spring Data MongoDB + MongoDB 7 |
| Migrações de schema | Flyway |
| Segurança | Spring Security + JWT (auth0 java-jwt 4.4) |
| Tempo real | WebSocket + STOMP (SockJS) |
| Rate limiting | Bucket4j 8.10 |
| Documentação da API | springdoc OpenAPI 3 + Swagger UI |
| Frontend | HTML5 + CSS3 + JavaScript vanilla |
| Testes | JUnit + Spring Test (unitários) + Playwright (E2E) + JaCoCo (cobertura) |
| Containerização | Docker + Docker Compose |
| Build | Maven 3.9 + Maven Wrapper |

## Pré-requisitos

- Docker e Docker Compose instalados
- Java 26 (apenas para build fora do Docker)

## Configuração rápida

### 1. Clonar o repositório

```bash
git clone <url-do-repo>
cd Spring_Boot_Clinica
```

### 2. Configurar variáveis de ambiente (opcional)

O `docker compose` já sobe com valores padrão para desenvolvimento (PostgreSQL e MongoDB
locais), então **não é obrigatório** criar nenhum arquivo para subir o projeto.

Para customizar (ex.: trocar `JWT_SECRET` ou as credenciais do banco), crie um arquivo
`.env` na raiz — ele é lido automaticamente pelo `docker compose` (`env_file`, opcional).
As variáveis disponíveis estão no bloco `environment` do
[docker-compose.yml](docker-compose.yml).

> Para rodar a aplicação **fora do Docker** (Maven direto), use
> [src/main/resources/application.properties.example](src/main/resources/application.properties.example)
> como base.

### 3. Subir a aplicação

```bash
docker compose up -d
```

Na primeira execução o Docker fará o build da imagem (~3–5 minutos).

### 4. Acessar

| Serviço | URL |
|---|---|
| Portal da equipe (login interno) | http://localhost:8080 |
| Portal do paciente | http://localhost:8080/patient-login.html |
| Documentação interativa da API (Swagger UI) | http://localhost:8080/swagger-ui.html |

### 5. (Opcional) Popular o banco com dados de demonstração

Com a aplicação **no ar**, o script [seed_medicos.py](seed_medicos.py) cria 4 médicos de
exemplo — cada um com perfil completo, foto e janelas de disponibilidade — prontos para
aparecer no marketplace do paciente.

```bash
pip install requests        # única dependência do script
python3 seed_medicos.py     # usa http://localhost:8080 por padrão
```

Para apontar para outra URL: `python3 seed_medicos.py --base-url http://localhost:8080`.

Os médicos criados (senha padrão **`Demo@1234`**):

| Login | Nome | Especialidade | Cidade |
|---|---|---|---|
| `dr.rafael.costa` | Dr. Rafael Costa | Cardiologia | São Paulo |
| `dr.bruno.almeida` | Dr. Bruno Almeida | Ortopedia | Rio de Janeiro |
| `dra.ana.lima` | Dra. Ana Lima | Dermatologia | Belo Horizonte |
| `dra.fernanda.santos` | Dra. Fernanda Santos | Psiquiatria | Porto Alegre |

> O script é **idempotente**: se um médico já existe, ele segue em frente sem duplicar.
> As fotos vêm da pasta [Imagens/](Imagens/). Para começar do zero antes de popular,
> rode `docker compose down -v && docker compose up -d` (limpa Postgres + Mongo).

## Portas

| Porta | Serviço | Interface |
|---|---|---|
| 8080 | Spring Boot | todas (0.0.0.0) |
| 5432 | PostgreSQL | somente localhost |
| 27017 | MongoDB (chat) | somente localhost |

## Comandos úteis

```bash
# Subir tudo
docker compose up -d

# Ver logs em tempo real
docker compose logs -f app

# Reiniciar só a aplicação (bancos mantidos)
docker compose restart app

# Parar tudo e limpar os bancos (Postgres + Mongo)
docker compose down -v

# Rebuild após mudança no código
docker compose build app && docker compose up -d app
```

### Testes

```bash
./scripts/run-unit.sh        # testes unitários da lógica de negócio
./scripts/run-coverage.sh    # cobertura JaCoCo (compila em Java 21 — ver pom.xml)
./scripts/run-e2e.sh         # testes E2E de interface (Playwright)
```

Detalhes em [TESTING.md](docs/TESTING.md).

## Estrutura do projeto

```
src/main/java/com/learn/projeto_learn/
├── config/           Configurações globais (Security, Jackson, OpenAPI, WebSocket, Flyway)
├── controller/       Endpoints HTTP (REST) e WebSocket
│   ├── login/        Autenticação (login, registro de médico)
│   ├── patient/      Portal público do paciente (auto-cadastro, marketplace, agenda)
│   ├── medico/       Portal do médico (perfil, disponibilidade, estatísticas)
│   ├── agendamento/  Cancelamento de agendamentos
│   ├── captcha/      Geração de CAPTCHA
│   ├── chat/         Chat REST e WebSocket/STOMP
│   └── ImagemController  Servir imagens/anexos
├── dto/              Objetos de transferência (records request/response)
├── exception/        BusinessException + handler global
├── infra/security/   JWT, rate limiting, bloqueio de IP, filtro de segurança
├── model/            Entidades JPA (PostgreSQL) e documentos MongoDB (chat)
├── repository/       Interfaces Spring Data (JPA + MongoDB)
└── service/          Regras de negócio

src/main/resources/
├── application.properties      Configuração da aplicação
├── db/migration/               Migrações Flyway (versionamento do schema)
└── static/                     Frontend (HTML, CSS, JS)
```

## Bancos de dados

A aplicação usa **dois** bancos:

- **PostgreSQL** — dados relacionais (usuários, pacientes, agendamentos, etc.).
- **MongoDB** — mensagens do chat (coleção `mensagens`).

### Tabelas PostgreSQL

| Tabela | Descrição |
|---|---|
| `tb_users` | Usuários do sistema (equipe e pacientes); inclui os campos de perfil do médico |
| `tb_patients` | Dados clínicos dos pacientes |
| `tb_appointments` | Agendamentos de consultas (com status e médico) |
| `tb_disponibilidade_medico` | Janelas de disponibilidade do médico por dia da semana |
| `tb_conversas` | Conversas paciente↔médico (metadados; as mensagens ficam no MongoDB) |
| `tb_imagens` | Imagens e anexos (fotos de perfil, anexos do chat) |

### Gestão de schema

O schema é gerenciado por `spring.jpa.hibernate.ddl-auto=update` (Hibernate cria/atualiza
tabelas) **em conjunto com migrações Flyway** em [src/main/resources/db/migration/](src/main/resources/db/migration/).
As migrações corrigem o *drift* que o `ddl-auto=update` acumula (ele cria colunas/constraints
mas nunca as remove). O baseline é configurado em código
([config/FlywayConfig](src/main/java/com/learn/projeto_learn/config/FlywayConfig.java)),
então as migrações rodam com segurança tanto em bancos novos quanto antigos.

## Roles de usuário

| Role | Acesso |
|---|---|
| `MEDIC` | Agenda, perfil e disponibilidade do médico |
| `PACIENTE` | Apenas o próprio portal (perfil, marketplace, consultas, chat) |

Ambos os papéis fazem login com **login + senha**. O médico se auto-cadastra em
`/auth/register`; o paciente em `/patient/register`. O paciente vincula seu CPF ao registro
clínico ao completar o perfil (não faz login com CPF).

## Segurança

**Ativo no código** (fluxo de login atual: CAPTCHA → senha → JWT):

- CAPTCHA "Não sou um robô": o usuário só clica numa caixinha — não há imagem nem
  quebra-cabeça. Por baixo é um proof-of-work: o clique dispara um worker que resolve o
  desafio do servidor (acha um nonce cujo SHA-256 começa com N zeros). Desafio de uso
  único, expira em 10 min
- Rate limiting por IP: 40 req/min em rotas de auth, 200 req/min nas demais (HTTP 429)
- Bloqueio de IP após 5 falhas consecutivas de login (15 min)
- Tokens JWT com expiração de 2 horas (HMAC-256); conexões WebSocket também autenticadas por JWT
- Senhas armazenadas com BCrypt

> **Não implementado:** o login **não** tem segundo fator (MFA) e o sistema **não envia
> e-mail**. Recursos como MFA por e-mail, verificação de e-mail no cadastro, recuperação de
> senha e bloqueio de domínios descartáveis não existem no backend — o scaffolding morto
> que existia (serviços de e-mail/MFA, DTOs, dependência de SMTP e MailHog) foi removido.

## Documentação da API

- Interativa: **Swagger UI** em `/swagger-ui.html` (contrato OpenAPI bruto em `/v3/api-docs`).
- Referência em Markdown: [docs/API.md](docs/API.md).

## Licença

Projeto acadêmico — Faculdade.
