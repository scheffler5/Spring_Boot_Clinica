const API   = "";
const token = localStorage.getItem("token");
const role  = localStorage.getItem("role");

if (!token || (role !== "MEDIC" && role !== "ADMIN")) {
    window.location.href = "index.html";
}

document.getElementById("btn-logout").addEventListener("click", () => {
    ["token", "role", "perfilCompleto"].forEach(k => localStorage.removeItem(k));
    window.location.href = "index.html";
});

async function apiFetch(path, options = {}) {
    const res = await fetch(API + path, {
        ...options,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + token, ...(options.headers || {}) }
    });
    if (res.status === 401 || res.status === 403) { window.location.href = "index.html"; throw new Error("Sessão expirada"); }
    return res;
}

function showMsg(text, type) {
    const el = document.getElementById("message");
    el.textContent = text;
    el.className = "message " + type;
}

function clearFieldErrors() {
    document.querySelectorAll(".field-error").forEach(el => { el.textContent = ""; el.classList.add("hidden"); });
    document.querySelectorAll(".field-invalid").forEach(el => el.classList.remove("field-invalid"));
}

async function carregarEspecialidades() {
    const res  = await fetch(`${API}/medico/especialidades`);
    const data = await res.json();
    const sel  = document.getElementById("especialidade");
    data.forEach(({ value, label }) => {
        const opt = document.createElement("option");
        opt.value = value;
        opt.textContent = label;
        sel.appendChild(opt);
    });
}

async function carregarPerfilExistente() {
    try {
        const res = await apiFetch("/medico/perfil");
        if (!res.ok) return;
        const data = await res.json();
        if (data.nome)           document.getElementById("nome").value = data.nome;
        if (data.crm)            document.getElementById("crm").value  = data.crm;
        if (data.especialidade)  document.getElementById("especialidade").value = data.especialidade;
        if (data.valorConsulta)  document.getElementById("valorConsulta").value = data.valorConsulta;
    } catch {}
}

document.getElementById("form-profile").addEventListener("submit", async e => {
    e.preventDefault();
    clearFieldErrors();

    const body = {
        nome:                  document.getElementById("nome").value.trim(),
        crm:                   document.getElementById("crm").value.trim(),
        especialidade:         document.getElementById("especialidade").value,
        valorConsulta:         parseFloat(document.getElementById("valorConsulta").value),
        duracaoConsultaMinutos: parseInt(document.getElementById("duracaoConsultaMinutos").value)
    };

    const res  = await apiFetch("/medico/perfil", { method: "PATCH", body: JSON.stringify(body) });
    const data = await res.json().catch(() => ({}));

    if (!res.ok) {
        (data.fieldErrors || []).forEach(({ field, message }) => {
            const el = document.querySelector(`[data-field="${field}"]`);
            if (el) { el.textContent = message; el.classList.remove("hidden"); }
        });
        showMsg(data.message || "Erro ao salvar perfil.", "error");
        return;
    }

    localStorage.setItem("perfilCompleto", "true");
    window.location.href = "dashboard.html";
});

carregarEspecialidades();
carregarPerfilExistente();
