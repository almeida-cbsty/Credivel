#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v java >/dev/null || { echo 'Instale Java 21.' >&2; exit 1; }
command -v npm >/dev/null || { echo 'Instale Node.js 22.12+ com npm.' >&2; exit 1; }
(cd frontend && npm ci && npm run build)
(cd backend && ./mvnw -B package)
mkdir -p data
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"
echo "Credível: http://localhost:${SERVER_PORT:-8083}"
exec java -jar backend/target/credivel-api-1.0.0.jar "$@"
