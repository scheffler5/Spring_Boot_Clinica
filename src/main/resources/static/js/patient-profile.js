const API   = "";
const token = localStorage.getItem("token");
const role  = localStorage.getItem("role");

if (!token || role !== "PACIENTE") {
    localStorage.clear();
    window.location.href = "patient-login.html";
}

// Perfil já completo? Vai direto para o marketplace.
if (localStorage.getItem("perfilCompleto") === "true") {
    window.location.href = "patient-marketplace.html";
}

const msgBox = document.getElementById("message");

function showMsg(text, type) {
    msgBox.textContent = text;
    msgBox.className = "message " + type;
}

function clearFieldErrors() {
    document.querySelectorAll(".field-error").forEach(el => { el.textContent = ""; el.classList.add("hidden"); });
    document.querySelectorAll("input.field-invalid").forEach(el => el.classList.remove("field-invalid"));
}

function showFieldErrors(fieldErrors) {
    if (!fieldErrors?.length) return;
    fieldErrors.forEach(({ field, message }) => {
        const input = document.getElementById(field);
        const err   = document.querySelector(`[data-field="${field}"]`);
        if (input) input.classList.add("field-invalid");
        if (err)   { err.textContent = message; err.classList.remove("hidden"); }
    });
}

function setFieldError(id, message) {
    const input = document.getElementById(id);
    const err   = document.querySelector(`[data-field="${id}"]`);
    if (input) input.classList.add("field-invalid");
    if (err)   { err.textContent = message; err.classList.remove("hidden"); }
}

document.getElementById("btn-logout").addEventListener("click", () => {
    localStorage.clear();
    window.location.href = "patient-login.html";
});

// Máscara simples de CPF
const cpfInput = document.getElementById("cpf");
cpfInput.addEventListener("input", () => {
    let v = cpfInput.value.replace(/\D/g, "").slice(0, 11);
    v = v.replace(/(\d{3})(\d)/, "$1.$2")
         .replace(/(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
         .replace(/(\d{3})\.(\d{3})\.(\d{3})(\d)/, "$1.$2.$3-$4");
    cpfInput.value = v;
});

document.getElementById("form-profile").addEventListener("submit", async e => {
    e.preventDefault();
    clearFieldErrors();
    msgBox.className = "message hidden";

    const nome           = document.getElementById("nome").value.trim();
    const cpf            = cpfInput.value.replace(/\D/g, "");
    const dataNascimento = document.getElementById("dataNascimento").value;
    const nomeMae        = document.getElementById("nomeMae").value.trim();

    let valid = true;
    if (nome.length < 3)            { setFieldError("nome", "Informe seu nome completo"); valid = false; }
    if (cpf.length !== 11)          { setFieldError("cpf", "CPF deve ter 11 dígitos"); valid = false; }
    if (!dataNascimento)            { setFieldError("dataNascimento", "Informe a data de nascimento"); valid = false; }
    if (nomeMae.length < 3)         { setFieldError("nomeMae", "Informe o nome da mãe"); valid = false; }
    if (!valid) return;

    const res = await fetch(`${API}/patient/complete-profile`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + token },
        body: JSON.stringify({ nome, cpf, dataNascimento, nomeMae })
    });

    if (res.status === 401 || res.status === 403) {
        localStorage.clear();
        window.location.href = "patient-login.html";
        return;
    }

    const data = await res.json().catch(() => null);

    if (!res.ok) {
        if (data?.fieldErrors) showFieldErrors(data.fieldErrors);
        showMsg(data?.message || "Não foi possível salvar. Verifique os dados.", "error");
        return;
    }

    localStorage.setItem("perfilCompleto", "true");
    showMsg("Cadastro completo! Redirecionando...", "success");
    setTimeout(() => { window.location.href = "patient-marketplace.html"; }, 800);
});
