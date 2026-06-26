# Clínica — Sistema de Gestão

[![CI](https://github.com/scheffler5/Spring_Boot_Clinica/actions/workflows/ci.yml/badge.svg)](https://github.com/scheffler5/Spring_Boot_Clinica/actions/workflows/ci.yml)

Sistema web de gestão para clínicas médicas: autenticação segura com MFA, agendamentos,
prontuários, convênios, procedimentos, portal do paciente, portal do médico (marketplace
de consultas) e chat em tempo real entre paciente e médico.

> 📚 **Documentação relacionada**
> - [docs/API.md](docs/API.md) — referência REST completa de todos os endpoints.
> - [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — camadas, fluxos e modelo de dados.
> - [TESTING.md](TESTING.md) — estratégia de testes (unitários + E2E), cobertura e bugs encontrados.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 26 + Spring Boot 4.0 |
| Persistência (relacional) | Spring Data JPA + Hibernate 7 + PostgreSQL 16 |
| Persistência (chat) | Spring Data MongoDB + MongoDB 7 |
| Migrações de schema | Flyway |
| Segurança | Spring Security + JWT (auth0 java-jwt 4.4) |
| Tempo real | WebSocket + STOMP (SockJS) |
| E-mail | Spring Mail (Brevo SMTP / MailHog em dev) |
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

O `docker compose` já sobe com valores padrão para desenvolvimento (PostgreSQL, MongoDB
e MailHog locais), então **não é obrigatório** criar nenhum arquivo para subir o projeto.

Para customizar (ex.: usar SMTP real do Brevo no lugar do MailHog), crie um arquivo `.env`
na raiz — ele é lido automaticamente pelo `docker compose` (`env_file`, opcional):

```env
# SMTP real (opcional — sem isso usa MailHog local)
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
MAIL_USER=seu-login-brevo
MAIL_PASS=sua-senha-smtp-brevo
MAIL_FROM=seu-email@gmail.com
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true

# Validação de domínio de e-mail (false permite e-mails temporários em testes)
EMAIL_DOMAIN_CHECK=true
```

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
| MailHog (e-mails de teste) | http://localhost:8025 |

## Portas

| Porta | Serviço | Interface |
|---|---|---|
| 8080 | Spring Boot | todas (0.0.0.0) |
| 5432 | PostgreSQL | somente localhost |
| 27017 | MongoDB (chat) | somente localhost |
| 1025 | SMTP (MailHog) | somente localhost |
| 8025 | MailHog UI | somente localhost |

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
./run-unit.sh        # testes unitários da lógica de negócio
./run-coverage.sh    # cobertura JaCoCo (compila em Java 21 — ver pom.xml)
./run-e2e.sh         # testes E2E de interface (Playwright)
```

Detalhes em [TESTING.md](TESTING.md).

## Estrutura do projeto

```
src/main/java/com/learn/projeto_learn/
├── config/           Configurações globais (Security, Jackson, OpenAPI, WebSocket, Flyway)
├── controller/       Endpoints HTTP (REST) e WebSocket
│   ├── Login/        Autenticação, MFA, recuperação de senha
│   ├── Paciente/     CRUD de pacientes (equipe)
│   ├── patient/      Portal público do paciente (auto-cadastro, marketplace, agenda)
│   ├── medico/       Portal do médico (perfil, disponibilidade, estatísticas)
│   ├── agendamento/  Agendamentos (equipe)
│   ├── medicalrecorder/ Prontuários
│   ├── convenio/     Convênios
│   ├── procedimento/ Procedimentos
│   ├── captcha/      Geração de CAPTCHA
│   ├── chat/         Chat REST e WebSocket/STOMP
│   ├── user/         Consultas auxiliares de usuários (lista de médicos)
│   └── ImagemController  Servir imagens/anexos
├── dto/              Objetos de transferência (records request/response)
├── exception/        BusinessException + handler global
├── Infra/Security/   JWT, rate limiting, MFA, bloqueio de IP, filtro de segurança
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

- **PostgreSQL** — dados relacionais (usuários, pacientes, agendamentos, prontuários, etc.).
- **MongoDB** — mensagens do chat (coleção `mensagens`).

### Tabelas PostgreSQL

| Tabela | Descrição |
|---|---|
| `tb_users` | Usuários do sistema (equipe e pacientes); inclui os campos de perfil do médico |
| `tb_patients` | Dados clínicos dos pacientes |
| `tb_appointments` | Agendamentos de consultas (com status e médico) |
| `tb_disponibilidade_medico` | Janelas de disponibilidade do médico por dia da semana |
| `tb_prontuarios` | Prontuários de atendimento |
| `tb_convenios` | Planos de saúde e descontos |
| `tb_procedimentos` | Procedimentos médicos e custos |
| `tb_locais` | Locais de atendimento |
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
| `ADMIN` | Total — herda as permissões de `MEDIC` e `RECEPCIONIST` |
| `MEDIC` | Prontuários, agenda, perfil e disponibilidade do médico |
| `RECEPCIONIST` | Pacientes e agendamentos |
| `PACIENTE` | Apenas o próprio portal (perfil, marketplace, consultas, prontuários, chat) |

Todos os papéis fazem login com **login + senha**. O paciente vincula seu CPF ao registro
clínico ao completar o perfil (não faz login com CPF).

## Segurança

- Autenticação em dois fatores (MFA) via e-mail em todos os logins
- CAPTCHA "Não sou um robô": o usuário só clica numa caixinha — não há imagem nem
  quebra-cabeça. Por baixo é um proof-of-work: o clique dispara um worker que resolve o
  desafio do servidor (acha um nonce cujo SHA-256 começa com N zeros). Desafio de uso
  único, expira em 10 min
- Rate limiting por IP: 40 req/min em rotas de auth, 200 req/min nas demais (HTTP 429)
- Bloqueio de IP após 5 falhas consecutivas de login (15 min)
- Máximo de 3 tentativas de código MFA por sessão
- Máximo de 3 reenvios de código MFA com cooldown de 60s
- Verificação de e-mail obrigatória no cadastro
- Bloqueio de domínios de e-mail descartáveis (~70 domínios)
- Tokens JWT com expiração de 2 horas (HMAC-256); conexões WebSocket também autenticadas por JWT
- Senhas armazenadas com BCrypt

## Documentação da API

- Interativa: **Swagger UI** em `/swagger-ui.html` (contrato OpenAPI bruto em `/v3/api-docs`).
- Referência em Markdown: [docs/API.md](docs/API.md).

## Licença

Projeto acadêmico — Faculdade.
