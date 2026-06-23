/**
 * Chat Widget — botão flutuante com painel expansível.
 * Inclua em qualquer página autenticada (médico ou paciente).
 *
 *   <link rel="stylesheet" href="css/chat-widget.css">
 *   <script src="js/sockjs.min.js"></script>
 *   <script src="js/stomp.umd.min.js"></script>
 *   <script src="js/chat-widget.js"></script>
 */
(function () {
    const API   = "";
    const token = localStorage.getItem("token");
    if (!token) return;

    const meId = (() => {
        try { return JSON.parse(atob(token.split(".")[1])).id; } catch { return null; }
    })();

    let stomp           = null;
    let activeSub       = null;
    let conversaId      = null;
    let panelAberto     = false;
    let pendingAnexo    = null; // { url, nome, tipo }

    // ── DOM ──────────────────────────────────────────────────────────

    function injetar() {
        const btn = document.createElement("button");
        btn.id = "cw-btn";
        btn.innerHTML = `<span>💬</span><span id="cw-unread" style="display:none"></span>`;
        btn.onclick = togglePanel;
        document.body.appendChild(btn);

        const panel = document.createElement("div");
        panel.id = "cw-panel";
        panel.className = "hidden";
        panel.innerHTML = `
            <div id="cw-header">
                <button id="cw-header-back" style="display:none" onclick="window._cw.voltarContatos()">←</button>
                <span id="cw-header-title">💬 Conversas</span>
                <button id="cw-header-close" onclick="window._cw.fechar()">✕</button>
            </div>
            <div id="cw-body"></div>`;
        document.body.appendChild(panel);

        window._cw = { voltarContatos, fechar };
        carregarContatos();
        carregarNaoLidas();
    }

    function togglePanel() {
        panelAberto = !panelAberto;
        document.getElementById("cw-panel").classList.toggle("hidden", !panelAberto);
        if (panelAberto) carregarContatos();
    }

    window._cw = { voltarContatos: () => {}, fechar: () => { panelAberto = false; document.getElementById("cw-panel").classList.add("hidden"); } };

    // ── Contatos ─────────────────────────────────────────────────────

    async function carregarContatos() {
        setHeader("💬 Conversas", false);
        const body = document.getElementById("cw-body");
        body.style.cssText = "";
        body.innerHTML = '<div class="cw-empty"><span>⏳</span><p>Carregando...</p></div>';

        try {
            const res  = await apiFetch("/chat/contatos");
            const list = await res.json();

            if (!Array.isArray(list) || list.length === 0) {
                body.innerHTML = '<div class="cw-empty"><span>💬</span><p>Nenhum contato disponível.<br>Agende uma consulta para conversar.</p></div>';
                return;
            }

            body.innerHTML = list.map(c => `
                <div class="cw-contact" onclick="window._cwAbrir('${c.usuarioId}','${c.nome}','${c.fotoUrl||''}','${c.conversaId||''}')">
                    <div class="cw-avatar">
                        ${c.fotoUrl ? `<img src="${c.fotoUrl}" alt="${c.nome}">` : "🩺"}
                    </div>
                    <div class="cw-contact-info">
                        <div class="cw-contact-nome">${c.nome}</div>
                        <div class="cw-contact-sub">${c.tipo === "MEDICO" ? "Médico" : "Paciente"}</div>
                    </div>
                </div>`).join("");
        } catch {
            body.innerHTML = '<div class="cw-empty"><span>⚠️</span><p>Erro ao carregar contatos.</p></div>';
        }
    }

    window._cwAbrir = async function(contatoId, nome, foto, convId) {
        setHeader(nome, true, foto);
        const body = document.getElementById("cw-body");
        body.style.cssText = "display:flex;flex-direction:column;height:100%";
        pendingAnexo = null;
        body.innerHTML = `
            <div id="cw-msgs" style="flex:1;overflow-y:auto;padding:14px 14px 10px;display:flex;flex-direction:column;gap:6px"></div>
            <div id="cw-anexo-preview" style="display:none;padding:6px 12px;background:#f0f4f8;border-top:1px solid #eee;font-size:12px;align-items:center;gap:8px">
                <span id="cw-anexo-info" style="flex:1;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"></span>
                <button onclick="window._cwRemoverAnexo()" style="background:none;border:none;color:#e53935;cursor:pointer;font-size:16px;padding:0">✕</button>
            </div>
            <div id="cw-input-row">
                <button id="cw-attach" title="Anexar arquivo" onclick="document.getElementById('cw-file-input').click()" style="width:34px;height:34px;border-radius:50%;background:none;border:1px solid #ddd;cursor:pointer;font-size:16px;display:flex;align-items:center;justify-content:center;flex-shrink:0">📎</button>
                <input type="file" id="cw-file-input" style="display:none" accept="image/*,.pdf,.doc,.docx" onchange="window._cwSelecionarAnexo(this)">
                <textarea id="cw-input" rows="1" placeholder="Mensagem..."
                    oninput="this.style.height='auto';this.style.height=Math.min(this.scrollHeight,80)+'px'"
                    onkeydown="if(event.key==='Enter'&&!event.shiftKey){event.preventDefault();window._cwEnviar()}"></textarea>
                <button id="cw-send" onclick="window._cwEnviar()">➤</button>
            </div>`;

        try {
            let convData;
            if (convId) {
                const res = await apiFetch(`/chat/conversas/${convId}/mensagens`);
                conversaId = convId;
                const msgs = await res.json();
                msgs.forEach(m => appendMsg(m, m.remetenteId === meId));
            } else {
                const res = await apiFetch(`/chat/conversas/com/${contatoId}`, { method: "POST" });
                convData   = await res.json();
                conversaId = convData.id.toString();
                const res2 = await apiFetch(`/chat/conversas/${conversaId}/mensagens`);
                const msgs = await res2.json();
                msgs.forEach(m => appendMsg(m, m.remetenteId === meId));
            }

            apiFetch(`/chat/conversas/${conversaId}/lidas`, { method: "PATCH" });
            scrollDown();
            inscrever();
            document.getElementById("cw-input")?.focus();
        } catch (e) {
            document.getElementById("cw-msgs").innerHTML =
                '<div class="cw-empty"><span>⚠️</span><p>Erro ao abrir conversa.</p></div>';
        }
    };

    window._cwSelecionarAnexo = async function(input) {
        const file = input.files[0];
        if (!file) return;
        const preview = document.getElementById("cw-anexo-preview");
        const info    = document.getElementById("cw-anexo-info");
        info.textContent = "⏳ Enviando " + file.name + "...";
        preview.style.display = "flex";

        const form = new FormData();
        form.append("arquivo", file);
        try {
            const res = await apiFetch("/chat/upload", {
                method:  "POST",
                headers: { Authorization: "Bearer " + token },
                body:    form
            });
            if (!res.ok) { info.textContent = "❌ Erro no upload"; return; }
            const data  = await res.json();
            pendingAnexo = data; // { url, nome, tipo }
            const icon  = data.tipo === "imagem" ? "🖼" : "📄";
            info.textContent = icon + " " + data.nome;
        } catch {
            info.textContent = "❌ Erro no upload";
            pendingAnexo = null;
        }
        input.value = "";
    };

    window._cwRemoverAnexo = function() {
        pendingAnexo = null;
        const preview = document.getElementById("cw-anexo-preview");
        if (preview) preview.style.display = "none";
    };

    window._cwEnviar = function() {
        if (!stomp?.connected || !conversaId) return;
        const input = document.getElementById("cw-input");
        const texto = input?.value.trim() || "";
        if (!texto && !pendingAnexo) return;

        const payload = { texto };
        if (pendingAnexo) {
            payload.imagemUrl = pendingAnexo.url;
            payload.nomeAnexo = pendingAnexo.nome;
        }

        stomp.publish({ destination: `/app/chat/${conversaId}`, body: JSON.stringify(payload) });
        input.value = "";
        input.style.height = "auto";
        window._cwRemoverAnexo();
    };

    function voltarContatos() {
        if (activeSub) { activeSub.unsubscribe(); activeSub = null; }
        conversaId = null;
        carregarContatos();
    }

    function fechar() {
        panelAberto = false;
        document.getElementById("cw-panel").classList.add("hidden");
    }

    window._cw = { voltarContatos, fechar };

    // ── Mensagens ─────────────────────────────────────────────────────

    function appendMsg(m, euSou) {
        const msgs = document.getElementById("cw-msgs");
        if (!msgs) return;
        const hora = m.timestamp ? new Date(m.timestamp).toLocaleTimeString("pt-BR",{hour:"2-digit",minute:"2-digit"}) : "";

        let conteudo = "";
        if (m.imagemUrl) {
            const nome = m.nomeAnexo || "arquivo";
            const ext  = nome.split(".").pop().toLowerCase();
            const isImg = ["jpg","jpeg","png","webp","gif"].includes(ext);
            if (isImg) {
                conteudo += `<img src="${m.imagemUrl}" style="max-width:100%;border-radius:8px;display:block;margin-bottom:4px;cursor:pointer" onclick="window.open('${m.imagemUrl}','_blank')">`;
            } else {
                conteudo += `<a href="${m.imagemUrl}" target="_blank" style="display:flex;align-items:center;gap:6px;color:inherit;text-decoration:none;margin-bottom:4px"><span style="font-size:20px">📄</span><span style="font-size:12px;text-decoration:underline">${esc(nome)}</span></a>`;
            }
        }
        if (m.texto) conteudo += esc(m.texto);

        const d = document.createElement("div");
        d.innerHTML = `
            ${!euSou ? `<div class="cw-nome-r">${m.remetenteNome}</div>` : ""}
            <div class="cw-bubble ${euSou ? 'eu' : 'outro'}">
                ${conteudo}<span class="cw-hora">${hora}</span>
            </div>`;
        msgs.appendChild(d);
        scrollDown();
    }

    function scrollDown() {
        const el = document.getElementById("cw-msgs");
        if (el) el.scrollTop = el.scrollHeight;
    }

    function esc(t) {
        return t.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/\n/g,"<br>");
    }

    // ── WebSocket ─────────────────────────────────────────────────────

    function conectar() {
        if (stomp) return;
        stomp = new StompJs.Client({
            webSocketFactory: () => new SockJS(`${API}/ws`),
            connectHeaders:   { Authorization: "Bearer " + token },
            reconnectDelay:   5000,
            onConnect:        () => { if (conversaId) inscrever(); },
            onStompError:     (e) => console.warn("STOMP:", e)
        });
        stomp.activate();
    }

    function inscrever() {
        if (!stomp?.connected || !conversaId) return;
        if (activeSub) activeSub.unsubscribe();
        activeSub = stomp.subscribe(`/topic/conversa/${conversaId}`, (msg) => {
            const m = JSON.parse(msg.body);
            appendMsg(m, m.remetenteId === meId);
            if (!panelAberto || !document.getElementById("cw-msgs")) {
                incrementarBadge();
            } else {
                apiFetch(`/chat/conversas/${conversaId}/lidas`, { method: "PATCH" });
            }
        });
    }

    // ── Badge de não lidas ────────────────────────────────────────────

    async function carregarNaoLidas() {
        try {
            const res  = await apiFetch("/chat/conversas");
            const list = await res.json();
            if (!Array.isArray(list)) return;
            const total = list.reduce((s, c) => s + (c.naoLidas || 0), 0);
            atualizarBadge(total);
        } catch {}
    }

    function atualizarBadge(n) {
        const el = document.getElementById("cw-unread");
        if (!el) return;
        el.style.display = n > 0 ? "flex" : "none";
        el.textContent   = n > 9 ? "9+" : String(n);
    }

    function incrementarBadge() {
        const el = document.getElementById("cw-unread");
        if (!el) return;
        const atual = parseInt(el.textContent) || 0;
        atualizarBadge(atual + 1);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    function setHeader(titulo, mostrarVoltar, foto) {
        document.getElementById("cw-header-title").textContent = titulo;
        const back = document.getElementById("cw-header-back");
        back.style.display = mostrarVoltar ? "block" : "none";
    }

    async function apiFetch(path, opts = {}) {
        const isFormData = opts.body instanceof FormData;
        const headers = isFormData
            ? { Authorization: "Bearer " + token, ...(opts.headers || {}) }
            : { "Content-Type": "application/json", Authorization: "Bearer " + token, ...(opts.headers || {}) };
        return fetch(API + path, { ...opts, headers });
    }

    // ── Init ──────────────────────────────────────────────────────────

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", () => { injetar(); conectar(); });
    } else {
        injetar(); conectar();
    }

})();
