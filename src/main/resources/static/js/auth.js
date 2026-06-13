const API = "";

const messageBox = document.getElementById("message");

function showMessage(text, type) {
    messageBox.textContent = text;
    messageBox.className = "message " + type;
}

function clearMessage() {
    messageBox.className = "message hidden";
}

// ---------- Navegação entre abas ----------
document.querySelectorAll(".tab").forEach((tab) => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
        tab.classList.add("active");
        clearMessage();

        document.getElementById("form-login").classList.toggle("hidden", tab.dataset.tab !== "login");
        document.getElementById("form-register").classList.toggle("hidden", tab.dataset.tab !== "register");
        document.getElementById("form-recovery-wrapper").classList.toggle("hidden", tab.dataset.tab !== "recovery");
    });
});

// ---------- Login ----------
document.getElementById("form-login").addEventListener("submit", async (e) => {
    e.preventDefault();
    clearMessage();

    const body = {
        login: document.getElementById("login-user").value,
        password: document.getElementById("login-pass").value,
    };

    try {
        const res = await fetch(`${API}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
        });

        if (!res.ok) {
            showMessage("Login ou senha inválidos.", "error");
            return;
        }

        const data = await res.json();
        localStorage.setItem("token", data.token);
        window.location.href = "dashboard.html";
    } catch (err) {
        showMessage("Erro de conexão com o servidor.", "error");
    }
});

// ---------- Cadastro ----------
document.getElementById("form-register").addEventListener("submit", async (e) => {
    e.preventDefault();
    clearMessage();

    const body = {
        login: document.getElementById("reg-user").value,
        email: document.getElementById("reg-email").value,
        password: document.getElementById("reg-pass").value,
        role: document.getElementById("reg-role").value,
    };

    try {
        const res = await fetch(`${API}/auth/register`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
        });

        if (res.ok) {
            showMessage("Usuário cadastrado! Agora faça login.", "success");
        } else {
            showMessage("Não foi possível cadastrar (login já existe?).", "error");
        }
    } catch (err) {
        showMessage("Erro de conexão com o servidor.", "error");
    }
});

// ---------- Recuperação de senha (3 etapas) ----------
const stepRequest = document.getElementById("form-recovery-request");
const stepValidate = document.getElementById("form-recovery-validate");
const stepChange = document.getElementById("form-recovery-change");

stepRequest.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearMessage();

    const email = document.getElementById("rec-email").value;
    const res = await fetch(`${API}/auth/request-recovery`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
    });

    if (res.ok) {
        showMessage("Código enviado para o e-mail.", "success");
        stepRequest.classList.add("hidden");
        stepValidate.classList.remove("hidden");
    } else {
        showMessage("E-mail não encontrado no sistema.", "error");
    }
});

stepValidate.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearMessage();

    const email = document.getElementById("rec-email").value;
    const code = document.getElementById("rec-code").value;
    const res = await fetch(`${API}/auth/validate-recovery`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, code }),
    });

    if (res.ok) {
        showMessage("Código válido. Defina a nova senha.", "success");
        stepValidate.classList.add("hidden");
        stepChange.classList.remove("hidden");
    } else {
        showMessage("Código inválido ou expirado.", "error");
    }
});

stepChange.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearMessage();

    const email = document.getElementById("rec-email").value;
    const code = document.getElementById("rec-code").value;
    const newPassword = document.getElementById("rec-newpass").value;
    const res = await fetch(`${API}/auth/change-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, code, newPassword }),
    });

    if (res.ok) {
        stepChange.classList.add("hidden");
        stepRequest.classList.remove("hidden");
        document.querySelector('.tab[data-tab="login"]').click();
        showMessage("Senha alterada com sucesso! Faça login.", "success");
    } else {
        showMessage("Código inválido ou e-mail incorreto.", "error");
    }
});
