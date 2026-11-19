import unittest
from publish_website import select_target


class WebsiteTargetTests(unittest.TestCase):
    def worker(self, host="gridfall.site", service="gridfall"):
        return {"hostname": host, "service": service, "environment": "production"}

    def test_existing_worker_aliases(self):
        target = select_target([self.worker(), self.worker("www.gridfall.site")], [])
        self.assertEqual(target["domains"], ["gridfall.site", "www.gridfall.site"])

    def test_existing_pages(self):
        self.assertEqual(select_target([], [{"name": "gridfall-web", "domains": ["gridfall.site"],
                                            "production_branch": "master"}])["mode"], "pages")

    def test_missing_target_refused(self):
        with self.assertRaises(ValueError):
            select_target([], [])

    def test_unrelated_worker_refused(self):
        with self.assertRaises(ValueError):
            select_target([self.worker(service="api")], [])

    def test_unrelated_bound_domain_refused(self):
        with self.assertRaises(ValueError):
            select_target([self.worker(), self.worker("api.gridfall.site")], [])

    def test_ambiguous_target_refused(self):
        with self.assertRaises(ValueError):
            select_target([self.worker()], [{"name": "gridfall", "domains": ["gridfall.site"], "production_branch": "master"}])

    def test_unrelated_pages_refused(self):
        with self.assertRaises(ValueError):
            select_target([], [{"name": "other", "domains": ["other.site"], "production_branch": "master"}])

    def test_missing_production_branch_refused(self):
        with self.assertRaises(ValueError):
            select_target([], [{"name": "gridfall", "domains": ["gridfall.site"]}])


if __name__ == "__main__":
    unittest.main()
