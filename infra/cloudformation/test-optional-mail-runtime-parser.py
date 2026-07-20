#!/usr/bin/env python3
"""Regression tests for the runtime parameter parsers embedded in apply-release."""

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
REQUIRED_RUNTIME_PARAMETERS = [
    "/threadia/restricted-nonprod/runtime/THREADIA_DB_URL",
    "/threadia/restricted-nonprod/runtime/THREADIA_DB_USERNAME",
    "/threadia/restricted-nonprod/runtime/THREADIA_DB_PASSWORD",
    "/threadia/restricted-nonprod/runtime/THREADIA_JWT_SECRET",
    "/threadia/restricted-nonprod/runtime/THREADIA_COOKIE_SECURE",
    "/threadia/restricted-nonprod/runtime/THREADIA_CORS_ALLOWED_ORIGIN",
    "/threadia/restricted-nonprod/runtime/THREADIA_PROJECT_CHAT_CURSOR_HMAC_SECRET",
    "/threadia/restricted-nonprod/runtime/THREADIA_NOTIFICATION_CURSOR_HMAC_SECRET",
    "/threadia/restricted-nonprod/runtime/THREADIA_AUDIT_CURSOR_HMAC_SECRET",
]
BOOTSTRAP_PARAMETER = "/threadia/restricted-nonprod/runtime/THREADIA_PLATFORM_ADMIN_BOOTSTRAP_EMAIL"


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


def load_primary_runtime_parser():
    helper = HELPER_PATH.read_text(encoding="utf-8")
    match = re.search(
        r'parameters_json="\$stage/runtime-parameters\.json".*?'
        r'python3 - "\$parameters_json" "\$new_env" <<\'PY\'\n(.*?)\nPY',
        helper,
        re.DOTALL,
    )
    if match is None:
        raise AssertionError("primary runtime parser heredoc was not found")
    return compile(match.group(1), str(HELPER_PATH), "exec")


def required_runtime_response():
    values = {
        REQUIRED_RUNTIME_PARAMETERS[0]: "jdbc:mysql://db.invalid/threadia",
        REQUIRED_RUNTIME_PARAMETERS[1]: "app-user",
        REQUIRED_RUNTIME_PARAMETERS[2]: "test-password",
        REQUIRED_RUNTIME_PARAMETERS[3]: "test-jwt-secret",
        REQUIRED_RUNTIME_PARAMETERS[4]: "true",
        REQUIRED_RUNTIME_PARAMETERS[5]: "https://app.invalid",
        REQUIRED_RUNTIME_PARAMETERS[6]: "p" * 32,
        REQUIRED_RUNTIME_PARAMETERS[7]: "n" * 32,
        REQUIRED_RUNTIME_PARAMETERS[8]: "a" * 32,
    }
    return {
        "Parameters": [
            {"Name": name, "Value": value, "Type": "SecureString"}
            for name, value in values.items()
        ]
    }


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
                        "Value": "invalid\tvalue" if name == MAIL_PARAMETER_NAMES[0] else "valid-value",
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
        self.assertNotIn("invalid\tvalue", str(exit_code))


class PrimaryRuntimeParserTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.parser = load_primary_runtime_parser()

    def run_parser(self, response):
        with tempfile.TemporaryDirectory() as directory:
            fixture_path = Path(directory) / "runtime-parameters.json"
            runtime_env_path = Path(directory) / "runtime.env"
            fixture_path.write_text(json.dumps(response), encoding="utf-8")

            previous_argv = sys.argv
            sys.argv = ["primary-runtime-parser", str(fixture_path), str(runtime_env_path)]
            try:
                exec(self.parser, {"__name__": "__main__"})
            except SystemExit as error:
                exit_code = error.code
            else:
                exit_code = 0
            finally:
                sys.argv = previous_argv

            return exit_code, runtime_env_path.read_text(encoding="utf-8") if runtime_env_path.exists() else ""

    def test_phase_one_omits_only_absent_bootstrap_parameter(self):
        response = required_runtime_response()
        response["InvalidParameters"] = [BOOTSTRAP_PARAMETER]

        exit_code, runtime_env = self.run_parser(response)

        self.assertEqual(0, exit_code)
        self.assertIn('THREADIA_PROJECT_CHAT_CURSOR_HMAC_SECRET="' + "p" * 32 + '"', runtime_env)
        self.assertIn('THREADIA_NOTIFICATION_CURSOR_HMAC_SECRET="' + "n" * 32 + '"', runtime_env)
        self.assertIn('THREADIA_AUDIT_CURSOR_HMAC_SECRET="' + "a" * 32 + '"', runtime_env)
        self.assertNotIn("THREADIA_PLATFORM_ADMIN_BOOTSTRAP_EMAIL", runtime_env)

    def test_phase_two_normalizes_and_materializes_valid_bootstrap_email(self):
        response = required_runtime_response()
        response["Parameters"].append({"Name": BOOTSTRAP_PARAMETER, "Value": " Intended.User@Example.Invalid "})
        response["InvalidParameters"] = []

        exit_code, runtime_env = self.run_parser(response)

        self.assertEqual(0, exit_code)
        self.assertIn('THREADIA_PLATFORM_ADMIN_BOOTSTRAP_EMAIL="intended.user@example.invalid"', runtime_env)

    def test_returned_blank_bootstrap_email_fails_without_materializing_environment(self):
        response = required_runtime_response()
        response["Parameters"].append({"Name": BOOTSTRAP_PARAMETER, "Value": "   "})
        response["InvalidParameters"] = []

        exit_code, runtime_env = self.run_parser(response)

        self.assertEqual("platform admin bootstrap email is blank or malformed", exit_code)
        self.assertEqual("", runtime_env)

    def test_returned_malformed_or_control_character_bootstrap_email_fails_without_materializing_environment(self):
        for value, expected_error in (
            ("not-an-email", "platform admin bootstrap email is blank or malformed"),
            ("intended@example.invalid\t", "runtime parameter contains an unsupported control character: " + BOOTSTRAP_PARAMETER),
            ("intended@example.invalid\n", "runtime parameter contains an unsupported control character: " + BOOTSTRAP_PARAMETER),
        ):
            with self.subTest(value=value):
                response = required_runtime_response()
                response["Parameters"].append({"Name": BOOTSTRAP_PARAMETER, "Value": value})
                response["InvalidParameters"] = []

                exit_code, runtime_env = self.run_parser(response)

                self.assertEqual(expected_error, exit_code)
                self.assertEqual("", runtime_env)

    def test_invalid_cursor_secret_fails_without_materializing_environment(self):
        for parameter_name in REQUIRED_RUNTIME_PARAMETERS[-3:]:
            for value in ("short", " " * 32, "valid\tvalue" * 4):
                with self.subTest(parameter_name=parameter_name, value=value):
                    response = required_runtime_response()
                    response["Parameters"] = [
                        {
                            "Name": item["Name"],
                            "Value": value if item["Name"] == parameter_name else item["Value"],
                            "Type": item["Type"],
                        }
                        for item in response["Parameters"]
                    ]
                    response["InvalidParameters"] = [BOOTSTRAP_PARAMETER]

                    exit_code, runtime_env = self.run_parser(response)

                    expected_error = (
                        "runtime parameter contains an unsupported control character: " + parameter_name
                        if "\t" in value
                        else "cursor runtime secret is blank or shorter than 32 UTF-8 bytes: " + parameter_name
                    )
                    self.assertEqual(expected_error, exit_code)
                    self.assertEqual("", runtime_env)

    def test_non_secure_string_cursor_secret_fails_without_materializing_environment(self):
        for parameter_name in REQUIRED_RUNTIME_PARAMETERS[-3:]:
            with self.subTest(parameter_name=parameter_name):
                response = required_runtime_response()
                for parameter in response["Parameters"]:
                    if parameter["Name"] == parameter_name:
                        parameter["Type"] = "String"
                response["InvalidParameters"] = [BOOTSTRAP_PARAMETER]

                exit_code, runtime_env = self.run_parser(response)

                self.assertEqual(
                    "cursor runtime parameter must be SecureString: " + parameter_name,
                    exit_code,
                )
                self.assertEqual("", runtime_env)

    def test_base_or_audit_mismatch_fails_without_materializing_environment(self):
        response = required_runtime_response()
        response["Parameters"] = response["Parameters"][:-1]
        response["InvalidParameters"] = [REQUIRED_RUNTIME_PARAMETERS[-1], BOOTSTRAP_PARAMETER]

        exit_code, runtime_env = self.run_parser(response)

        self.assertEqual("required SecureString runtime parameter is missing", exit_code)
        self.assertEqual("", runtime_env)


if __name__ == "__main__":
    unittest.main()
