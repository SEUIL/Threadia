#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/deploy/check-env.sh <backend|frontend|aws|deploy|all>

Checks whether required Threadia environment variable names are present.
This script never prints variable values.
USAGE
}

status=0
group="${1:-}"

if [[ -z "${group}" || "${group}" == "-h" || "${group}" == "--help" ]]; then
  usage
  exit 0
fi

check_var() {
  local name="$1"
  if [[ -n "${!name:-}" ]]; then
    printf '[ok] %s is set\n' "${name}"
  else
    printf '[missing] %s is not set\n' "${name}"
    status=1
  fi
}

check_group() {
  local title="$1"
  shift
  printf '\n== %s ==\n' "${title}"
  local var_name
  for var_name in "$@"; do
    check_var "${var_name}"
  done
}

backend_vars=(
  SPRING_PROFILES_ACTIVE
  THREADIA_DB_URL
  THREADIA_DB_USERNAME
  THREADIA_DB_PASSWORD
  THREADIA_JWT_SECRET
  THREADIA_COOKIE_SECURE
  THREADIA_CORS_ALLOWED_ORIGIN
  THREADIA_OAUTH_SUCCESS_REDIRECT_URL
  THREADIA_OAUTH_FAILURE_REDIRECT_URL
  GOOGLE_CLIENT_ID
  GOOGLE_CLIENT_SECRET
  GOOGLE_REDIRECT_URI
  MAIL_HOST
  MAIL_PORT
  MAIL_USERNAME
  MAIL_PASSWORD
  NOTIFICATION_FROM_EMAIL
  THREADIA_PASSWORD_RESET_URL_PREFIX
  THREADIA_PASSWORD_RESET_EXPIRATION_MINUTES
)

frontend_vars=(
  NEXT_PUBLIC_API_URL
)

aws_vars=(
  AWS_REGION
  AWS_ROLE_TO_ASSUME
  THREADIA_DOMAIN_NAME
  THREADIA_APP_DOMAIN
  THREADIA_API_DOMAIN
  THREADIA_ROUTE53_HOSTED_ZONE_ID
  THREADIA_ACM_CERTIFICATE_ARN
)

deploy_vars=(
  THREADIA_DEPLOY_TARGET_HOST
  THREADIA_DEPLOY_TARGET_USER
  THREADIA_DEPLOY_SSH_KEY_PATH
  THREADIA_DEPLOY_REMOTE_DIR
  THREADIA_DEPLOY_ARTIFACT
  THREADIA_DEPLOY_RESTART_COMMAND
  THREADIA_DEPLOY_EXECUTE
)

case "${group}" in
  backend)
    check_group "backend" "${backend_vars[@]}"
    ;;
  frontend)
    check_group "frontend" "${frontend_vars[@]}"
    ;;
  aws)
    check_group "aws" "${aws_vars[@]}"
    ;;
  deploy)
    check_group "deploy" "${deploy_vars[@]}"
    ;;
  all)
    check_group "backend" "${backend_vars[@]}"
    check_group "frontend" "${frontend_vars[@]}"
    check_group "aws" "${aws_vars[@]}"
    check_group "deploy" "${deploy_vars[@]}"
    ;;
  *)
    usage
    exit 2
    ;;
esac

exit "${status}"
