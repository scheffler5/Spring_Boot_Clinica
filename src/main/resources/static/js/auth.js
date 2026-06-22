const API = "";

const msgBox = document.getElementById("message");

function showMsg(text, type) { msgBox.textContent = text; msgBox.className = "message " + type; }
function clearMsg()          { msgBox.className = "message hidden"; }

function clearFieldErrors() {
    document.querySelectorAll(".field-error").forEach(el => { el.textContent = ""; el.classList.add("hidden"); });
    document.querySelectorAll("input.field-invalid, select.field-invalid").forEach(el => el.classList.remove("field-invalid"));
}

function showFieldErrors(fieldErrors) {
    if (!fieldErrors?.length) return;
    const map = { login: "login-user", password: "login-pass" };
    fieldErrors.forEach(({ field, message }) => {
        const id    = map[field] || field;
        const input = document.getElementById(id);
        const err   = document.querySelector(`[data-field="${id}"]`);
        if (input) input.classList.add("field-invalid");
        if (err)   { err.textContent = message; err.classList.remove("hidden"); }
    });
}

function validateLocal(fields) {
    clearFieldErrors();
    let valid = true;
    fields.forEach(({ id, checks }) => {
        const el  = document.getElementById(id);
        const err = document.querySelector(`[data-field="${id}"]`);
        if (!el) return;
        const val = el.value.trim();
        for (const { test, msg } of checks) {
            if (!test(val)) {
                el.classList.add("field-invalid");
                if (err) { err.textContent = msg; err.classList.remove("hidden"); }
                valid = false; break;
            }
        }
    });
    return valid;
}

document.querySelectorAll(".tab").forEach(tab => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".tab").forEach(t => t.classList.remove("active"));
        tab.classList.add("active");
        clearMsg(); clearFieldErrors();
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
            .catch(() => { box.className = "captcha-box"; box.innerHTML = ""; showMsg("Sem conexão.", "error"); });
    };
}

initCaptcha("captcha-box-login", "btn-login-submit", "login");
initCaptcha("captcha-box-reg",   "btn-reg-submit",   "reg");

function redirectAfterLogin(perfilCompleto) {
    window.location.href = perfilCompleto ? "dashboard.html" : "doctor-profile-complete.html";
}

document.getElementById("form-login").addEventListener("submit", async e => {
    e.preventDefault(); clearMsg();
    const ok = validateLocal([
        { id: "login-user", checks: [{ test: v => v.length > 0, msg: "Login é obrigatório" }] },
        { id: "login-pass", checks: [{ test: v => v.length > 0, msg: "Senha é obrigatória" }] }
    ]);
    if (!ok) return;
    if (!powLogin) { showMsg("Clique em 'Não sou um robô' antes de continuar.", "error"); return; }

    const body = {
        login: document.getElementById("login-user").value,
        password: document.getElementById("login-pass").value,
        captchaId: powLogin.challengeId,
        captchaCode: powLogin.nonce
    };
    initCaptcha("captcha-box-login", "btn-login-submit", "login");

    const res  = await fetch(`${API}/auth/login`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) { showFieldErrors(data.fieldErrors); showMsg(data.message || "Erro ao entrar.", "error"); return; }

    localStorage.setItem("token",         data.token);
    localStorage.setItem("role",          data.role);
    localStorage.setItem("perfilCompleto", String(data.perfilCompleto));
    redirectAfterLogin(data.perfilCompleto);
});

document.getElementById("form-register").addEventListener("submit", async e => {
    e.preventDefault(); clearMsg();
    const ok = validateLocal([
        { id: "reg-user", checks: [
            { test: v => v.length > 0,  msg: "Login é obrigatório" },
            { test: v => v.length >= 3, msg: "Login deve ter no mínimo 3 caracteres" }
        ]},
        { id: "reg-pass", checks: [
            { test: v => v.length > 0,  msg: "Senha é obrigatória" },
            { test: v => v.length >= 8, msg: "A senha deve ter no mínimo 8 caracteres" }
        ]}
    ]);
    if (!ok) return;
    if (!powReg) { showMsg("Clique em 'Não sou um robô' antes de continuar.", "error"); return; }

    const body = {
        login: document.getElementById("reg-user").value,
        password: document.getElementById("reg-pass").value,
        captchaId: powReg.challengeId,
        captchaCode: powReg.nonce
    };
    initCaptcha("captcha-box-reg", "btn-reg-submit", "reg");

    const res  = await fetch(`${API}/auth/register`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
    const data = await res.json().catch(() => null);
    if (!res.ok) { if (data?.fieldErrors) showFieldErrors(data.fieldErrors); showMsg(data?.message || "Erro ao cadastrar.", "error"); return; }

    showMsg("Conta criada! Faça login.", "success");
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
