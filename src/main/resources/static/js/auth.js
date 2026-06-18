const API = "";


const msgBox = document.getElementById("message");

function showMsg(text, type) {
    msgBox.textContent = text;
    msgBox.className = "message " + type;
}

function clearMsg() {
    msgBox.className = "message hidden";
}


document.querySelectorAll(".tab").forEach(tab => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".tab").forEach(t => t.classList.remove("active"));
        tab.classList.add("active");
        clearMsg();

        const active = tab.dataset.tab;
        document.getElementById("form-login").classList.toggle("hidden", active !== "login");
        document.getElementById("mfa-step").classList.add("hidden");
        document.getElementById("email-verify-step").classList.add("hidden");
        document.getElementById("form-register").classList.toggle("hidden", active !== "register");
        document.getElementById("form-recovery-wrapper").classList.toggle("hidden", active !== "recovery");
    });
});


let captchaIdLogin = null;
let captchaIdReg   = null;

async function loadCaptcha(imgId, store) {
    const img = document.getElementById(imgId);
    img.src = "";
    img.style.background = "#eee";
    const res  = await fetch(`${API}/captcha/generate`);
    const data = await res.json();
    img.src = data.image;
    img.style.background = "transparent";
    if (store === "login") captchaIdLogin = data.captchaId;
    else                   captchaIdReg   = data.captchaId;
}

loadCaptcha("captcha-img-login", "login");
loadCaptcha("captcha-img-reg",   "reg");

document.getElementById("btn-refresh-login").addEventListener("click", () =>
    loadCaptcha("captcha-img-login", "login"));
document.getElementById("btn-refresh-reg").addEventListener("click", () =>
    loadCaptcha("captcha-img-reg", "reg"));


let mfaEmailGlobal = null;

document.getElementById("form-login").addEventListener("submit", async e => {
    e.preventDefault();
    clearMsg();

    const body = {
        login:       document.getElementById("login-user").value,
        password:    document.getElementById("login-pass").value,
        captchaId:   captchaIdLogin,
        captchaCode: document.getElementById("captcha-input-login").value.toUpperCase()
    };

    const res = await fetch(`${API}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });

    loadCaptcha("captcha-img-login", "login");
    document.getElementById("captcha-input-login").value = "";

    if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        showMsg(err.message || "Login inválido.", "error");
        return;
    }

    const data = await res.json();

    mfaEmailGlobal = data.email;

    document.getElementById("form-login").classList.add("hidden");
    document.getElementById("mfa-hint").textContent =
        `Código de acesso enviado para ${data.emailHint}. Verifique sua caixa de entrada.`;
    document.getElementById("mfa-email").value = data.emailHint;
    document.getElementById("mfa-step").classList.remove("hidden");

    if (window.initMfaStep) window.initMfaStep(data.emailHint, data.email);
    showMsg("Código MFA enviado por e-mail.", "info");
});


document.getElementById("btn-verify-mfa").addEventListener("click", async () => {
    clearMsg();

    const email   = mfaEmailGlobal;
    const mfaCode = document.getElementById("mfa-code").value.trim();

    if (!mfaCode) { showMsg("Informe o código MFA.", "error"); return; }

    const res = await fetch(`${API}/auth/verify-mfa`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, mfaCode })
    });

    if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        showMsg(err.message || "Código inválido.", "error");

        const match = (err.message || "").match(/(\d+)\s*tentativa/);
        const remaining = match ? parseInt(match[1]) : undefined;
        if (window.onMfaFailure) window.onMfaFailure(err.message, remaining);
        return;
    }

    const data = await res.json();
    localStorage.setItem("token", data.token);
    localStorage.setItem("role",  data.role);
    window.location.href = "dashboard.html";
});


let pendingRegEmail = null;

document.getElementById("form-register").addEventListener("submit", async e => {
    e.preventDefault();
    clearMsg();

    const email = document.getElementById("reg-email").value;

    const body = {
        login:       document.getElementById("reg-user").value,
        email,
        password:    document.getElementById("reg-pass").value,
        role:        document.getElementById("reg-role").value,
        captchaId:   captchaIdReg,
        captchaCode: document.getElementById("captcha-input-reg").value.toUpperCase()
    };

    const res = await fetch(`${API}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });

    loadCaptcha("captcha-img-reg", "reg");
    document.getElementById("captcha-input-reg").value = "";

    if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        showMsg(err.message || "Erro ao cadastrar.", "error");
        return;
    }

    pendingRegEmail = email;
    document.getElementById("form-register").classList.add("hidden");
    document.getElementById("verify-email").value = email;
    document.getElementById("email-verify-step").classList.remove("hidden");
    showMsg("Cadastro realizado! Verifique seu e-mail para ativar a conta.", "success");
});


document.getElementById("btn-verify-email").addEventListener("click", async () => {
    clearMsg();

    const email = pendingRegEmail;
    const code  = document.getElementById("verify-code").value.trim();

    if (!code) { showMsg("Informe o código de verificação.", "error"); return; }

    const res = await fetch(`${API}/auth/verify-email`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, code })
    });

    if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        showMsg(err.message || "Código inválido.", "error");
        return;
    }

    showMsg("E-mail verificado! Agora faça login.", "success");
    document.getElementById("email-verify-step").classList.add("hidden");
    document.querySelector('.tab[data-tab="login"]').click();
});


const stepRequest  = document.getElementById("form-recovery-request");
const stepValidate = document.getElementById("form-recovery-validate");
const stepChange   = document.getElementById("form-recovery-change");

let recoveryEmail = null;
let recoveryCode  = null;

stepRequest.addEventListener("submit", async e => {
    e.preventDefault(); clearMsg();
    recoveryEmail = document.getElementById("rec-email").value;

    const res = await fetch(`${API}/auth/request-recovery`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: recoveryEmail })
    });

    showMsg(await res.text().then(t => t.replace(/"/g, "")), res.ok ? "success" : "error");
    if (res.ok) { stepRequest.classList.add("hidden"); stepValidate.classList.remove("hidden"); }
});

stepValidate.addEventListener("submit", async e => {
    e.preventDefault(); clearMsg();
    recoveryCode = document.getElementById("rec-code").value;

    const res = await fetch(`${API}/auth/validate-recovery`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: recoveryEmail, code: recoveryCode })
    });

    if (res.ok) { stepValidate.classList.add("hidden"); stepChange.classList.remove("hidden"); }
    else { showMsg("Código inválido ou expirado.", "error"); }
});

stepChange.addEventListener("submit", async e => {
    e.preventDefault(); clearMsg();
    const newPassword = document.getElementById("rec-newpass").value;

    const res = await fetch(`${API}/auth/change-password`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: recoveryEmail, code: recoveryCode, newPassword })
    });

    if (res.ok) {
        stepChange.classList.add("hidden");
        stepRequest.classList.remove("hidden");
        document.querySelector('.tab[data-tab="login"]').click();
        showMsg("Senha alterada com sucesso! Faça login.", "success");
    } else {
        showMsg("Erro ao alterar senha.", "error");
    }
});
