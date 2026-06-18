# Clínica — Sistema de Gestão

Sistema web de gestão para clínicas médicas, com autenticação segura, agendamentos, prontuários e portal do paciente.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 26 + Spring Boot 4.0 |
| Persistência | Spring Data JPA + Hibernate 7 + PostgreSQL 16 |
| Segurança | Spring Security + JWT (auth0 java-jwt 4.4) |
| E-mail | Spring Mail (Brevo SMTP / MailHog dev) |
| Rate Limiting | Bucket4j 8.10 |
| Frontend | HTML5 + CSS3 + JavaScript vanilla |
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

### 2. Configurar variáveis de ambiente

Copie o arquivo de exemplo e edite com suas credenciais:

```bash
cp .env.example .env
```

Variáveis disponíveis:

```env
# SMTP real (opcional — sem isso usa MailHog local)
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
MAIL_USER=seu-login-brevo
MAIL_PASS=sua-senha-smtp-brevo
MAIL_FROM=seu-email@gmail.com
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true

# Validação de domínio de e-mail
EMAIL_DOMAIN_CHECK=true   # false para permitir e-mails temporários em testes
```

### 3. Subir a aplicação

```bash
docker compose up -d
```

Na primeira execução o Docker fará o build da imagem (~3–5 minutos).

### 4. Acessar

| Serviço | URL |
|---|---|
| Aplicação | http://localhost:8080 |
| Portal do paciente | http://localhost:8080/patient-login.html |
| MailHog (e-mails de teste) | http://localhost:8025 |

## Portas

| Porta | Serviço | Interface |
|---|---|---|
| 8080 | Spring Boot | todas (0.0.0.0) |
| 5432 | PostgreSQL | somente localhost |
| 1025 | SMTP (MailHog) | somente localhost |
| 8025 | MailHog UI | somente localhost |

## Comandos úteis

```bash
# Subir tudo
docker compose up -d

# Ver logs em tempo real
docker compose logs -f app

# Reiniciar só a aplicação (banco mantido)
docker compose restart app

# Parar tudo e limpar banco
docker compose down -v

# Rebuild após mudança no código
docker compose build app && docker compose up -d app
```

## Estrutura do projeto

```
src/main/java/com/learn/projeto_learn/
├── config/           Configurações globais (Security, Jackson)
├── controller/       Endpoints HTTP (REST)
│   ├── Login/        Autenticação, MFA, recuperação de senha
│   ├── Paciente/     CRUD de pacientes
│   ├── agendamento/  Agendamentos
│   ├── medicalrecorder/ Prontuários
│   ├── convenio/     Convênios
│   ├── procedimento/ Procedimentos
│   ├── captcha/      Geração de CAPTCHA
│   └── patient/      Portal do paciente
├── dto/              Objetos de transferência (request/response)
├── exception/        Exceções e handler global
├── Infra/Security/   JWT, rate limiting, MFA, IP blocking
├── model/            Entidades JPA (tabelas do banco)
├── repository/       Interfaces Spring Data JPA
└── service/          Regras de negócio

src/main/resources/
├── application.properties   Configuração da aplicação
└── static/                  Frontend (HTML, CSS, JS)
```

## Banco de dados

| Tabela | Descrição |
|---|---|
| `tb_users` | Usuários do sistema (funcionários e pacientes) |
| `tb_patients` | Dados clínicos dos pacientes |
| `tb_appointments` | Agendamentos de consultas |
| `tb_prontuarios` | Prontuários de atendimento |
| `tb_convenios` | Planos de saúde e descontos |
| `tb_procedimentos` | Procedimentos médicos e custos |
| `tb_locais` | Locais de atendimento |

O Hibernate gerencia o schema automaticamente (`ddl-auto=create-drop` em dev).

## Roles de usuário

| Role | Acesso | Login com |
|---|---|---|
| `ADMIN` | Total — herda MEDIC e RECEPCIONIST | username |
| `MEDIC` | Prontuários e consultas | username |
| `RECEPCIONIST` | Pacientes e agendamentos | username |
| `PACIENTE` | Apenas próprios dados | CPF |

## Segurança

- Autenticação em dois fatores (MFA) via e-mail em todos os logins
- CAPTCHA gerado no servidor (imagem PNG com distorção)
- Rate limiting por IP: 40 req/min em rotas de auth, 200 req/min nas demais
- Bloqueio de IP após 5 falhas consecutivas de login (15 min)
- Máximo de 3 tentativas de código MFA por sessão
- Máximo de 3 reenvios de código MFA com cooldown de 60s
- Verificação de e-mail obrigatória no cadastro
- Bloqueio de domínios de e-mail descartáveis (~70 domínios)
- Tokens JWT com expiração de 2 horas
- Senhas armazenadas com BCrypt

## Documentação da API

Ver [docs/API.md](docs/API.md) para referência completa de todos os endpoints.

## Licença

Projeto acadêmico — Faculdade.
