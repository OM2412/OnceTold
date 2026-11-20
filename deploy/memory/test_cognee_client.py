import unittest

from cognee_client import normalize_results


class DatasetIsolationTests(unittest.TestCase):
    def test_only_exact_dataset_is_returned(self):
        payload = [
            {"dataset_name": "customer_2", "text": "PRIVATE OTHER CUSTOMER"},
            {"dataset_name": "customer_1", "text": "My own ticket"},
        ]
        self.assertEqual(["My own ticket"], normalize_results(payload, "customer_1"))

    def test_no_match_must_not_fall_back_to_everyone(self):
        payload = [{"dataset_name": "customer_2", "text": "PRIVATE OTHER CUSTOMER"}]
        self.assertEqual([], normalize_results(payload, "customer_1"))

    def test_unknown_provider_shapes_are_not_trusted(self):
        self.assertEqual([], normalize_results(["unlabelled memory"], "customer_1"))
        self.assertEqual([], normalize_results({"answer": "no dataset metadata"}, "customer_1"))
        self.assertEqual([], normalize_results([{"text": "no dataset metadata"}], "customer_1"))

    def test_nested_results(self):
        payload = {"results": [{"dataset_name": "customer_1", "text": "trusted"}]}
        self.assertEqual(["trusted"], normalize_results(payload, "customer_1"))


if __name__ == "__main__":
    unittest.main()
