# API da Clínica — Referência REST

API REST do sistema de gestão de clínica médica (Spring Boot 4). Cobre autenticação,
portal do paciente, portal do médico, agendamentos e chat.

> **Documentação interativa (Swagger UI):** com a aplicação no ar, acesse
> [`/swagger-ui.html`](http://localhost:8080/swagger-ui.html). O contrato OpenAPI bruto
> fica em [`/v3/api-docs`](http://localhost:8080/v3/api-docs).

---

## Sumário

- [Autenticação](#autenticação)
- [Papéis (roles)](#papéis-roles)
- [Convenções](#convenções)
- [Endpoints](#endpoints)
  - [Autenticação (`/auth`)](#autenticação-auth)
  - [CAPTCHA (`/captcha`)](#captcha-captcha)
  - [Portal do Paciente (`/patient`)](#portal-do-paciente-patient)
  - [Portal do Médico (`/medico`)](#portal-do-médico-medico)
  - [Agendamentos (`/appointments`)](#agendamentos-appointments)
  - [Imagens (`/imagens`)](#imagens-imagens)
  - [Chat REST (`/chat`)](#chat-rest-chat)
  - [Chat em tempo real (WebSocket/STOMP)](#chat-em-tempo-real-websocketstomp)
- [Modelos de dados](#modelos-de-dados)
- [Tratamento de erros](#tratamento-de-erros)

---

## Autenticação

A API usa **JWT (Bearer token)**, sem sessão (stateless). O fluxo típico:

1. `GET /captcha/generate` → devolve um desafio de proof-of-work (`challengeId`, `challenge`, `difficulty`).
2. `POST /auth/login` com login, senha e o CAPTCHA resolvido (o `nonce` encontrado) → devolve o `token`.
3. Nas demais requisições, envie o cabeçalho:

```
Authorization: Bearer <token>
```

O token expira de acordo com `api.security.token.expiration-hours` (padrão: 2 horas).
No Swagger UI, clique em **Authorize** e informe apenas o token (sem o prefixo `Bearer`).

## Papéis (roles)

| Role       | Descrição                                       |
|------------|-------------------------------------------------|
| `MEDIC`    | Médico                                          |
| `PACIENTE` | Paciente (acesso apenas ao próprio portal)      |

## Convenções

- **Base URL** (desenvolvimento): `http://localhost:8080`
- **Formato:** JSON em requisições e respostas, exceto uploads (`multipart/form-data`)
  e o endpoint de imagens (bytes binários).
- **Datas/horas:** ISO-8601 (`2026-06-26`, `2026-06-26T14:30:00`).
- **IDs:** UUID, salvo onde indicado.
- A coluna **Auth** indica os papéis exigidos. "Público" = não exige token.

---

## Endpoints

### Autenticação (`/auth`)

| Método | Caminho          | Auth     | Descrição                                  |
|--------|------------------|----------|--------------------------------------------|
| POST   | `/auth/login`    | Público  | Autentica e retorna um token JWT           |
| POST   | `/auth/register` | Público  | Registra um novo usuário médico (`MEDIC`)  |

**`POST /auth/login`** — corpo:

```json
{
  "login": "medico@clinica.com",
  "password": "senha-secreta",
  "captchaId": "uuid-do-desafio",
  "captchaCode": "284731"
}
```

Resposta `200 OK`:

```json
{ "token": "<jwt>", "role": "MEDIC", "perfilCompleto": false }
```

Erros: `401` credenciais inválidas, `403` conta desativada,
`429` IP bloqueado por excesso de tentativas, `400` CAPTCHA inválido.

**`POST /auth/register`** — corpo: `{ "login", "password", "captchaId", "captchaCode" }`.
Resposta `201 Created` (sem corpo).

---

### CAPTCHA (`/captcha`)

| Método | Caminho             | Auth    | Descrição                       |
|--------|---------------------|---------|---------------------------------|
| GET    | `/captcha/generate` | Público | Gera um novo desafio CAPTCHA    |

O CAPTCHA é de **prova de trabalho (proof-of-work)** — na interface aparece apenas como
uma caixinha "Não sou um robô", sem imagem nem quebra-cabeça. O cliente deve encontrar um
`nonce` tal que `SHA-256("<challenge>:<nonce>")` comece com `difficulty` zeros e enviá-lo
no login (campo `captchaCode`). O desafio é de uso único e expira em 10 minutos.

Resposta `200 OK`:

```json
{ "challengeId": "uuid-do-desafio", "challenge": "a1b2c3...", "difficulty": 4 }
```

---

### Portal do Paciente (`/patient`)

| Método | Caminho                                  | Auth       | Descrição                                |
|--------|------------------------------------------|------------|------------------------------------------|
| POST   | `/patient/register`                      | Público    | Auto-cadastro de paciente                |
| GET    | `/patient/me`                            | `PACIENTE` | Perfil do paciente autenticado           |
| POST   | `/patient/foto`                          | `PACIENTE` | Envia foto de perfil (multipart)         |
| PUT    | `/patient/complete-profile`              | `PACIENTE` | Completa o cadastro                      |
| GET    | `/patient/especialidades`                | `PACIENTE` | Lista especialidades do marketplace      |
| GET    | `/patient/medicos`                       | `PACIENTE` | Busca médicos (filtros: especialidade, cidade) |
| GET    | `/patient/medicos/{medicoId}/horarios`   | `PACIENTE` | Horários livres de um médico numa data (`?data=yyyy-MM-dd`) |
| GET    | `/patient/medicos/{id}/detalhes`         | `PACIENTE` | Detalhes do médico e disponibilidades    |
| POST   | `/patient/agendamentos`                  | `PACIENTE` | Agenda uma consulta                      |
| GET    | `/patient/appointments`                  | `PACIENTE` | Agendamentos do paciente                 |

**`POST /patient/register`** — corpo: `{ "login", "password", "captchaId", "captchaCode" }` → `201`.

**`PUT /patient/complete-profile`** — corpo:

```json
{ "nome": "Maria Silva", "cpf": "12345678900", "dataNascimento": "1990-05-10", "nomeMae": "Joana Silva" }
```

**`POST /patient/agendamentos`** — corpo:

```json
{ "medicoId": "uuid-do-medico", "dataHora": "2026-06-30T14:00:00" }
```

**`POST /patient/foto`** — `multipart/form-data`, campo `arquivo`. Aceita JPEG/PNG/WebP até 10 MB.

---

### Portal do Médico (`/medico`)

| Método | Caminho                          | Auth                  | Descrição                                |
|--------|----------------------------------|-----------------------|------------------------------------------|
| GET    | `/medico/especialidades`         | Público               | Lista especialidades (valor/rótulo)      |
| GET    | `/medico/perfil`                 | `MEDIC`               | Perfil do médico autenticado             |
| PUT    | `/medico/perfil`                 | `MEDIC`               | Atualiza o perfil (completo)             |
| PATCH  | `/medico/perfil`                 | `MEDIC`               | Completa/atualiza o perfil (parcial)     |
| GET    | `/medico/estatisticas`           | `MEDIC`               | Estatísticas do mês (`?mes=yyyy-MM`)     |
| POST   | `/medico/foto`                   | `MEDIC`               | Envia foto de perfil (multipart)         |
| GET    | `/medico/agenda`                 | `MEDIC`               | Agenda de consultas                      |
| POST   | `/medico/disponibilidade`        | `MEDIC`               | Adiciona janela de disponibilidade       |
| GET    | `/medico/disponibilidade`        | `MEDIC`               | Lista disponibilidades                   |
| DELETE | `/medico/disponibilidade/{id}`   | `MEDIC`               | Remove uma disponibilidade               |

**`PUT`/`PATCH /medico/perfil`** — corpo:

```json
{
  "nome": "Dr. João",
  "crm": "CRM/SP 123456",
  "especialidade": "CARDIOLOGIA",
  "cidade": "São Paulo",
  "valorConsulta": 250.00,
  "duracaoConsultaMinutos": 30,
  "descricao": "...",
  "universidade": "USP",
  "anoFormacao": 2015
}
```

**`POST /medico/disponibilidade`** — corpo:

```json
{ "diaSemana": "MONDAY", "horaInicio": "08:00", "horaFim": "12:00" }
```

---

### Agendamentos (`/appointments`)

| Método | Caminho               | Auth                       | Descrição                          |
|--------|-----------------------|----------------------------|------------------------------------|
| DELETE | `/appointments/{id}`  | `MEDIC`, `PACIENTE` | Cancela um agendamento                    |

O cancelamento valida posse e antecedência no serviço: o paciente cancela a própria
consulta (mín. 24h de antecedência); o médico/admin cancelam as suas (mín. 10h).
O agendamento em si é criado pelo paciente em `POST /patient/agendamentos`.

---

### Imagens (`/imagens`)

| Método | Caminho          | Auth    | Descrição                                  |
|--------|------------------|---------|--------------------------------------------|
| GET    | `/imagens/{id}`  | Público | Serve uma imagem/anexo (bytes binários)    |

Retorna o conteúdo com o `Content-Type` original e `Cache-Control: public, max-age=86400`.
Anexos não-imagem retornam `Content-Disposition: attachment`.

---

### Chat REST (`/chat`)

Todos exigem usuário autenticado (qualquer papel).

| Método | Caminho                            | Descrição                                  |
|--------|------------------------------------|--------------------------------------------|
| GET    | `/chat/contatos`                   | Lista contatos disponíveis                 |
| GET    | `/chat/conversas`                  | Lista as conversas do usuário              |
| POST   | `/chat/conversas/com/{contatoId}`  | Cria ou recupera conversa com um contato   |
| GET    | `/chat/conversas/{id}/mensagens`   | Lista mensagens de uma conversa            |
| PATCH  | `/chat/conversas/{id}/lidas`       | Marca mensagens como lidas                 |
| POST   | `/chat/upload`                     | Upload de anexo (multipart, até 10 MB)     |

**`POST /chat/upload`** — `multipart/form-data`, campo `arquivo`. Aceita imagens
(JPEG, PNG, WebP, GIF) e documentos (PDF, DOC, DOCX). Resposta:

```json
{ "url": "/imagens/<id>", "nome": "documento.pdf", "tipo": "arquivo" }
```

---

### Chat em tempo real (WebSocket/STOMP)

O envio de mensagens em tempo real usa **WebSocket + STOMP** (não REST), por isso não
aparece no Swagger.

- **Endpoint de conexão (SockJS):** `/ws`
- **Autenticação:** cabeçalho STOMP `Authorization: Bearer <token>` no `CONNECT`.
- **Envio:** publique em `/app/chat/{conversaId}` com o corpo:

  ```json
  { "texto": "Olá!", "imagemUrl": "/imagens/<id>", "nomeAnexo": "foto.png" }
  ```

- **Recebimento:** assine `/topic/conversa/{conversaId}`.

---

## Modelos de dados

### Especialidades

`CLINICA_GERAL`, `CARDIOLOGIA`, `DERMATOLOGIA`, `ENDOCRINOLOGIA`, `GASTROENTEROLOGIA`,
`GERIATRIA`, `GINECOLOGIA_OBSTETRICIA`, `HEMATOLOGIA`, `INFECTOLOGIA`, `NEFROLOGIA`,
`NEUROLOGIA`, `NUTROLOGIA`, `OFTALMOLOGIA`, `ONCOLOGIA`, `ORTOPEDIA`,
`OTORRINOLARINGOLOGIA`, `PEDIATRIA`, `PNEUMOLOGIA`, `PSIQUIATRIA`, `PSICOLOGIA`,
`REUMATOLOGIA`, `UROLOGIA`.

### Status de agendamento

`AGENDADO`, `CONFIRMADO`, `REALIZADO`, `CANCELADO`.

---

## Tratamento de erros

Os erros de negócio retornam um corpo JSON padronizado (`ErrorResponse`):

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Login já está em uso.",
  "timestamp": "2026-06-26T10:00:00",
  "fieldErrors": [ { "field": "login", "message": "Login é obrigatório" } ]
}
```

`fieldErrors` é preenchido apenas em erros de validação (`@Valid`); caso contrário vem vazio.

| Código | Significado                                                  |
|--------|-------------------------------------------------------------|
| 400    | Requisição inválida / regra de negócio violada / CAPTCHA    |
| 401    | Não autenticado ou credenciais inválidas                    |
| 403    | Sem permissão (papel insuficiente) ou conta desativada      |
| 404    | Recurso não encontrado                                      |
| 422    | Conta sem vínculo com paciente                              |
| 429    | Muitas tentativas (rate limit / IP bloqueado)               |
</content>
