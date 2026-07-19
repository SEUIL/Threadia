#!/usr/bin/env python3
"""Fail-closed static preflight for the public dual-image SSM source."""

import re
from pathlib import Path


BASE = Path(__file__).parent
ROOT = BASE.parent.parent
workflow = (ROOT / ".github/workflows/ecr-compose-dual-image-ssm-deploy.yml").read_text(encoding="utf-8")
document = (BASE / "dual-image-ssm-document.yaml").read_text(encoding="utf-8")

frontend_pattern = r"^940507692158\.dkr\.ecr\.ap-northeast-2\.amazonaws\.com/threadia-frontend@sha256:[a-f0-9]{64}$"
backend_pattern = r"^940507692158\.dkr\.ecr\.ap-northeast-2\.amazonaws\.com/threadia-backend@sha256:[a-f0-9]{64}$"

assert 'id: release' in workflow
assert '${{ steps.release.outputs.command_id }}' in workflow
assert '${{ steps.command.outputs.command_id }}' not in workflow

for parameter, pattern in (
    ("FrontendImageRef", frontend_pattern),
    ("BackendImageRef", backend_pattern),
):
    parameter_source = document.split(f"  {parameter}:", 1)[1].split("\n", 1)[0]
    assert "interpolationType: ENV_VAR" in parameter_source
    assert f"allowedPattern: '{pattern}'" in parameter_source

frontend_reference = "940507692158.dkr.ecr.ap-northeast-2.amazonaws.com/threadia-frontend@sha256:" + "a" * 64
backend_reference = "940507692158.dkr.ecr.ap-northeast-2.amazonaws.com/threadia-backend@sha256:" + "b" * 64

assert re.fullmatch(frontend_pattern, frontend_reference)
assert re.fullmatch(backend_pattern, backend_reference)
assert not re.fullmatch(frontend_pattern, backend_reference)
assert not re.fullmatch(backend_pattern, frontend_reference)
assert not re.fullmatch(frontend_pattern, frontend_reference.replace("@sha256:", ":"))
assert not re.fullmatch(backend_pattern, backend_reference[:-1])

print("Public dual-image SSM source preflight passed; no AWS or GitHub command was executed.")
