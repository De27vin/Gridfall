"""Read-only verification of existing Cloudflare token and R2 bucket configuration."""
import json
import os
import re
import urllib.request


def main():
    account = os.environ.get("CLOUDFLARE_ACCOUNT_ID", "")
    token = os.environ.get("CLOUDFLARE_API_TOKEN", "")
    bucket = os.environ.get("CLOUDFLARE_R2_BUCKET", "")
    if not re.fullmatch(r"[0-9a-fA-F]{32}", account) or not token or not re.fullmatch(r"[a-z0-9][a-z0-9-]{1,61}[a-z0-9]", bucket):
        raise SystemExit("Missing or invalid existing R2 configuration; no credentials printed")

    def get(path):
        request = urllib.request.Request("https://api.cloudflare.com/client/v4" + path,
                                         headers={"Authorization": "Bearer " + token, "User-Agent": "Gridfall-release"})
        with urllib.request.urlopen(request, timeout=30) as response:
            result = json.load(response)
        if not result.get("success"):
            raise SystemExit("Cloudflare rejected read-only release preflight")
        return result["result"]

    if get("/user/tokens/verify").get("status") != "active":
        raise SystemExit("Cloudflare API token is not active")
    print("Existing Cloudflare API token is active")
    if get("/accounts/" + account + "/r2/buckets/" + bucket).get("name") != bucket:
        raise SystemExit("Existing R2 bucket does not match configuration")
    print("Existing R2 bucket read-access verified; no objects modified")


if __name__ == "__main__":
    main()
