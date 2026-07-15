#!/usr/bin/env bash
set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly HELPER_SOURCE="$SCRIPT_DIR/apply-release"
readonly UNIT_SOURCE="$SCRIPT_DIR/threadia-backend.service"

fail() {
  printf '%s\n' "bootstrap failed: $*" >&2
  exit 1
}

[[ "$(id -u)" -eq 0 ]] || fail "must run as root"
getent passwd threadia >/dev/null || fail "deploy user threadia is missing"
for command in aws python3 tar curl systemctl install flock; do
  command -v "$command" >/dev/null 2>&1 || fail "baseline prerequisite missing: $command"
done
[[ -f "$HELPER_SOURCE" && -f "$UNIT_SOURCE" ]] || fail "reviewed host artifacts are missing"

install -d -o root -g root -m 0755 /usr/local/lib/threadia-release
install -o root -g root -m 0750 "$HELPER_SOURCE" /usr/local/lib/threadia-release/apply-release
install -o root -g root -m 0644 "$UNIT_SOURCE" /etc/systemd/system/threadia-backend.service
install -d -o root -g threadia -m 0750 /opt/threadia/releases
install -d -o root -g root -m 0750 /etc/threadia
systemctl daemon-reload
systemctl enable threadia-backend.service
printf '%s\n' 'bootstrap complete; no network package installation was performed'
