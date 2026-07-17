#!/usr/bin/env python3
"""Regression tests for the optional MAIL runtime parser embedded in apply-release."""

import json
import re
import sys
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
HELPER_PATH = ROOT / "scripts/ssm-delivery/apply-release"
MAIL_PARAMETER_NAMES = [
    "/threadia/restricted-nonprod/runtime/MAIL_HOST",
    "/threadia/restricted-nonprod/runtime/MAIL_PORT",
    "/threadia/restricted-nonprod/runtime/MAIL_USERNAME",
    "/threadia/restricted-nonprod/runtime/MAIL_PASSWORD",
    "/threadia/restricted-nonprod/runtime/NOTIFICATION_FROM_EMAIL",
]


def load_optional_mail_parser():
    helper = HELPER_PATH.read_text(encoding="utf-8")
    match = re.search(
        r'mail_parameters_json="\$stage/mail-runtime-parameters\.json".*?'
        r'python3 - "\$mail_parameters_json" "\$new_env" <<\'PY\'\n(.*?)\nPY',
        helper,
        re.DOTALL,
    )
    if match is None:
        raise AssertionError("optional MAIL parser heredoc was not found")
    return compile(match.group(1), str(HELPER_PATH), "exec")


class OptionalMailRuntimeParserTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.parser = load_optional_mail_parser()

    def run_parser(self, response):
        with tempfile.TemporaryDirectory() as directory:
            fixture_path = Path(directory) / "mail-runtime-parameters.json"
            runtime_env_path = Path(directory) / "runtime.env"
            fixture_path.write_text(json.dumps(response), encoding="utf-8")
            runtime_env_path.write_text("REQUIRED_SIX_ONLY=true\n", encoding="utf-8")

            previous_argv = sys.argv
            sys.argv = ["optional-mail-parser", str(fixture_path), str(runtime_env_path)]
            try:
                exec(self.parser, {"__name__": "__main__"})
            except SystemExit as error:
                exit_code = error.code
            else:
                exit_code = 0
            finally:
                sys.argv = previous_argv

            return exit_code, runtime_env_path.read_text(encoding="utf-8")

    def test_absent_group_omits_mail_environment(self):
        exit_code, runtime_env = self.run_parser(
            {"Parameters": [], "InvalidParameters": MAIL_PARAMETER_NAMES}
        )

        self.assertEqual(0, exit_code)
        self.assertEqual("REQUIRED_SIX_ONLY=true\n", runtime_env)
        self.assertNotIn("MAIL_", runtime_env)
        self.assertNotIn("NOTIFICATION_FROM_EMAIL", runtime_env)

    def test_complete_group_appends_escaped_mail_environment(self):
        values = ["smtp.example.invalid", "587", 'quoted"value', r"back\slash", "sender@example.invalid"]
        exit_code, runtime_env = self.run_parser(
            {
                "Parameters": [
                    {"Name": name, "Value": value}
                    for name, value in zip(MAIL_PARAMETER_NAMES, values)
                ],
                "InvalidParameters": [],
            }
        )

        self.assertEqual(0, exit_code)
        self.assertEqual(
            "REQUIRED_SIX_ONLY=true\n"
            'MAIL_HOST="smtp.example.invalid"\n'
            'MAIL_PORT="587"\n'
            'MAIL_USERNAME="quoted\\"value"\n'
            'MAIL_PASSWORD="back\\\\slash"\n'
            'NOTIFICATION_FROM_EMAIL="sender@example.invalid"\n',
            runtime_env,
        )

    def test_partial_group_fails_with_parameter_names_only(self):
        exit_code, runtime_env = self.run_parser(
            {
                "Parameters": [{"Name": MAIL_PARAMETER_NAMES[0], "Value": "present-value"}],
                "InvalidParameters": MAIL_PARAMETER_NAMES[1:],
            }
        )

        self.assertEqual(
            "optional runtime parameter group is incomplete or invalid: "
            + ", ".join(sorted(MAIL_PARAMETER_NAMES[1:])),
            exit_code,
        )
        self.assertEqual("REQUIRED_SIX_ONLY=true\n", runtime_env)
        self.assertNotIn("present-value", str(exit_code))

    def test_blank_value_fails_without_writing_mail_environment(self):
        exit_code, runtime_env = self.run_parser(
            {
                "Parameters": [
                    {
                        "Name": name,
                        "Value": "" if name == MAIL_PARAMETER_NAMES[3] else "valid-value",
                    }
                    for name in MAIL_PARAMETER_NAMES
                ],
                "InvalidParameters": [],
            }
        )

        self.assertEqual(
            "optional runtime parameter is blank: " + MAIL_PARAMETER_NAMES[3],
            exit_code,
        )
        self.assertEqual("REQUIRED_SIX_ONLY=true\n", runtime_env)
        self.assertNotIn("valid-value", str(exit_code))

    def test_control_character_fails_without_writing_mail_environment(self):
        exit_code, runtime_env = self.run_parser(
            {
                "Parameters": [
                    {
                        "Name": name,
                        "Value": "invalid\nvalue" if name == MAIL_PARAMETER_NAMES[0] else "valid-value",
                    }
                    for name in MAIL_PARAMETER_NAMES
                ],
                "InvalidParameters": [],
            }
        )

        self.assertEqual(
            "optional runtime parameter contains an unsupported control character: "
            + MAIL_PARAMETER_NAMES[0],
            exit_code,
        )
        self.assertEqual("REQUIRED_SIX_ONLY=true\n", runtime_env)
        self.assertNotIn("invalid\nvalue", str(exit_code))


if __name__ == "__main__":
    unittest.main()
