#!/usr/bin/env bash
#
# Roda os testes unitários da lógica de negócio (base da pirâmide).
# São puros (JUnit + Mockito) — não precisam de app, banco nem navegador.
#
# Uso:
#   ./run-unit.sh                  # roda todos os *ServiceTest
#   ./run-unit.sh MarketplaceServiceTest
#
set -euo pipefail

cd "$(dirname "$0")/.."

IMAGE="${IMAGE:-clinica-e2e:latest}"
TEST_PATTERN="${1:-*ServiceTest}"

# Reaproveita a imagem de teste (tem JDK 26 + Maven). Constrói se faltar.
if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then
    echo "==> Imagem $IMAGE não encontrada. Construindo a partir do Dockerfile.e2e..."
    docker build -f Dockerfile.e2e -t "$IMAGE" .
fi

echo "==> Testes unitários (pattern: $TEST_PATTERN)"

docker run --rm \
    -v "$PWD":/workspace -w /workspace \
    -v clinica-m2:/root/.m2 \
    "$IMAGE" \
    mvn -B test -Dtest="$TEST_PATTERN" -Dsurefire.failIfNoSpecifiedTests=false
