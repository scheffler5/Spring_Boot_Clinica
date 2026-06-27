#!/usr/bin/env bash
#
# Gera o relatório de cobertura (JaCoCo) dos testes unitários.
#
# O JaCoCo ainda não suporta o bytecode do Java 26, então a cobertura é
# gerada compilando em Java 21 (profile -Pcoverage), numa imagem com JDK 21.
# Isso NÃO altera o build de produção (que continua em Java 26).
#
# Saída: target/site/jacoco/index.html
#
set -euo pipefail

cd "$(dirname "$0")/.."

# Imagem com JDK 21 + Maven (a oficial do Playwright já tem ambos).
JDK21_IMAGE="${JDK21_IMAGE:-mcr.microsoft.com/playwright/java:v1.49.0-noble}"
TEST_PATTERN="${1:-*ServiceTest}"

echo "==> Gerando cobertura (profile coverage, JDK 21) — pattern: $TEST_PATTERN"

docker run --rm \
    -v "$PWD":/workspace -w /workspace \
    -v clinica-m2:/root/.m2 \
    "$JDK21_IMAGE" \
    mvn -B -Pcoverage test -Dtest="$TEST_PATTERN" -Dsurefire.failIfNoSpecifiedTests=false

echo ""
echo "==> Relatório HTML: target/site/jacoco/index.html"

# Resumo por pacote de serviço (instruções cobertas) a partir do CSV.
CSV="target/site/jacoco/jacoco.csv"
if [ -f "$CSV" ]; then
    echo "==> Cobertura das classes de serviço (instruções):"
    awk -F, 'NR>1 && $2 ~ /service/ {
        miss[$3]+=$4; cov[$3]+=$5
    }
    END {
        for (c in cov) {
            total = miss[c] + cov[c];
            if (total > 0) printf "    %-32s %5.1f%%\n", c, (cov[c]*100.0/total);
        }
    }' "$CSV" | sort
fi
