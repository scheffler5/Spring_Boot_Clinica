# Referência da API

Base URL: `http://localhost:8080`

Todas as respostas de erro seguem o formato:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Descrição do erro",
  "timestamp": "2025-06-18T10:30:00",
  "fieldErrors": [
    { "field": "email", "message": "Formato de e-mail inválido" }
  ]
}
```

---

## Autenticação

### Fluxo de login (2 fatores)

```
POST /captcha/generate  →  obtém captchaId + imagem
POST /auth/login        →  valida senha + CAPTCHA → envia código MFA por e-mail
POST /auth/verify-mfa   →  valida código MFA → retorna JWT
```

O JWT retornado deve ser enviado em todas as requisições autenticadas:
```
Authorization: Bearer <token>
```

---

### CAPTCHA

#### `GET /captcha/generate`
Gera uma imagem CAPTCHA para ser exibida ao usuário.

**Autenticação:** não requerida

**Resposta `200`:**
```json
{
  "captchaId": "uuid-do-captcha",
  "image": "data:image/png;base64,iVBORw0..."
}
```

---

### Login

#### `POST /auth/login`
Primeiro fator de autenticação. Valida senha e CAPTCHA, envia código MFA por e-mail.

**Autenticação:** não requerida

**Body:**
```json
{
  "login": "gabriel",
  "password": "Admin@1234",
  "captchaId": "uuid-do-captcha",
  "captchaCode": "ABC123"
}
```

**Resposta `200`:**
```json
{
  "mfaRequired": true,
  "emailHint": "ga***@gmail.com",
  "email": "gabriel@gmail.com"
}
```

**Erros:**
| Status | Mensagem |
|---|---|
| 400 | CAPTCHA inválido ou expirado |
| 401 | Login ou senha inválidos. Tentativas restantes: N |
| 403 | Conta não ativada. Verifique seu e-mail |
| 429 | IP bloqueado por excesso de tentativas |

---

#### `POST /auth/verify-mfa`
Segundo fator — valida o código MFA enviado por e-mail e retorna o JWT.

**Autenticação:** não requerida

**Body:**
```json
{
  "email": "gabriel@gmail.com",
  "mfaCode": "123456"
}
```

**Resposta `200`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "ADMIN"
}
```

**Erros:**
| Status | Mensagem |
|---|---|
| 401 | Código MFA inválido. N tentativa(s) restante(s) |
| 429 | Tentativas esgotadas. Solicite um novo código |

---

#### `POST /auth/resend-mfa`
Reenvia o código MFA. Máximo 3 reenvios por sessão, cooldown de 60s entre reenvios.

**Autenticação:** não requerida

**Body:**
```json
{ "email": "gabriel@gmail.com" }
```

**Resposta `200`:**
```json
{
  "emailHint": "ga***@gmail.com",
  "cooldownSeconds": 60,
  "remainingResends": 2
}
```

---

### Cadastro de funcionário

#### `POST /auth/register`

**Autenticação:** não requerida

**Body:**
```json
{
  "login": "gabriel",
  "email": "gabriel@gmail.com",
  "password": "MinhaS3nha!",
  "role": "ADMIN",
  "captchaId": "uuid",
  "captchaCode": "ABC123"
}
```

Roles disponíveis: `ADMIN`, `MEDIC`, `RECEPCIONIST`

**Resposta `201`:** sem body

---

#### `POST /auth/verify-email`
Verifica o e-mail após o cadastro com o código enviado.

**Body:**
```json
{
  "email": "gabriel@gmail.com",
  "code": "123456"
}
```

**Resposta `200`:** `"E-mail verificado com sucesso."`

---

#### `POST /auth/resend-verification`
Reenvia o código de verificação de e-mail.

**Body:**
```json
{ "email": "gabriel@gmail.com" }
```

**Resposta `200`:** mensagem genérica (evita enumeração de usuários)

---

### Recuperação de senha

#### `POST /auth/request-recovery`
Solicita código de recuperação. Código válido por 15 minutos.

**Body:**
```json
{ "email": "gabriel@gmail.com" }
```

**Resposta `200`:** mensagem genérica

---

#### `POST /auth/validate-recovery`
Valida o código de recuperação recebido por e-mail.

**Body:**
```json
{
  "email": "gabriel@gmail.com",
  "code": "123456"
}
```

**Resposta `200`:** `"Código válido."`
**Resposta `400`:** código inválido ou expirado

---

#### `POST /auth/change-password`
Altera a senha usando o código de recuperação validado.

**Body:**
```json
{
  "email": "gabriel@gmail.com",
  "code": "123456",
  "newPassword": "NovaSenha@123"
}
```

**Resposta `200`:** `"Senha alterada com sucesso."`

---

#### `GET /auth/users`
Lista todos os usuários do sistema.

**Autenticação:** requerida — `ROLE_ADMIN`

**Resposta `200`:**
```json
[
  {
    "id": "uuid",
    "login": "gabriel",
    "email": "gabriel@gmail.com",
    "role": "ADMIN"
  }
]
```

---

## Cadastro de Paciente (portal público)

### `POST /patient/register`
Cadastro de paciente via portal público. Cria registro em `tb_patients` e conta em `tb_users` com role `PACIENTE`.

**Autenticação:** não requerida

**Body:**
```json
{
  "nome": "João da Silva",
  "cpf": "52998224725",
  "dataNascimento": "1990-05-15",
  "email": "joao@gmail.com",
  "password": "Senha@123",
  "captchaId": "uuid",
  "captchaCode": "ABC123"
}
```

**Resposta `201`:** sem body — e-mail de verificação enviado

**Notas:**
- CPF deve ser somente dígitos (11 caracteres)
- Se o CPF já existe em `tb_patients` mas sem conta, a conta é vinculada ao cadastro existente
- Login do paciente no sistema será o CPF (sem formatação)

---

## Portal do Paciente

Todos os endpoints abaixo requerem autenticação com JWT de role `PACIENTE`.

### `GET /patient/me`
Retorna o perfil do paciente autenticado.

**Resposta `200`:**
```json
{
  "id": "uuid",
  "nome": "João da Silva",
  "cpf": "52998224725",
  "dataNascimento": "1990-05-15",
  "ativo": true
}
```

---

### `GET /patient/appointments`
Retorna todos os agendamentos do paciente autenticado.

**Resposta `200`:**
```json
[
  {
    "id": "uuid",
    "pacienteId": "uuid",
    "nomePaciente": "João da Silva",
    "dataHora": "2025-07-10T14:30:00",
    "createdAt": "2025-06-01T09:00:00"
  }
]
```

---

### `GET /patient/prontuarios`
Retorna o histórico médico do paciente autenticado.

**Resposta `200`:**
```json
[
  {
    "id": "uuid",
    "nomePaciente": "João da Silva",
    "nomeMedico": "dra.ana",
    "nomeConvenio": "Unimed",
    "descricaoProcedimento": "Consulta clínica",
    "valorCalculado": 140.00,
    "observacoes": "Paciente com pressão elevada",
    "dataAtendimento": "2025-06-15T10:00:00"
  }
]
```

---

## Pacientes (funcionários)

Requer autenticação JWT.

### `POST /patients`
Cadastra um novo paciente.

**Body:**
```json
{
  "nome": "Maria Souza",
  "cpf": "529.982.247-25",
  "dataNascimento": "2010-03-01",
  "nomeMae": "Ana Souza",
  "nomePai": "Carlos Souza"
}
```

`nomeMae` e `nomePai` são obrigatórios para menores de 18 anos.

**Resposta `201`:** dados do paciente criado

---

### `GET /patients`
Lista pacientes ativos. Suporta busca por nome ou CPF.

**Query params:** `?search=maria`

**Resposta `200`:**
```json
[
  {
    "id": "uuid",
    "nome": "Maria Souza",
    "cpf": "52998224725",
    "dataNascimento": "2010-03-01",
    "ativo": true
  }
]
```

---

### `GET /patients/{id}`
Busca paciente por ID.

**Resposta `200`:** dados do paciente
**Resposta `404`:** paciente não encontrado

---

### `PUT /patients/{id}`
Atualiza dados do paciente.

**Body:** mesmo formato do POST

**Resposta `200`:** dados atualizados

---

### `DELETE /patients/{id}`
Desativa o paciente (soft delete — `ativo = false`).

**Autenticação:** `ROLE_ADMIN`

**Resposta `204`:** sem body

---

## Agendamentos

Requer autenticação JWT.

### `POST /appointments`

**Body:**
```json
{
  "pacienteId": "uuid-do-paciente",
  "dataHora": "2025-07-10T14:30:00"
}
```

`dataHora` deve ser no futuro. Não é permitido agendar horário já ocupado.

**Resposta `201`:** dados do agendamento

---

### `GET /appointments`
Lista todos os agendamentos.

---

### `GET /appointments/paciente/{pacienteId}`
Lista agendamentos de um paciente específico.

---

### `GET /appointments/{id}`
Busca agendamento por ID.

---

## Prontuários

Requer autenticação JWT com role `ADMIN` ou `MEDIC`.

### `POST /prontuarios`
Registra um atendimento. O valor final é calculado automaticamente aplicando o desconto do convênio ao custo do procedimento.

**Body:**
```json
{
  "idPaciente": "uuid",
  "idMedico": "uuid",
  "idConvenio": "uuid",
  "idProcedimento": "uuid",
  "observacoes": "Paciente relatou dores no peito"
}
```

**Resposta `201`:** prontuário criado com `valorCalculado`

---

### `GET /prontuarios`
Lista todos os prontuários.

### `GET /prontuarios/paciente/{pacienteId}`
Lista prontuários de um paciente.

### `GET /prontuarios/{id}`
Busca prontuário por ID.

---

## Convênios

Requer autenticação JWT. Criação/edição/exclusão requer `ROLE_ADMIN`.

### `POST /convenios`

**Body:**
```json
{
  "nome": "Unimed",
  "desconto": 0.30
}
```

`desconto` é um decimal entre 0.0 e 1.0 (ex: 0.30 = 30% de desconto).

**Resposta `201`:** convênio criado

---

### `GET /convenios`
Lista convênios ativos.

**Resposta `200`:**
```json
[
  { "id": "uuid", "nome": "Unimed", "desconto": 0.30, "ativo": true }
]
```

### `GET /convenios/{id}` — busca por ID
### `PUT /convenios/{id}` — atualiza (ADMIN)
### `DELETE /convenios/{id}` — desativa (ADMIN)

---

## Procedimentos

Requer autenticação JWT. Criação/edição/exclusão requer `ROLE_ADMIN`.

### `POST /procedimentos`

**Body:**
```json
{
  "descricao": "Consulta clínica",
  "custo": 200.00
}
```

**Resposta `201`:** procedimento criado

---

### `GET /procedimentos`
Lista procedimentos ativos.

**Resposta `200`:**
```json
[
  { "id": "uuid", "descricao": "Consulta clínica", "custo": 200.00, "ativo": true }
]
```

### `GET /procedimentos/{id}` — busca por ID
### `PUT /procedimentos/{id}` — atualiza (ADMIN)
### `DELETE /procedimentos/{id}` — desativa (ADMIN)

---

## Rotas públicas (sem autenticação)

| Rota | Método |
|---|---|
| `/captcha/generate` | GET |
| `/auth/login` | POST |
| `/auth/verify-mfa` | POST |
| `/auth/resend-mfa` | POST |
| `/auth/register` | POST |
| `/auth/verify-email` | POST |
| `/auth/resend-verification` | POST |
| `/auth/request-recovery` | POST |
| `/auth/validate-recovery` | POST |
| `/auth/change-password` | POST |
| `/patient/register` | POST |
| `/*.html`, `/css/**`, `/js/**` | GET |

---

## Rotas por role

| Role | Acesso adicional |
|---|---|
| Autenticado (qualquer role) | `/patients` GET/POST, `/appointments` todos, `/convenios` GET, `/procedimentos` GET |
| `MEDIC` + `ADMIN` | `/prontuarios` todos |
| `ADMIN` | `/patients` DELETE, `/convenios` POST/PUT/DELETE, `/procedimentos` POST/PUT/DELETE, `/auth/users` GET |
| `PACIENTE` | `/patient/me`, `/patient/appointments`, `/patient/prontuarios` |
