# Arquitetura do Sistema

## Visão geral

```
┌─────────────────────────────────────────────────────────────┐
│                         Navegador                            │
│  Equipe:   index.html · dashboard.html · doctor-view.html    │
│  Médico:   doctor-profile-complete.html                      │
│  Paciente: patient-login.html · patient-dashboard.html       │
│            patient-marketplace.html · patient-profile-*.html │
│  Chat:     js/chat-widget.js (embutido nos dashboards)       │
└───────────────┬───────────────────────────┬─────────────────┘
                │  HTTP / JWT                │  WebSocket / STOMP (SockJS)
                ▼                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot :8080                         │
│                                                              │
│  RateLimitFilter (bucket4j)                                  │
│  SecurityFilter (validação de JWT)                           │
│  SecurityConfigurations (Spring Security)                    │
│  WebSocketConfig (handshake STOMP autenticado por JWT)       │
│                                                              │
│  Controllers  →  Services  →  Repositories                  │
│                                                              │
│  TokenService        AuthorizationService (UserDetails)     │
│  IpBlockingService   CaptchaService                         │
│  MarketplaceService  MedicoService / DisponibilidadeService │
│  AgendamentoService  ChatService                            │
└──────────┬─────────────────┬───────────────────────────────┘
           │                 │
           ▼                 ▼
 ┌──────────────────┐ ┌──────────────────┐
 │ PostgreSQL :5432 │ │  MongoDB :27017  │
 │  banco: clinica  │ │ chat (mensagens) │
 └──────────────────┘ └──────────────────┘
```

## Camadas da aplicação

### `config/`
Configurações globais do Spring aplicadas transversalmente a todas as camadas.

- **`SecurityConfigurations`** — define quais rotas são públicas, política stateless (sem
  sessão HTTP), ordem dos filtros, beans de encoder (BCrypt) e authentication manager.
- **`JacksonConfig`** — configura o ObjectMapper para serializar `LocalDateTime` como ISO-8601.
- **`OpenApiConfig`** — metadados do contrato OpenAPI 3 e esquema de segurança Bearer (Swagger UI).
- **`WebSocketConfig`** — habilita o broker STOMP (`/topic`, `/queue`, prefixo `/app`),
  registra o endpoint SockJS `/ws` e autentica o handshake validando o JWT no `CONNECT`.
- **`FlywayConfig`** — habilita `baselineOnMigrate` para que as migrações rodem também em
  bancos antigos já populados.

### `Infra/Security/`
Infraestrutura de segurança que atua antes dos controllers.

- **`RateLimitFilter`** — token bucket por IP. Auth: 40 req/min, API geral: 200 req/min. Responde 429 ao exceder.
- **`SecurityFilter`** — extrai JWT do header `Authorization: Bearer`, valida e popula o `SecurityContextHolder`.
- **`TokenService`** — gera e valida tokens JWT (HMAC-256, expiração 2h). Reutilizado também no handshake WebSocket.
- **`IpBlockingService`** — bloqueia IPs após 5 falhas de login consecutivas por 15 minutos.

### `controller/`
Ponto de entrada HTTP (e WebSocket, no caso do chat). Recebe requisições, delega para
services, define status de retorno.

Não contém regras de negócio — apenas deserialização, validação de formato (`@Valid`),
controle de acesso (`@PreAuthorize`) e montagem do `ResponseEntity`.

Grupos: autenticação (`Login/`), gestão pela equipe (`Paciente/`, `agendamento/`),
portal do paciente (`patient/`), portal do médico (`medico/`), `captcha/`,
`chat/` (REST + STOMP), `user/` e `ImagemController`.

### `service/`
Regras de negócio puras, agnósticas ao protocolo HTTP.

- **`AuthorizationService`** — implementa o `UserDetailsService` do Spring Security: carrega o usuário por `login` durante a autenticação. (Não faz cadastro/MFA/e-mail, apesar do nome.)
- **`PatientService`** — CRUD de pacientes com validação de CPF único e obrigatoriedade de responsável para menores.
- **`PatientAuthService`** — auto-cadastro de paciente pelo portal e vínculo da conta ao registro clínico por CPF (ao completar o perfil).
- **`MarketplaceService`** — busca de médicos por especialidade/cidade e cálculo de horários livres a partir das disponibilidades.
- **`MedicoService`** — perfil do médico, estatísticas mensais e agenda.
- **`DisponibilidadeMedicoService`** — janelas de disponibilidade e geração de slots.
- **`AgendamentoService`** — cria agendamentos com validação de conflito de horário e transições de status.
- **`ChatService`** — conversas (PostgreSQL) e mensagens (MongoDB); marca leitura e atualiza a última mensagem da conversa.
- **`CaptchaService`** — CAPTCHA "Não sou um robô" sem imagem nem quebra-cabeça: na interface é só uma caixinha que o usuário clica. Por baixo é um proof-of-work / hashcash — gera um `challenge` + `difficulty` e o cliente ([pow-worker.js](../src/main/resources/static/js/pow-worker.js)) acha um `nonce` tal que `SHA-256(challenge:nonce)` comece com `difficulty` zeros. Desafios em memória, expiração de 10 min, uso único.

### `repository/`
Interfaces Spring Data. Sem SQL manual — queries geradas por convention (Query Methods).
A maioria estende `JpaRepository` (PostgreSQL); `MensagemRepository` estende
`MongoRepository` (MongoDB).

```java
boolean existsByCpf(String cpf);
List<Mensagem> findByConversaIdOrderByTimestampAsc(String conversaId);
```

### `model/`
Entidades JPA mapeadas para tabelas PostgreSQL e documentos MongoDB para o chat.

- PKs do tipo `UUID` gerado pelo banco (`GenerationType.UUID`) nas entidades JPA
- Relacionamentos via `@ManyToOne`/`@OneToOne` e `@JoinColumn`
- Soft delete via campo `ativo: boolean`
- Auditoria automática via `@CreationTimestamp` e `@UpdateTimestamp`
- `Mensagem` é um `@Document` MongoDB (id `String`, índice em `conversaId`)

### `dto/`
Records Java (imutáveis) usados como contratos de entrada e saída da API.

- Request DTOs: validações com Bean Validation (`@NotBlank`, `@Email`, etc.)
- Response DTOs: projeções das entidades — nunca expõem a entidade diretamente

### `exception/`
- **`BusinessException`** — exceção com `HttpStatus` associado. Lançada pelos services.
- **`GlobalExceptionHandler`** — `@RestControllerAdvice` que intercepta as exceções e retorna `ErrorResponse` padronizado.

## Fluxo de autenticação (estado atual)

O login é direto: CAPTCHA → senha → JWT. **Não há MFA nem envio de e-mail** (ver
"Funcionalidades não implementadas" abaixo).

```
1. GET  /captcha/generate
        ↓ CaptchaService gera um desafio de proof-of-work
        ← { challengeId, challenge, difficulty }
        (o cliente resolve o nonce em js/pow-worker.js antes do login)

2. POST /auth/login  { login, password, captchaId, captchaCode }
        ↓ RateLimitFilter: verifica IP (40 req/min)
        ↓ IpBlockingService.isBlocked(): verifica bloqueio de IP
        ↓ CaptchaService.validate(): confere o nonce (uso único)
        ↓ AuthenticationManager.authenticate(): BCrypt hash check
              (usa AuthorizationService.loadUserByUsername)
        ↓ IpBlockingService.registerSuccess() + TokenService.generateToken(): JWT, 2h
        ← { token: "eyJ...", role: "ADMIN", perfilCompleto: false }
        (em falha de senha: registerFailure → 401 com tentativas restantes)

3. Próximas requisições:
        Authorization: Bearer eyJ...
        ↓ SecurityFilter extrai e valida JWT
        ↓ Spring Security verifica roles em @PreAuthorize
        ↓ Controller → Service → Repository
```

## Funcionalidades não implementadas

Os recursos abaixo **não existem** no backend. O scaffolding morto que existia (serviços
de e-mail/MFA, DTOs, dependência de SMTP e MailHog) foi removido; o que resta é apontado
na coluna "Resíduo".

| Recurso | Estado | Resíduo |
|---|---|---|
| MFA (segundo fator) | não implementado — `/auth/login` devolve o JWT direto | — |
| Envio de e-mail | não implementado — nenhum código envia e-mail | — |
| Verificação de e-mail no cadastro | não implementado — `register` cria o usuário já ativo; `Usuario` não tem campo `email_verified` | — |
| Recuperação de senha | não implementado | — |
| Validação de domínio de e-mail | não implementado | — |

## Chat em tempo real

O chat usa **PostgreSQL** para os metadados da conversa e **MongoDB** para as mensagens.

```
- Conexão:   SockJS em /ws, com Authorization: Bearer <jwt> no frame STOMP CONNECT
             (WebSocketConfig valida o token e injeta o usuário autenticado).
- Envio:     publica em /app/chat/{conversaId}  → ChatService persiste a Mensagem (Mongo)
             e atualiza a Conversa (Postgres).
- Recebimento: assina /topic/conversa/{conversaId}.
- Histórico/anexos: via REST em /chat/** (ver docs/API.md).
```

## Modelo de dados

### PostgreSQL

```
tb_users
  id uuid PK
  login varchar(50) UNIQUE
  nome varchar(100)
  email varchar(150) UNIQUE (nullable — paciente cadastra só com login)
  password varchar
  role varchar (ADMIN|MEDIC|PACIENTE)
  perfil_completo boolean DEFAULT FALSE
  paciente_id uuid FK tb_patients   ← só para role=PACIENTE
  ativo boolean DEFAULT TRUE
  created_at / updated_at timestamp
  -- campos de perfil do médico (role=MEDIC):
  especialidade varchar(50)  crm varchar(20)  cidade varchar(100)
  descricao text  universidade varchar(150)  ano_formacao integer
  foto_url text  valor_consulta numeric(10,2)  duracao_consulta_minutos integer DEFAULT 60

tb_patients
  id uuid PK
  nome varchar  cpf varchar UNIQUE  data_nascimento date
  nome_mae varchar  nome_pai varchar
  ativo boolean DEFAULT TRUE
  created_at / updated_at timestamp

tb_appointments
  id uuid PK
  paciente_id uuid FK tb_patients
  medico_id   uuid FK tb_users
  data_hora timestamp
  status varchar (AGENDADO|CONFIRMADO|REALIZADO|CANCELADO)
  motivo_cancelamento text
  created_at timestamp

tb_disponibilidade_medico
  id uuid PK
  medico_id uuid FK tb_users
  dia_semana varchar(20)  (MONDAY..SUNDAY)
  hora_inicio time  hora_fim time
  ativo boolean DEFAULT TRUE
  created_at timestamp

tb_conversas
  id uuid PK
  paciente_id uuid FK tb_users   ← UNIQUE(paciente_id, medico_id)
  medico_id   uuid FK tb_users
  ultima_mensagem text  ultima_mensagem_em timestamp
  criada_em timestamp

tb_imagens
  id uuid PK
  content_type varchar(50)  nome_arquivo varchar(255)  dados bytea
  created_at timestamp
```

### MongoDB — coleção `mensagens`

```
_id            String
conversaId     String (indexado)
remetenteId    String  remetenteNome String  tipoRemetente String
texto          String
imagemUrl      String  nomeAnexo String   ← anexos opcionais
timestamp      datetime
lida           boolean
```

### Gestão de schema (Flyway)

`spring.jpa.hibernate.ddl-auto=update` cria/atualiza as tabelas, mas nunca remove
colunas/constraints obsoletas. As migrações em
[src/main/resources/db/migration/](../src/main/resources/db/migration/) corrigem esse
*drift*. `FlywayConfig` ativa `baselineOnMigrate`, então rodam tanto em bancos novos
quanto em bancos antigos já populados.

## Frontend

O Spring Boot serve os arquivos estáticos diretamente de `src/main/resources/static/`.

| Arquivo | Responsabilidade |
|---|---|
| `index.html` + `js/auth.js` | Portal da equipe — login e cadastro (com o CAPTCHA proof-of-work) |
| `dashboard.html` + `js/dashboard.js` | Painel interno — pacientes e agendamentos |
| `doctor-view.html` + `js/doctor-view.js` | Visão do médico (agenda/consultas) |
| `doctor-profile-complete.html` + `js/doctor-profile.js` | Completar perfil do médico |
| `patient-login.html` + `js/patient-auth.js` | Portal do paciente — login/cadastro |
| `patient-dashboard.html` + `js/patient-dashboard.js` | Área do paciente — consultas |
| `patient-marketplace.html` + `js/patient-marketplace.js` | Marketplace — buscar médico e agendar |
| `patient-profile-complete.html` + `js/patient-profile.js` | Completar perfil do paciente |
| `js/chat-widget.js` + `css/chat-widget.css` | Widget de chat embutido nos dashboards (STOMP via `js/stomp.umd.min.js` + `js/sockjs.min.js`) |
| `js/pow-worker.js` | Proof-of-work do CAPTCHA em worker |
| `js/tour.js` + `css/tour.css` | Tour guiado da interface |
| `css/*.css` | Estilos (tema da equipe, paciente, médico, chat, marketplace) |

A variável `const API = ""` nos JS define a base URL vazia, fazendo as chamadas para o
mesmo servidor que serve o HTML (same-origin).
