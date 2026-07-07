#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${THREADIA_E2E_BASE_URL:-http://localhost:8080}"
REQUIRE_OAUTH="${THREADIA_E2E_REQUIRE_OAUTH:-false}"
MAILHOG_API_URL="${THREADIA_E2E_MAILHOG_API_URL:-}"
CURL_CONNECT_TIMEOUT="${THREADIA_E2E_CONNECT_TIMEOUT:-3}"
CURL_MAX_TIME="${THREADIA_E2E_MAX_TIME:-10}"
WORK_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'FAIL missing required command: %s\n' "$1" >&2
    exit 1
  fi
}

curl_status() {
  curl -sS --connect-timeout "$CURL_CONNECT_TIMEOUT" --max-time "$CURL_MAX_TIME" -w '%{http_code}' "$@" || printf '000'
}

assert_status() {
  local label="$1"
  local actual="$2"
  local expected="$3"

  if [[ "$actual" != "$expected" ]]; then
    printf 'FAIL %-34s expected=%s actual=%s\n' "$label" "$expected" "$actual" >&2
    exit 1
  fi

  printf 'PASS %-34s status=%s\n' "$label" "$actual"
}

json_value() {
  local file="$1"
  local expression="$2"

  python3 - "$file" "$expression" <<'PY'
import json
import sys

path, expression = sys.argv[1], sys.argv[2]
with open(path, encoding="utf-8") as fp:
    data = json.load(fp)

value = data
for key in expression.split("."):
    value = value[key]
print(value)
PY
}

json_contains_key() {
  local file="$1"
  local key="$2"

  python3 - "$file" "$key" <<'PY'
import json
import sys

path, target = sys.argv[1], sys.argv[2]
with open(path, encoding="utf-8") as fp:
    data = json.load(fp)

def contains(value):
    if isinstance(value, dict):
        return target in value or any(contains(v) for v in value.values())
    if isinstance(value, list):
        return any(contains(v) for v in value)
    return False

print("true" if contains(data) else "false")
PY
}

cookie_value() {
  local jar="$1"
  local name="$2"

  python3 - "$jar" "$name" <<'PY'
import sys

path, name = sys.argv[1], sys.argv[2]
try:
    with open(path, encoding="utf-8") as fp:
        lines = fp.read().splitlines()
except FileNotFoundError:
    print("")
    sys.exit(0)

for line in lines:
    if not line or (line.startswith("#") and not line.startswith("#HttpOnly_")):
        continue
    columns = line.split("\t")
    if len(columns) >= 7 and columns[5] == name:
        print(columns[6])
        sys.exit(0)

print("")
PY
}

oauth_redirect_uri_path() {
  local headers_file="$1"

  python3 - "$headers_file" <<'PY'
import sys
from urllib.parse import parse_qs, unquote, urlparse

path = sys.argv[1]
location = ""
with open(path, encoding="utf-8") as fp:
    for line in fp:
        if line.lower().startswith("location:"):
            location = line.split(":", 1)[1].strip()
            break

if not location:
    print("")
    sys.exit(0)

redirect_values = parse_qs(urlparse(location).query).get("redirect_uri", [])
if not redirect_values:
    print("")
    sys.exit(0)

print(urlparse(unquote(redirect_values[0])).path)
PY
}

mailhog_reset_token() {
  local api_url="$1"
  local target_email="$2"

  python3 - "$api_url" "$target_email" <<'PY'
import json
import quopri
import re
import sys
import time
from urllib.parse import unquote, urlparse, parse_qs
from urllib.request import urlopen

api_url, target_email = sys.argv[1].rstrip("/"), sys.argv[2]
token_pattern = re.compile(r"reset-password\?token=([A-Za-z0-9_-]+)")

def strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for nested in value.values():
            yield from strings(nested)
    elif isinstance(value, list):
        for nested in value:
            yield from strings(nested)

for _ in range(20):
    try:
        with urlopen(f"{api_url}/api/v2/messages", timeout=3) as response:
            messages = json.load(response).get("items", [])
    except Exception:
        messages = []

    for message in messages:
        serialized = json.dumps(message, ensure_ascii=False)
        if target_email not in serialized:
            continue
        for value in strings(message):
            decoded_values = []
            try:
                decoded_values.append(quopri.decodestring(value.encode()).decode())
            except Exception:
                pass
            decoded_values.append(value)
            for decoded_value in decoded_values:
                match = token_pattern.search(decoded_value)
                if not match:
                    continue
                token = parse_qs(urlparse("http://localhost/reset-password?token=" + match.group(1)).query).get("token", [])
                if token:
                    print(unquote(token[0]))
                    sys.exit(0)
    time.sleep(1)

print("")
PY
}

assert_json_key_absent() {
  local label="$1"
  local file="$2"
  local key="$3"
  local found

  found="$(json_contains_key "$file" "$key")"
  if [[ "$found" == "true" ]]; then
    printf 'FAIL %-34s key_present=%s\n' "$label" "$key" >&2
    exit 1
  fi

  printf 'PASS %-34s key_absent=%s\n' "$label" "$key"
}

assert_non_empty() {
  local label="$1"
  local value="$2"

  if [[ -z "$value" ]]; then
    printf 'FAIL %-34s value is empty\n' "$label" >&2
    exit 1
  fi

  printf 'PASS %-34s present\n' "$label"
}

require_command curl
require_command python3

printf 'Auth E2E backend preflight\n'
printf 'BASE_URL=%s\n' "$BASE_URL"

health_status="$(curl_status -o "$WORK_DIR/health.json" "$BASE_URL/actuator/health")"
assert_status "actuator health" "$health_status" "200"

stamp="$(date +%Y%m%d%H%M%S)"
email="auth.e2e.${stamp}@example.com"
signup_email=" $(printf '%s' "$email" | tr '[:lower:]' '[:upper:]') "
password="Password1234"

signup_status="$(curl_status -o "$WORK_DIR/signup.json" \
  -X POST "$BASE_URL/api/v1/auth/signup" \
  -H 'Content-Type: application/json' \
  --data "{\"email\":\"$signup_email\",\"password\":\"$password\",\"nickname\":\"Auth E2E\"}")"
assert_status "signup" "$signup_status" "201"

signup_email_response="$(json_value "$WORK_DIR/signup.json" 'data.user.email')"
if [[ "$signup_email_response" != "$email" ]]; then
  printf 'FAIL %-34s expected=%s actual=%s\n' "signup email normalized" "$email" "$signup_email_response" >&2
  exit 1
fi
printf 'PASS %-34s email_normalized\n' "signup email normalized"

duplicate_status="$(curl_status -o "$WORK_DIR/duplicate-signup.json" \
  -X POST "$BASE_URL/api/v1/auth/signup" \
  -H 'Content-Type: application/json' \
  --data "{\"email\":\"$email\",\"password\":\"$password\",\"nickname\":\"Auth E2E\"}")"
assert_status "duplicate signup" "$duplicate_status" "409"

login_status="$(curl_status -o "$WORK_DIR/login.json" -D "$WORK_DIR/login.headers" -c "$WORK_DIR/login-cookie.jar" \
  -X POST "$BASE_URL/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  --data "{\"email\":\"$email\",\"password\":\"$password\"}")"
assert_status "login" "$login_status" "200"
assert_json_key_absent "login body" "$WORK_DIR/login.json" "refreshToken"

login_access_token="$(json_value "$WORK_DIR/login.json" 'data.accessToken')"
assert_non_empty "login access token" "$login_access_token"

login_cookie="$(cookie_value "$WORK_DIR/login-cookie.jar" refreshToken)"
assert_non_empty "login refresh cookie" "$login_cookie"

bad_login_status="$(curl_status -o "$WORK_DIR/bad-login.json" \
  -X POST "$BASE_URL/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  --data "{\"email\":\"$email\",\"password\":\"WrongPassword1234\"}")"
assert_status "bad login" "$bad_login_status" "401"

refresh_status="$(curl_status -o "$WORK_DIR/refresh.json" -D "$WORK_DIR/refresh.headers" -b "$WORK_DIR/login-cookie.jar" -c "$WORK_DIR/refresh-cookie.jar" \
  -X POST "$BASE_URL/api/v1/auth/refresh")"
assert_status "refresh" "$refresh_status" "200"
assert_json_key_absent "refresh body" "$WORK_DIR/refresh.json" "refreshToken"

refresh_access_token="$(json_value "$WORK_DIR/refresh.json" 'data.accessToken')"
assert_non_empty "refresh access token" "$refresh_access_token"

refresh_cookie="$(cookie_value "$WORK_DIR/refresh-cookie.jar" refreshToken)"
assert_non_empty "refresh rotated cookie" "$refresh_cookie"

if [[ "$login_cookie" == "$refresh_cookie" ]]; then
  printf 'FAIL %-34s cookie was not rotated\n' "refresh rotation" >&2
  exit 1
fi
printf 'PASS %-34s rotated\n' "refresh rotation"

old_refresh_reuse_status="$(curl_status -o "$WORK_DIR/old-refresh-reuse.json" -b "$WORK_DIR/login-cookie.jar" \
  -X POST "$BASE_URL/api/v1/auth/refresh")"
assert_status "old refresh reuse" "$old_refresh_reuse_status" "401"

me_status="$(curl_status -o "$WORK_DIR/me.json" \
  -X GET "$BASE_URL/api/v1/auth/me" \
  -H "Authorization: Bearer $refresh_access_token")"
assert_status "auth me" "$me_status" "200"

me_email="$(json_value "$WORK_DIR/me.json" 'data.user.email')"
if [[ "$me_email" != "$email" ]]; then
  printf 'FAIL %-34s expected=%s actual=%s\n' "auth me email" "$email" "$me_email" >&2
  exit 1
fi
printf 'PASS %-34s matches_login_user\n' "auth me email"

missing_me_status="$(curl_status -o "$WORK_DIR/me-missing-token.json" \
  -X GET "$BASE_URL/api/v1/auth/me")"
assert_status "auth me missing token" "$missing_me_status" "401"

logout_status="$(curl_status -o "$WORK_DIR/logout.json" -D "$WORK_DIR/logout.headers" -b "$WORK_DIR/refresh-cookie.jar" \
  -X POST "$BASE_URL/api/v1/auth/logout" \
  -H "Authorization: Bearer $refresh_access_token")"
assert_status "logout" "$logout_status" "200"

if ! grep -qi 'Set-Cookie: refreshToken=.*Max-Age=0' "$WORK_DIR/logout.headers"; then
  printf 'FAIL %-34s missing clear cookie header\n' "logout clear cookie" >&2
  exit 1
fi
printf 'PASS %-34s Max-Age=0\n' "logout clear cookie"

revoked_refresh_status="$(curl_status -o "$WORK_DIR/revoked-refresh.json" -b "$WORK_DIR/refresh-cookie.jar" \
  -X POST "$BASE_URL/api/v1/auth/refresh")"
assert_status "revoked refresh after logout" "$revoked_refresh_status" "401"

oauth_status="$(curl_status -o "$WORK_DIR/oauth.html" -D "$WORK_DIR/oauth.headers" \
  "$BASE_URL/oauth2/authorization/google")"
if [[ "$oauth_status" == "302" ]] && grep -qi '^Location: https://accounts.google.com/' "$WORK_DIR/oauth.headers"; then
  printf 'PASS %-34s google_redirect_302\n' "oauth start"
  oauth_callback_path="$(oauth_redirect_uri_path "$WORK_DIR/oauth.headers")"
  if [[ "$oauth_callback_path" != "/api/v1/auth/oauth2/callback/google" ]]; then
    printf 'FAIL %-34s callback redirect_uri mismatch\n' "oauth callback readiness" >&2
    exit 1
  fi
  printf 'PASS %-34s redirect_uri_configured\n' "oauth callback readiness"
  printf 'BLOCKED %-31s actual_google_login_required\n' "oauth callback"
elif [[ "$REQUIRE_OAUTH" == "true" ]]; then
  printf 'FAIL %-34s expected Google redirect, status=%s\n' "oauth start" "$oauth_status" >&2
  exit 1
else
  printf 'BLOCKED %-31s status=%s; set GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET to verify\n' "oauth start" "$oauth_status"
  printf 'BLOCKED %-31s set GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET and matching redirect URI\n' "oauth callback"
fi

password_reset_status="$(curl_status -o "$WORK_DIR/password-reset.json" \
  -X POST "$BASE_URL/api/v1/auth/password-reset/request" \
  -H 'Content-Type: application/json' \
  --data "{\"email\":\"$email\"}")"
assert_status "password reset request" "$password_reset_status" "200"

password_reset_missing_user_status="$(curl_status -o "$WORK_DIR/password-reset-missing-user.json" \
  -X POST "$BASE_URL/api/v1/auth/password-reset/request" \
  -H 'Content-Type: application/json' \
  --data "{\"email\":\"missing.$email\"}")"
assert_status "password reset missing user" "$password_reset_missing_user_status" "200"

password_reset_bad_token_status="$(curl_status -o "$WORK_DIR/password-reset-bad-token.json" \
  -X POST "$BASE_URL/api/v1/auth/password-reset/confirm" \
  -H 'Content-Type: application/json' \
  --data '{"token":"bad-token","newPassword":"NewPassword1234"}')"
assert_status "password reset bad token" "$password_reset_bad_token_status" "401"

password_reset_weak_password_status="$(curl_status -o "$WORK_DIR/password-reset-weak-password.json" \
  -X POST "$BASE_URL/api/v1/auth/password-reset/confirm" \
  -H 'Content-Type: application/json' \
  --data '{"token":"reset-token","newPassword":"short"}')"
assert_status "password reset weak password" "$password_reset_weak_password_status" "400"

if [[ -n "$MAILHOG_API_URL" ]]; then
  reset_token="$(mailhog_reset_token "$MAILHOG_API_URL" "$email")"
  assert_non_empty "password reset raw token" "$reset_token"

  new_password="NewPassword1234"
  password_reset_confirm_status="$(curl_status -o "$WORK_DIR/password-reset-confirm.json" \
    -X POST "$BASE_URL/api/v1/auth/password-reset/confirm" \
    -H 'Content-Type: application/json' \
    --data "{\"token\":\"$reset_token\",\"newPassword\":\"$new_password\"}")"
  assert_status "password reset confirm" "$password_reset_confirm_status" "200"

  password_reset_reuse_status="$(curl_status -o "$WORK_DIR/password-reset-reuse.json" \
    -X POST "$BASE_URL/api/v1/auth/password-reset/confirm" \
    -H 'Content-Type: application/json' \
    --data "{\"token\":\"$reset_token\",\"newPassword\":\"$password\"}")"
  assert_status "password reset token reuse" "$password_reset_reuse_status" "401"

  old_password_login_status="$(curl_status -o "$WORK_DIR/old-password-login.json" \
    -X POST "$BASE_URL/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    --data "{\"email\":\"$email\",\"password\":\"$password\"}")"
  assert_status "old password after reset" "$old_password_login_status" "401"

  new_password_login_status="$(curl_status -o "$WORK_DIR/new-password-login.json" \
    -X POST "$BASE_URL/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    --data "{\"email\":\"$email\",\"password\":\"$new_password\"}")"
  assert_status "new password after reset" "$new_password_login_status" "200"
else
  printf 'BLOCKED %-31s raw_reset_token_required_from_email\n' "password reset confirm"
fi

printf 'PASS backend auth e2e preflight completed without printing tokens or cookie values\n'
