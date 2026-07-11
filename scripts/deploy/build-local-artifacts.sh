#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/deploy/build-local-artifacts.sh <backend|frontend|docs-site|all>

Runs Threadia local build commands for the selected group.
USAGE
}

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
group="${1:-}"

if [[ -z "${group}" || "${group}" == "-h" || "${group}" == "--help" ]]; then
  usage
  exit 0
fi

build_backend() {
  printf '\n== backend build ==\n'
  (cd "${repo_root}/backend" && ./gradlew build)
}

build_frontend() {
  printf '\n== frontend lint/build ==\n'
  (cd "${repo_root}/frontend" && npm ci && npm run lint && npm run build)
}

build_docs_site() {
  printf '\n== docs-site lint/build ==\n'
  (cd "${repo_root}/docs-site" && npm ci && npm run lint && npm run build)
}

case "${group}" in
  backend)
    build_backend
    ;;
  frontend)
    build_frontend
    ;;
  docs-site)
    build_docs_site
    ;;
  all)
    build_backend
    build_frontend
    build_docs_site
    ;;
  *)
    usage
    exit 2
    ;;
esac
