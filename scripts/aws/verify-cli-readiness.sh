#!/usr/bin/env bash
set -eu

if ! command -v aws >/dev/null 2>&1; then
  printf '[fail] AWS CLI is not available\n'
  exit 1
fi

if ! aws --version 2>&1 | grep -q '^aws-cli/2'; then
  printf '[fail] AWS CLI v2 is required\n'
  exit 1
fi

printf '[ok] AWS CLI v2 is available\n'

if aws sts get-caller-identity --no-cli-pager --output text --query '[Account, Arn, UserId]' 2>/dev/null |
  awk -F '\t' '
    NF == 3 && length($1) > 0 && length($2) > 0 && length($3) > 0 {
      found = 1
      exit 0
    }
    END {
      exit(found ? 0 : 1)
    }
  ';
then
  printf '[ok] AWS STS caller identity response is valid\n'
  exit 0
fi

printf '[fail] AWS STS caller identity response is invalid\n'
exit 1
