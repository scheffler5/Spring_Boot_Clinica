#!/usr/bin/env python3
"""
Seed de médicos para demonstração.
Uso: python3 seed_medicos.py [--base-url http://localhost:8080]
"""

import hashlib
import sys
import argparse
from pathlib import Path
import requests

IMAGENS_DIR = Path(__file__).parent / "Imagens"
SENHA_PADRAO = "Demo@1234"

MEDICOS = [
    {
        "login": "dr.rafael.costa",
        "imagem": "médico1.jpeg",
        "content_type": "image/jpeg",
        "perfil": {
            "nome": "Dr. Rafael Costa",
            "crm": "SP-12345",
            "especialidade": "CARDIOLOGIA",
            "cidade": "São Paulo",
            "valorConsulta": 350.00,
            "duracaoConsultaMinutos": 45,
            "descricao": "Cardiologista com mais de 15 anos de experiência em cardiologia intervencionista e preventiva. Especialista em arritmias e insuficiência cardíaca.",
            "universidade": "Universidade de São Paulo (USP)",
            "anoFormacao": 2008,
        },
        "disponibilidade": [
            {"diaSemana": "MONDAY",    "horaInicio": "08:00", "horaFim": "12:00"},
            {"diaSemana": "WEDNESDAY", "horaInicio": "08:00", "horaFim": "12:00"},
            {"diaSemana": "FRIDAY",    "horaInicio": "14:00", "horaFim": "18:00"},
        ],
    },
    {
        "login": "dr.bruno.almeida",
        "imagem": "médico2.jpeg",
        "content_type": "image/jpeg",
        "perfil": {
            "nome": "Dr. Bruno Almeida",
            "crm": "RJ-54321",
            "especialidade": "ORTOPEDIA",
            "cidade": "Rio de Janeiro",
            "valorConsulta": 280.00,
            "duracaoConsultaMinutos": 30,
            "descricao": "Ortopedista especializado em cirurgia do joelho e quadril, com foco em medicina esportiva. Atende atletas profissionais e amadores.",
            "universidade": "Universidade Federal do Rio de Janeiro (UFRJ)",
            "anoFormacao": 2011,
        },
        "disponibilidade": [
            {"diaSemana": "TUESDAY",   "horaInicio": "09:00", "horaFim": "13:00"},
            {"diaSemana": "THURSDAY",  "horaInicio": "09:00", "horaFim": "13:00"},
            {"diaSemana": "SATURDAY",  "horaInicio": "08:00", "horaFim": "12:00"},
        ],
    },
    {
        "login": "dra.ana.lima",
        "imagem": "médico3.jpeg",
        "content_type": "image/jpeg",
        "perfil": {
            "nome": "Dra. Ana Lima",
            "crm": "MG-98765",
            "especialidade": "DERMATOLOGIA",
            "cidade": "Belo Horizonte",
            "valorConsulta": 300.00,
            "duracaoConsultaMinutos": 30,
            "descricao": "Dermatologista com especialização em dermatologia clínica e cosmética. Referência em tratamentos de acne, psoríase e rejuvenescimento facial.",
            "universidade": "Universidade Federal de Minas Gerais (UFMG)",
            "anoFormacao": 2013,
        },
        "disponibilidade": [
            {"diaSemana": "MONDAY",    "horaInicio": "14:00", "horaFim": "18:00"},
            {"diaSemana": "WEDNESDAY", "horaInicio": "14:00", "horaFim": "18:00"},
            {"diaSemana": "FRIDAY",    "horaInicio": "08:00", "horaFim": "12:00"},
        ],
    },
    {
        "login": "dra.fernanda.santos",
        "imagem": "médico4.jpg",
        "content_type": "image/jpeg",
        "perfil": {
            "nome": "Dra. Fernanda Santos",
            "crm": "RS-11223",
            "especialidade": "PSIQUIATRIA",
            "cidade": "Porto Alegre",
            "valorConsulta": 400.00,
            "duracaoConsultaMinutos": 60,
            "descricao": "Psiquiatra com foco em transtornos de ansiedade, depressão e TDAH em adultos. Abordagem integrativa combinando psicofarmacologia e psicoterapia.",
            "universidade": "Universidade Federal do Rio Grande do Sul (UFRGS)",
            "anoFormacao": 2010,
        },
        "disponibilidade": [
            {"diaSemana": "TUESDAY",   "horaInicio": "10:00", "horaFim": "17:00"},
            {"diaSemana": "THURSDAY",  "horaInicio": "10:00", "horaFim": "17:00"},
        ],
    },
]


def solve_pow(challenge: str, difficulty: int) -> str:
    prefix = "0" * difficulty
    nonce = 0
    while True:
        digest = hashlib.sha256(f"{challenge}:{nonce}".encode()).hexdigest()
        if digest.startswith(prefix):
            return str(nonce)
        nonce += 1


def step(label: str, ok: bool, detail: str = "") -> bool:
    mark = "✓" if ok else "✗"
    suffix = f"  ({detail})" if detail else ""
    print(f"  {mark} {label}{suffix}")
    return ok


def seed_medico(medico: dict, base_url: str) -> bool:
    print(f"\n── {medico['perfil']['nome']} ──────────────────────────")

    # 1. Gerar e resolver CAPTCHA
    try:
        r = requests.get(f"{base_url}/captcha/generate", timeout=10)
        r.raise_for_status()
        captcha = r.json()
    except Exception as e:
        step("CAPTCHA generate", False, str(e))
        return False

    nonce = solve_pow(captcha["challenge"], captcha["difficulty"])
    step("CAPTCHA PoW resolvido", True, f"nonce={nonce}")

    # 2. Registrar
    r = requests.post(f"{base_url}/auth/register", json={
        "login": medico["login"],
        "password": SENHA_PADRAO,
        "captchaId": captcha["challengeId"],
        "captchaCode": nonce,
    }, timeout=10)

    if r.status_code == 201:
        step("Registro", True)
    elif r.status_code in (400, 409) and "já está em uso" in r.text:
        step("Registro", True, "login já existia, continuando")
    else:
        step("Registro", False, f"HTTP {r.status_code}: {r.text[:120]}")
        return False

    # 3. Login → JWT (também exige CAPTCHA PoW)
    try:
        rc = requests.get(f"{base_url}/captcha/generate", timeout=10)
        rc.raise_for_status()
        cap_login = rc.json()
    except Exception as e:
        step("CAPTCHA (login)", False, str(e))
        return False

    nonce_login = solve_pow(cap_login["challenge"], cap_login["difficulty"])
    step("CAPTCHA PoW (login)", True, f"nonce={nonce_login}")

    r = requests.post(f"{base_url}/auth/login", json={
        "login": medico["login"],
        "password": SENHA_PADRAO,
        "captchaId": cap_login["challengeId"],
        "captchaCode": nonce_login,
    }, timeout=10)

    if not step("Login", r.status_code == 200, f"HTTP {r.status_code}: {r.text[:80]}" if r.status_code != 200 else ""):
        return False

    auth = {"Authorization": f"Bearer {r.json()['token']}"}

    # 4. Completar perfil
    r = requests.put(f"{base_url}/medico/perfil", json=medico["perfil"], headers=auth, timeout=10)
    if r.status_code == 200:
        step("Perfil completo", True)
    elif r.status_code == 409:
        step("Perfil completo", True, "já completo")
    else:
        step("Perfil completo", False, f"HTTP {r.status_code}: {r.text[:120]}")
        return False

    # 5. Upload da foto
    img_path = IMAGENS_DIR / medico["imagem"]
    if img_path.exists():
        with open(img_path, "rb") as f:
            r = requests.post(
                f"{base_url}/medico/foto",
                files={"arquivo": (medico["imagem"], f, medico["content_type"])},
                headers=auth,
                timeout=15,
            )
        step("Foto", r.status_code == 200, img_path.name if r.status_code == 200 else f"HTTP {r.status_code}")
    else:
        step("Foto", False, f"arquivo não encontrado: {img_path}")

    # 6. Disponibilidade
    ok_slots = 0
    for slot in medico["disponibilidade"]:
        r = requests.post(f"{base_url}/medico/disponibilidade", json=slot, headers=auth, timeout=10)
        if r.status_code in (200, 201, 409):
            ok_slots += 1
    step("Disponibilidade", ok_slots > 0, f"{ok_slots}/{len(medico['disponibilidade'])} slots")

    return True


def main():
    parser = argparse.ArgumentParser(description="Seed de médicos para demonstração")
    parser.add_argument("--base-url", default="http://localhost:8080", help="URL base da API")
    args = parser.parse_args()

    base_url = args.base_url.rstrip("/")
    print(f"Seed de médicos → {base_url}")
    print(f"Senha padrão: {SENHA_PADRAO}")

    resultados = []
    for m in MEDICOS:
        ok = seed_medico(m, base_url)
        resultados.append((m["perfil"]["nome"], ok))

    print("\n══════════════════════════════════════")
    print("Resumo:")
    for nome, ok in resultados:
        print(f"  {'✓' if ok else '✗'}  {nome}")

    falhas = sum(1 for _, ok in resultados if not ok)
    if falhas == 0:
        print(f"\nTodos os {len(resultados)} médicos criados com sucesso.")
    else:
        print(f"\n{falhas} médico(s) com erro.")
        sys.exit(1)


if __name__ == "__main__":
    main()
