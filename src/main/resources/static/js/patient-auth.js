const API = "";

const msgBox = document.getElementById("message");

function showMsg(text, type) {
    msgBox.textContent = text;
    msgBox.className = "message " + type;
}

function clearMsg() { msgBox.className = "message hidden"; }

function clearFieldErrors() {
    document.querySelectorAll(".field-error").forEach(el => {
        el.textContent = "";
        el.classList.add("hidden");
    });
    document.querySelectorAll("input.field-invalid").forEach(el => el.classList.remove("field-invalid"));
}

function showFieldErrors(fieldErrors) {
    if (!fieldErrors || fieldErrors.length === 0) return;
    const fieldMap = { nome: "reg-nome", cpf: "reg-cpf", dataNascimento: "reg-nascimento", email: "reg-email", password: "reg-pass" };
    fieldErrors.forEach(({ field, message }) => {
        const inputId = fieldMap[field] || field;
        const input   = document.getElementById(inputId);
        const errEl   = document.querySelector(`[data-field="${inputId}"]`);
        if (input)  input.classList.add("field-invalid");
        if (errEl)  { errEl.textContent = message; errEl.classList.remove("hidden"); }
    });
}

function validateLocal(fields) {
    clearFieldErrors();
    let valid = true;
    fields.forEach(({ id, checks }) => {
        const el    = document.getElementById(id);
        const errEl = document.querySelector(`[data-field="${id}"]`);
        if (!el) return;
        const val = el.value.trim();
        for (const { test, msg } of checks) {
            if (!test(val)) {
                el.classList.add("field-invalid");
                if (errEl) { errEl.textContent = msg; errEl.classList.remove("hidden"); }
                valid = false;
                break;
            }
        }
    });
    return valid;
}

document.querySelectorAll(".tab").forEach(tab => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".tab").forEach(t => t.classList.remove("active"));
        tab.classList.add("active");
        clearMsg();
        clearFieldErrors();
        const active = tab.dataset.tab;
        document.getElementById("form-login").classList.toggle("hidden", active !== "login");
        document.getElementById("form-register").classList.toggle("hidden", active !== "register");
    });
});

let powLogin = null;
let powReg   = null;

function initCaptcha(boxId, submitId, store) {
    const box    = document.getElementById(boxId);
    const submit = document.getElementById(submitId);

    submit.disabled = true;
    box.className   = "captcha-box";
    box.innerHTML   = "";
    box.setAttribute("aria-checked", "false");

    if (store === "login") powLogin = null;
    else                   powReg   = null;

    box.onclick = () => {
        if (box.classList.contains("loading") || box.classList.contains("done")) return;

        box.className = "captcha-box loading";
        box.innerHTML = '<div class="captcha-spinner"></div>';

        fetch(`${API}/captcha/generate`)
            .then(r => r.json())
            .then(({ challengeId, challenge, difficulty }) => {
                const worker = new Worker("js/pow-worker.js");
                worker.onmessage = ({ data: { nonce } }) => {
                    if (store === "login") powLogin = { challengeId, nonce };
                    else                   powReg   = { challengeId, nonce };
                    box.className = "captcha-box done";
                    box.innerHTML = '<span class="captcha-check">✓</span>';
                    box.setAttribute("aria-checked", "true");
                    submit.disabled = false;
                    worker.terminate();
                };
                worker.onerror = () => {
                    box.className = "captcha-box";
                    box.innerHTML = "";
                    showMsg("Erro na verificação. Tente novamente.", "error");
                };
                worker.postMessage({ challenge, difficulty });
            })
            .catch(() => {
                box.className = "captcha-box";
                box.innerHTML = "";
                showMsg("Sem conexão com o servidor.", "error");
            });
    };
}

initCaptcha("captcha-box-login", "btn-login-submit", "login");
initCaptcha("captcha-box-reg",   "btn-reg-submit",   "reg");

document.getElementById("form-login").addEventListener("submit", async e => {
    e.preventDefault();
    clearMsg();

    const ok = validateLocal([
        { id: "login-cpf",  checks: [
            { test: v => v.length > 0,                          msg: "CPF é obrigatório" },
            { test: v => /^\d{11}$/.test(v.replace(/\D/g, "")), msg: "CPF deve ter 11 dígitos" }
        ]},
        { id: "login-pass", checks: [{ test: v => v.length > 0, msg: "Senha é obrigatória" }] }
    ]);
    if (!ok) return;
    if (!powLogin) { showMsg("Clique em 'Não sou um robô' antes de continuar.", "error"); return; }

    const cpf  = document.getElementById("login-cpf").value.replace(/\D/g, "");
    const body = {
        login:       cpf,
        password:    document.getElementById("login-pass").value,
        captchaId:   powLogin.challengeId,
        captchaCode: powLogin.nonce
    };

    initCaptcha("captcha-box-login", "btn-login-submit", "login");

    const res  = await fetch(`${API}/auth/login`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });
    const data = await res.json().catch(() => ({}));

    if (!res.ok) {
        showFieldErrors(data.fieldErrors);
        showMsg(data.message || "CPF ou senha inválidos.", "error");
        return;
    }

    localStorage.setItem("token", data.token);
    localStorage.setItem("role",  data.role);
    window.location.href = "patient-dashboard.html";
});

document.getElementById("form-register").addEventListener("submit", async e => {
    e.preventDefault();
    clearMsg();

    const ok = validateLocal([
        { id: "reg-nome",       checks: [
            { test: v => v.length > 0,  msg: "Nome é obrigatório" },
            { test: v => v.length >= 3, msg: "Nome deve ter no mínimo 3 caracteres" }
        ]},
        { id: "reg-cpf",        checks: [
            { test: v => v.length > 0,                          msg: "CPF é obrigatório" },
            { test: v => /^\d{11}$/.test(v.replace(/\D/g,"")), msg: "CPF deve ter 11 dígitos" }
        ]},
        { id: "reg-nascimento", checks: [{ test: v => v.length > 0, msg: "Data de nascimento é obrigatória" }] },
        { id: "reg-email",      checks: [
            { test: v => v.length > 0, msg: "E-mail é obrigatório" },
            { test: v => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v), msg: "Formato de e-mail inválido" }
        ]},
        { id: "reg-pass",       checks: [
            { test: v => v.length > 0,  msg: "Senha é obrigatória" },
            { test: v => v.length >= 8, msg: "Senha deve ter no mínimo 8 caracteres" }
        ]}
    ]);
    if (!ok) return;
    if (!powReg) { showMsg("Clique em 'Não sou um robô' antes de continuar.", "error"); return; }

    const cpf  = document.getElementById("reg-cpf").value.replace(/\D/g, "");
    const body = {
        nome:           document.getElementById("reg-nome").value,
        cpf,
        dataNascimento: document.getElementById("reg-nascimento").value,
        email:          document.getElementById("reg-email").value,
        password:       document.getElementById("reg-pass").value,
        captchaId:      powReg.challengeId,
        captchaCode:    powReg.nonce
    };

    initCaptcha("captcha-box-reg", "btn-reg-submit", "reg");

    const res  = await fetch(`${API}/patient/register`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });
    const data = await res.json().catch(() => null);

    if (!res.ok) {
        if (data?.fieldErrors) showFieldErrors(data.fieldErrors);
        showMsg(data?.message || "Erro ao cadastrar.", "error");
        return;
    }

    showMsg("Conta criada! Faça login com seu CPF.", "success");
    document.querySelector('.tab[data-tab="login"]').click();
});

document.querySelectorAll(".toggle-pass").forEach(btn => {
    btn.addEventListener("click", () => {
        const input = btn.previousElementSibling;
        const show  = input.type === "password";
        input.type  = show ? "text" : "password";
        btn.textContent = show ? "🙈" : "👁";
        btn.setAttribute("aria-label", show ? "Ocultar senha" : "Mostrar senha");
    });
});
