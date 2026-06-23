#!/usr/bin/env bash
#
# Roda os testes E2E (Playwright) dentro do container clinica-e2e,
# apontando para o app que já está rodando.
#
# Uso:
#   ./run-e2e.sh                 # roda todos os testes *E2ETest
#   ./run-e2e.sh PatientLoginE2ETest        # uma classe específica
#   ./run-e2e.sh 'PatientLoginE2ETest#portalDoPacienteCarregaComFormularioDeLogin'  # um método
#
# Usa --network host (Linux) para que:
#   - o container alcance o app em http://localhost:8080
#   - a origem seja "localhost" = contexto seguro, exigido pelo crypto.subtle
#     do captcha PoW (não funciona em origem HTTP não-localhost).
#
# Variáveis de ambiente:
#   BASE_URL   URL do app (default: http://localhost:8080)
#   IMAGE      Imagem de teste (default: clinica-e2e:latest)
#
set -euo pipefail

cd "$(dirname "$0")"

IMAGE="${IMAGE:-clinica-e2e:latest}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
TEST_PATTERN="${1:-*E2ETest}"

# Constrói a imagem de teste se ainda não existir
if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then
    echo "==> Imagem $IMAGE não encontrada. Construindo a partir do Dockerfile.e2e..."
    docker build -f Dockerfile.e2e -t "$IMAGE" .
fi

echo "==> Rodando testes E2E (pattern: $TEST_PATTERN) contra $BASE_URL"

docker run --rm \
    --network host \
    -v "$PWD":/workspace -w /workspace \
    -v clinica-m2:/root/.m2 \
    -e BASE_URL="$BASE_URL" \
    "$IMAGE" \
    mvn -B test -Dtest="$TEST_PATTERN" -Dsurefire.failIfNoSpecifiedTests=false
