# Arquitetura do Sistema

## Visão geral

```
┌─────────────────────────────────────────────────────┐
│                    Navegador                         │
│  index.html        dashboard.html                    │
│  patient-login.html patient-dashboard.html           │
│  auth.js  patient-auth.js  mfa-block.js              │
│  dashboard.js  patient-dashboard.js                  │
└──────────────────────┬──────────────────────────────┘
                       │  HTTP / JWT
                       ▼
┌─────────────────────────────────────────────────────┐
│              Spring Boot :8080                       │
│                                                      │
│  RateLimitFilter (bucket4j)                          │
│  SecurityFilter (JWT validation)                     │
│  SecurityConfigurations (Spring Security)            │
│                                                      │
│  Controllers  →  Services  →  Repositories           │
│                                                      │
│  IpBlockingService   MfaAttemptService               │
│  CaptchaService      EmailValidationService          │
│  TokenService        AuthorizationService            │
└──────────────┬──────────────────┬───────────────────┘
               │                  │
               ▼                  ▼
    ┌──────────────────┐  ┌──────────────────┐
    │  PostgreSQL :5432 │  │  SMTP (Brevo /   │
    │  banco: clinica   │  │  MailHog) :1025  │
    └──────────────────┘  └──────────────────┘
```

## Camadas da aplicação

### `config/`
Configurações globais do Spring aplicadas transversalmente a todas as camadas.

- **`SecurityConfigurations`** — define quais rotas são públicas, política stateless (sem sessão HTTP), ordem dos filtros, beans de encoder e authentication manager.
- **`JacksonConfig`** — configura o ObjectMapper para serializar `LocalDateTime` como ISO-8601 string.

### `Infra/Security/`
Infraestrutura de segurança que atua antes dos controllers.

- **`RateLimitFilter`** — token bucket por IP. Auth: 40 req/min, API geral: 200 req/min. Responde 429 ao exceder.
- **`SecurityFilter`** — extrai JWT do header `Authorization: Bearer`, valida e popula o `SecurityContextHolder`.
- **`TokenService`** — gera e valida tokens JWT (HMAC-256, expiração 2h).
- **`IpBlockingService`** — bloqueia IPs após 5 falhas de login consecutivas por 15 minutos.
- **`MfaAttemptService`** — rastreia tentativas de código MFA por e-mail. Máx 3 tentativas e 3 reenvios.

### `controller/`
Ponto de entrada HTTP. Recebe requisições, delega para services, define status de retorno.

Não contém regras de negócio — apenas deserialização, validação de formato (`@Valid`) e montagem do `ResponseEntity`.

### `service/`
Regras de negócio puras, agnósticas ao protocolo HTTP.

- **`AuthorizationService`** — cadastro de usuário, verificação de e-mail, MFA, recuperação de senha.
- **`PatientService`** — CRUD de pacientes com validação de CPF único e obrigatoriedade de responsável para menores.
- **`PatientAuthService`** — cadastro de paciente via portal público (vincula conta ao registro clínico por CPF).
- **`ProntuarioService`** — registra atendimentos e calcula `valorCalculado = custo × (1 - desconto_convenio)`.
- **`AgendamentoService`** — cria agendamentos com validação de conflito de horário.
- **`CaptchaService`** — gera imagem PNG com distorção no servidor, armazena tokens em memória com expiração de 5 minutos. Token de uso único.
- **`EmailValidationService`** — valida formato de e-mail e rejeita ~70 domínios descartáveis conhecidos.
- **`EmailService`** — envia e-mails via JavaMailSender (configurável para MailHog ou Brevo).

### `repository/`
Interfaces Spring Data JPA. Sem SQL manual — queries geradas por convention (Query Methods).

```java
boolean existsByCpf(String cpf);
List<Paciente> findByNomeContainingIgnoreCaseOrCpfAndAtivo(String nome, String cpf, Boolean ativo);
```

### `model/`
Entidades JPA mapeadas para tabelas PostgreSQL.

- PKs do tipo `UUID` gerado pelo banco (`GenerationType.UUID`)
- Relacionamentos via `@ManyToOne` e `@JoinColumn`
- Soft delete via campo `ativo: boolean`
- Auditoria automática via `@CreationTimestamp` e `@UpdateTimestamp`

### `dto/`
Records Java (imutáveis) usados como contratos de entrada e saída da API.

- Request DTOs: validações com Bean Validation (`@NotBlank`, `@Email`, `@CPF`, etc.)
- Response DTOs: projeções das entidades — nunca expõem a entidade diretamente

### `exception/`
- **`BusinessException`** — exceção com `HttpStatus` associado. Lançada pelos services.
- **`GlobalExceptionHandler`** — `@RestControllerAdvice` que intercepta todas as exceções e retorna `ErrorResponse` padronizado.

## Fluxo de autenticação completo

```
1. GET  /captcha/generate
        ↓ CaptchaService gera código 6 chars + imagem PNG
        ← { captchaId, image }

2. POST /auth/login  { login, password, captchaId, captchaCode }
        ↓ RateLimitFilter: verifica IP (40 req/min)
        ↓ IpBlockingService: verifica bloqueio de IP
        ↓ CaptchaService.validate(): token uso único
        ↓ AuthenticationManager.authenticate(): BCrypt hash check
        ↓ AuthorizationService.sendMfaCode(): gera código, salva em mfa_token
        ↓ EmailService.sendEmailText(): envia via SMTP
        ← { mfaRequired: true, emailHint: "ga***", email: "real@email.com" }

3. POST /auth/verify-mfa  { email, mfaCode }
        ↓ MfaAttemptService: verifica tentativas restantes
        ↓ AuthorizationService.verifyMfaAndGetLogin(): compara com mfa_token
        ↓ TokenService.generateToken(): JWT HMAC-256, 2h
        ← { token: "eyJ...", role: "ADMIN" }

4. Próximas requisições:
        Authorization: Bearer eyJ...
        ↓ SecurityFilter extrai e valida JWT
        ↓ Spring Security verifica roles em @PreAuthorize
        ↓ Controller → Service → Repository
```

## Modelo de dados

```
tb_users
  id uuid PK
  login varchar(50) UNIQUE
  email varchar(150) UNIQUE
  password varchar
  role varchar (ADMIN|MEDIC|RECEPCIONIST|PACIENTE)
  ativo boolean DEFAULT FALSE
  email_verified boolean DEFAULT FALSE
  email_verification_token varchar(6)
  email_verification_expiration timestamp
  mfa_token varchar(6)
  mfa_token_expiration timestamp
  two_factor_token varchar(6)        ← recuperação de senha
  recovery_token_expiration timestamp
  paciente_id uuid FK tb_patients    ← só para role=PACIENTE
  created_at timestamp
  updated_at timestamp

tb_patients
  id uuid PK
  nome varchar
  cpf varchar UNIQUE
  data_nascimento date
  nome_mae varchar
  nome_pai varchar
  ativo boolean DEFAULT TRUE
  created_at / updated_at timestamp

tb_appointments
  id uuid PK
  paciente_id uuid FK tb_patients
  data_hora timestamp
  created_at timestamp

tb_prontuarios
  id uuid PK
  paciente_id uuid FK tb_patients
  usuario_id  uuid FK tb_users
  convenio_id uuid FK tb_convenios
  procedimento_id uuid FK tb_procedimentos
  valor_calculado numeric
  observacoes text
  data_atendimento timestamp

tb_convenios
  id uuid PK
  nome varchar UNIQUE
  desconto numeric      ← 0.0 a 1.0
  ativo boolean
  created_at / updated_at timestamp

tb_procedimentos
  id uuid PK
  descricao varchar
  custo numeric
  ativo boolean
  created_at / updated_at timestamp

tb_locais
  id uuid PK
  descricao varchar
  ativo boolean
  created_at / updated_at timestamp
```

## Frontend

O Spring Boot serve os arquivos estáticos diretamente da pasta `src/main/resources/static/`.

| Arquivo | Responsabilidade |
|---|---|
| `index.html` + `auth.js` | Portal da equipe — login, cadastro, recuperação |
| `dashboard.html` + `dashboard.js` | Painel interno — pacientes e agendamentos |
| `patient-login.html` + `patient-auth.js` | Portal do paciente — login com CPF |
| `patient-dashboard.html` + `patient-dashboard.js` | Área do paciente — consultas e prontuários |
| `mfa-block.js` | Componente compartilhado — MFA, reenvio, countdown |
| `css/style.css` | Estilos globais com tema azul (equipe) e teal (paciente) |

A variável `const API = ""` em todos os JS define a base URL vazia, fazendo chamadas para o mesmo servidor que serve o HTML (same-origin).
