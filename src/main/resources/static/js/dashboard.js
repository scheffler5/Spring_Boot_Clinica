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
        dataHora: document.getElementById("app-datahora").value,
    };

    const res = await apiFetch("/appointments", {
        method: "POST",
        body: JSON.stringify(body),
    });

    if (res.ok) {
        showMessage("Agendamento criado com sucesso!", "success");
        e.target.reset();
    } else {
        const err = await res.json().catch(() => null);
        showMessage(err?.message || "Erro ao criar agendamento.", "error");
    }
});

loadPatients();
