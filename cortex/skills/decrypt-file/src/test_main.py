import argparse
from unittest import TestCase
from main import init_parser, parse_args, DecryptRequest


class Test(TestCase):
    def setUp(self) -> None:
        self.parser = init_parser()
        self.token = "token"
        self.filename = "german-credit.zip"
        self.fileKey = "german-credit"
        self.project_id = "smoke-test"
        self.api = "https://cortex.example.com"
        self.expected = DecryptRequest(payload={"filename": self.filename},
                                       properties={"fileKey": self.fileKey}, token=self.token,
                                       apiEndpoint=self.api, projectId=self.project_id)

    def test_init_parser_json_string(self):
        parsed: argparse.Namespace = parse_args(["-m", self.expected.json()])
        self.evaluate(parsed)

    def evaluate(self, parsed):
        self.assertIsNotNone(parsed, "a response should have been parsed")
        result: DecryptRequest = parsed.config
        self.assertEqual(result.token, self.token, "Token should match")
        self.assertEqual(result.filename, self.filename, "Filename should match")
        self.assertEqual(result.fileKey, self.fileKey, "FileKey should match")
