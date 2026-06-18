(function () {

    const step = document.getElementById("mfa-step");

    const panel = document.createElement("div");
    panel.id = "mfa-status-panel";
    panel.style.cssText = "margin-top:12px;font-size:13px;";
    panel.innerHTML = `
        <div id="mfa-attempts-info" style="color:var(--muted);margin-bottom:10px"></div>
        <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
            <button id="btn-resend-mfa" class="btn-secondary" style="flex-shrink:0">
                ↺ Reenviar código
            </button>
            <span id="resend-status" style="color:var(--muted);font-size:12px"></span>
        </div>
        <div id="mfa-expiry-info" style="color:var(--muted);font-size:12px;margin-top:6px"></div>
    `;

    const btnVerify = document.getElementById("btn-verify-mfa");
    btnVerify.parentNode.insertBefore(panel, btnVerify.nextSibling);

    let mfaEmail        = null;
    let remainingAttempts = 3;
    let cooldownTimer   = null;
    let expiryTimer     = null;
    let expirySeconds   = 600;

    window.initMfaStep = function (emailHint, email) {
        mfaEmail          = email;
        remainingAttempts = 3;

        document.getElementById("mfa-attempts-info").textContent =
            "Você tem 3 tentativas para inserir o código.";
        document.getElementById("resend-status").textContent = "";
        document.getElementById("mfa-expiry-info").textContent = "";
        document.getElementById("btn-resend-mfa").disabled = false;

        startExpiryCountdown();
    };

    window.onMfaFailure = function (message, remaining) {
        remainingAttempts = (remaining !== undefined) ? remaining : remainingAttempts - 1;

        const attemptsEl = document.getElementById("mfa-attempts-info");
        if (remainingAttempts <= 0) {
            attemptsEl.innerHTML =
                '<span style="color:var(--error);font-weight:600">⛔ Tentativas esgotadas.</span> Use o botão "Reenviar código" abaixo.';
            document.getElementById("mfa-code").disabled = true;
            btnVerify.disabled = true;
        } else {
            attemptsEl.innerHTML =
                `<span style="color:var(--warning)">⚠️ Código incorreto. Tentativas restantes: <strong>${remainingAttempts}</strong></span>`;
        }
    };

    document.getElementById("btn-resend-mfa").addEventListener("click", async () => {
        if (!mfaEmail) return;

        const btn    = document.getElementById("btn-resend-mfa");
        const status = document.getElementById("resend-status");
        btn.disabled = true;

        try {
            const res = await fetch(`${API}/auth/resend-mfa`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: mfaEmail })
            });

            const data = await res.json().catch(() => ({}));

            if (!res.ok) {
                status.style.color = "var(--error)";
                status.textContent = data.message || "Não foi possível reenviar.";

                if (res.status !== 429 || data.message?.includes("Faça login")) {

                } else {
                    const secs = data.message?.match(/\d+/)?.[0] || 60;
                    startCooldown(parseInt(secs));
                }
                return;
            }

            remainingAttempts = 3;
            const attemptsEl = document.getElementById("mfa-attempts-info");
            attemptsEl.innerHTML = "Você tem 3 tentativas para inserir o novo código.";
            document.getElementById("mfa-code").disabled = false;
            btnVerify.disabled = false;
            document.getElementById("mfa-code").value = "";
            document.getElementById("mfa-code").focus();

            status.style.color = "var(--success)";
            const left = data.remainingResends ?? "?";
            status.textContent = `Código reenviado para ${data.emailHint}. Reenvios restantes: ${left}`;

            expirySeconds = 600;
            startExpiryCountdown();
            startCooldown(data.cooldownSeconds || 60);

        } catch {
            status.style.color = "var(--error)";
            status.textContent = "Erro de conexão.";
            btn.disabled = false;
        }
    });

    function startCooldown(seconds) {
        if (cooldownTimer) clearInterval(cooldownTimer);
        const btn    = document.getElementById("btn-resend-mfa");
        const status = document.getElementById("resend-status");
        let s = seconds;

        btn.disabled = true;
        btn.textContent = `↺ Aguarde ${s}s`;

        cooldownTimer = setInterval(() => {
            s--;
            if (s <= 0) {
                clearInterval(cooldownTimer);
                btn.disabled = false;
                btn.textContent = "↺ Reenviar código";
                status.textContent = "";
            } else {
                btn.textContent = `↺ Aguarde ${s}s`;
            }
        }, 1000);
    }

    function startExpiryCountdown() {
        if (expiryTimer) clearInterval(expiryTimer);
        expirySeconds = 600;
        const el = document.getElementById("mfa-expiry-info");

        expiryTimer = setInterval(() => {
            expirySeconds--;
            const m = Math.floor(expirySeconds / 60);
            const s = String(expirySeconds % 60).padStart(2, "0");
            el.textContent = `Código expira em ${m}:${s}`;
            if (expirySeconds <= 0) {
                clearInterval(expiryTimer);
                el.style.color = "var(--error)";
                el.textContent = "Código expirado. Reenvie ou faça login novamente.";
                document.getElementById("mfa-code").disabled = true;
                btnVerify.disabled = true;
            } else if (expirySeconds <= 60) {
                el.style.color = "var(--error)";
            } else if (expirySeconds <= 180) {
                el.style.color = "var(--warning)";
            }
        }, 1000);
    }

})();
