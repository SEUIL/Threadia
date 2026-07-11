#!/usr/bin/env bash
set -euo pipefail

# Threadia backend EC2 deployment skeleton.
# This script is intentionally dry-run by default.
# Actual deployment requires prior team approval and THREADIA_DEPLOY_EXECUTE=true.
# It never prints secret values.

required_vars=(
  THREADIA_DEPLOY_TARGET_HOST
  THREADIA_DEPLOY_TARGET_USER
  THREADIA_DEPLOY_SSH_KEY_PATH
  THREADIA_DEPLOY_REMOTE_DIR
  THREADIA_DEPLOY_ARTIFACT
  THREADIA_DEPLOY_RESTART_COMMAND
)

missing=0
for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    printf '[missing] %s is not set\n' "${var_name}"
    missing=1
  else
    printf '[ok] %s is set\n' "${var_name}"
  fi
done

if [[ "${missing}" -ne 0 ]]; then
  printf '\nDeployment skeleton stopped before any remote action because required variables are missing.\n'
  exit 1
fi

if [[ ! -f "${THREADIA_DEPLOY_ARTIFACT}" ]]; then
  printf '[missing] THREADIA_DEPLOY_ARTIFACT path does not exist\n'
  exit 1
fi

if [[ ! -f "${THREADIA_DEPLOY_SSH_KEY_PATH}" ]]; then
  printf '[missing] THREADIA_DEPLOY_SSH_KEY_PATH path does not exist\n'
  exit 1
fi

printf '\n== dry-run summary ==\n'
printf 'Target host variable is set: THREADIA_DEPLOY_TARGET_HOST\n'
printf 'Target user variable is set: THREADIA_DEPLOY_TARGET_USER\n'
printf 'Remote directory variable is set: THREADIA_DEPLOY_REMOTE_DIR\n'
printf 'Artifact variable is set: THREADIA_DEPLOY_ARTIFACT\n'
printf 'Restart command variable is set: THREADIA_DEPLOY_RESTART_COMMAND\n'

if [[ "${THREADIA_DEPLOY_EXECUTE:-false}" != "true" ]]; then
  printf '\nDry-run only. Set THREADIA_DEPLOY_EXECUTE=true after explicit approval to run remote copy/restart.\n'
  exit 0
fi

printf '\nTHREADIA_DEPLOY_EXECUTE=true detected. Running approved deployment skeleton.\n'

ssh_target="${THREADIA_DEPLOY_TARGET_USER}@${THREADIA_DEPLOY_TARGET_HOST}"

ssh -i "${THREADIA_DEPLOY_SSH_KEY_PATH}" "${ssh_target}" "mkdir -p '${THREADIA_DEPLOY_REMOTE_DIR}'"
scp -i "${THREADIA_DEPLOY_SSH_KEY_PATH}" "${THREADIA_DEPLOY_ARTIFACT}" "${ssh_target}:${THREADIA_DEPLOY_REMOTE_DIR}/"
ssh -i "${THREADIA_DEPLOY_SSH_KEY_PATH}" "${ssh_target}" "${THREADIA_DEPLOY_RESTART_COMMAND}"

printf 'Deployment skeleton completed. Verify backend health endpoint and application logs separately.\n'
