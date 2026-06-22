const API = "";
const token = localStorage.getItem("token");

if (!token) {
    window.location.href = "index.html";
}

const messageBox = document.getElementById("message");

function showMessage(text, type) {
    messageBox.textContent = text;
    messageBox.className = "message " + type + " card full";
    setTimeout(() => (messageBox.className = "message hidden"), 5000);
}

async function apiFetch(path, options = {}) {
    const res = await fetch(API + path, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            Authorization: "Bearer " + token,
            ...(options.headers || {}),
        },
    });

    if (res.status === 401 || res.status === 403) {
        localStorage.removeItem("token");
        window.location.href = "index.html";
        throw new Error("Sessão expirada");
    }

    return res;
}

document.getElementById("btn-logout").addEventListener("click", () => {
    localStorage.removeItem("token");
    window.location.href = "index.html";
});

async function loadPatients(search = "") {
    const query = search ? `?search=${encodeURIComponent(search)}` : "";
    const res = await apiFetch(`/patients${query}`);
    const patients = await res.json();

    const tbody = document.getElementById("patients-body");
    const select = document.getElementById("app-paciente");

    if (patients.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3">Nenhum paciente encontrado.</td></tr>';
    } else {
        tbody.innerHTML = patients
            .map((p) => `<tr><td>${p.nome}</td><td>${p.cpf}</td><td>${p.id}</td></tr>`)
            .join("");
    }

    select.innerHTML =
        '<option value="">Selecione...</option>' +
        patients.map((p) => `<option value="${p.id}">${p.nome}</option>`).join("");
}

async function loadMedicos() {
    const res = await apiFetch("/users/medicos");
    const medicos = await res.json();
    const select = document.getElementById("app-medico");
    select.innerHTML =
        '<option value="">Selecione...</option>' +
        medicos.map((m) => `<option value="${m.id}">${m.login}</option>`).join("");
}

const STATUS_LABELS = {
    AGENDADO: "Agendado",
    CONFIRMADO: "Confirmado",
    CANCELADO: "Cancelado",
    REALIZADO: "Realizado",
};

function formatDateTime(iso) {
    if (!iso) return "—";
    return new Date(iso).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

function statusActions(a) {
    if (a.status === "CANCELADO" || a.status === "REALIZADO") return "—";
    const btns = [];
    if (a.status === "AGENDADO") {
        btns.push(`<button onclick="changeStatus('${a.id}','CONFIRMADO')">Confirmar</button>`);
    }
    if (a.status === "AGENDADO" || a.status === "CONFIRMADO") {
        btns.push(`<button onclick="changeStatus('${a.id}','REALIZADO')">Realizar</button>`);
        btns.push(`<button onclick="changeStatus('${a.id}','CANCELADO')">Cancelar</button>`);
    }
    return btns.join(" ");
}

async function loadAppointments() {
    const res = await apiFetch("/appointments");
    const list = await res.json();
    const tbody = document.getElementById("appointments-body");

    if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5">Nenhum agendamento.</td></tr>';
        return;
    }

    list.sort((a, b) => new Date(a.dataHora) - new Date(b.dataHora));
    tbody.innerHTML = list
        .map((a) => `<tr>
            <td>${a.nomePaciente}</td>
            <td>${a.nomeMedico}</td>
            <td>${formatDateTime(a.dataHora)}</td>
            <td>${STATUS_LABELS[a.status] || a.status}</td>
            <td>${statusActions(a)}</td>
        </tr>`)
        .join("");
}

async function changeStatus(id, status) {
    const res = await apiFetch(`/appointments/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status }),
    });
    if (res.ok) {
        showMessage("Status atualizado!", "success");
        loadAppointments();
    } else {
        const err = await res.json().catch(() => null);
        showMessage(err?.message || "Erro ao atualizar status.", "error");
    }
}

document.getElementById("btn-search").addEventListener("click", () => {
    loadPatients(document.getElementById("search-input").value);
});

document.getElementById("form-patient").addEventListener("submit", async (e) => {
    e.preventDefault();

    const body = {
        nome: document.getElementById("pat-nome").value,
        cpf: document.getElementById("pat-cpf").value,
        dataNascimento: document.getElementById("pat-nascimento").value,
        nomeMae: document.getElementById("pat-mae").value || null,
        nomePai: document.getElementById("pat-pai").value || null,
    };

    const res = await apiFetch("/patients", {
        method: "POST",
        body: JSON.stringify(body),
    });

    if (res.ok) {
        showMessage("Paciente cadastrado com sucesso!", "success");
        e.target.reset();
        loadPatients();
    } else {
        const err = await res.json().catch(() => null);
        showMessage(err?.message || "Erro ao cadastrar paciente (verifique o CPF).", "error");
    }
});

document.getElementById("form-appointment").addEventListener("submit", async (e) => {
    e.preventDefault();

    const body = {
        pacienteId: document.getElementById("app-paciente").value,
        medicoId: document.getElementById("app-medico").value,
        dataHora: document.getElementById("app-datahora").value,
    };

    const res = await apiFetch("/appointments", {
        method: "POST",
        body: JSON.stringify(body),
    });

    if (res.ok) {
        showMessage("Agendamento criado com sucesso!", "success");
        e.target.reset();
        loadAppointments();
    } else {
        const err = await res.json().catch(() => null);
        showMessage(err?.message || "Erro ao criar agendamento.", "error");
    }
});

loadPatients();
loadMedicos();
loadAppointments();
