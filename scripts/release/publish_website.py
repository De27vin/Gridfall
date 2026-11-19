"""Deploy only the existing Cloudflare site bound to gridfall.site. Never create a project."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import urllib.error
import urllib.request

HOST = "gridfall.site"
ALIASES = {HOST, "www.gridfall.site"}
ROOT = Path(__file__).resolve().parents[2]


def select_target(domains, projects):
    workers = [d for d in domains if d.get("hostname") == HOST]
    pages = [p for p in projects if HOST in p.get("domains", [])]
    if len(workers) + len(pages) != 1:
        raise ValueError("Cannot uniquely identify the existing gridfall.site deployment; refusing changes")
    if workers:
        target = workers[0]
        if target.get("service") != "gridfall" or target.get("environment") != "production":
            raise ValueError("Website is not bound to the configured production gridfall Worker")
        bindings = [d for d in domains if d.get("service") == "gridfall" and d.get("environment") == "production"]
        if any(d.get("hostname") not in ALIASES for d in bindings):
            raise ValueError("Worker has unrelated domains; refusing a deployment affecting them")
        return {"mode": "worker", "name": "gridfall", "domains": sorted(d["hostname"] for d in bindings)}
    target = pages[0]
    name, branch = target.get("name", ""), target.get("production_branch", "")
    if not re.fullmatch(r"[a-z0-9][a-z0-9-]{0,62}", name) or not branch or any(c in branch for c in "\r\n"):
        raise ValueError("Invalid existing Pages production target")
    return {"mode": "pages", "name": name, "branch": branch}


def discover(account, token):
    def get(path):
        req = urllib.request.Request("https://api.cloudflare.com/client/v4/accounts/" + account + path,
                                     headers={"Authorization": "Bearer " + token, "User-Agent": "Gridfall-release"})
        try:
            with urllib.request.urlopen(req, timeout=30) as response:
                data = json.load(response)
            if not data.get("success") or not isinstance(data.get("result"), list):
                raise ValueError("Cloudflare target lookup returned invalid data")
            return data["result"]
        except urllib.error.HTTPError as error:
            print("Cloudflare target lookup unavailable (HTTP " + str(error.code) + ")")
            return []
    # A project is selected only by its existing, exact public hostname binding.
    domains = get("/workers/domains")
    projects = [] if any(d.get("hostname") == HOST for d in domains) else get("/pages/projects?per_page=100")
    return select_target(domains, projects)


def deploy(target):
    with tempfile.TemporaryDirectory(prefix="gridfall-website-") as temporary:
        if target["mode"] == "pages":
            directory = Path(temporary, "website")
            shutil.copytree(ROOT / "website", directory)
            # Outside the checkout so Worker config cannot be mistaken for Pages config.
            command = ["npx", "--yes", "wrangler@4", "pages", "deploy", str(directory),
                       "--project-name", target["name"], "--branch", target["branch"]]
        else:
            config = json.loads((ROOT / "wrangler.jsonc").read_text())
            if config.get("name") != target["name"] or config.get("assets", {}).get("directory") != "website":
                raise ValueError("Worker configuration does not match the verified website")
            config["assets"]["directory"] = str(ROOT / "website")
            # Preserve the already verified public aliases; never touch API/tunnel routes.
            config["routes"] = [{"pattern": host, "custom_domain": True} for host in target["domains"]]
            path = Path(temporary, "wrangler.json")
            path.write_text(json.dumps(config))
            command = ["npx", "--yes", "wrangler@4", "deploy", "--config", str(path), "--keep-vars"]
        subprocess.run(command, cwd=temporary, check=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--inspect-only", action="store_true")
    args = parser.parse_args()
    account, token = os.environ.get("CLOUDFLARE_ACCOUNT_ID", ""), os.environ.get("CLOUDFLARE_API_TOKEN", "")
    if not account or not token:
        raise SystemExit("Existing Cloudflare account/token secrets are required")
    target = discover(account, token)
    print("Verified website target:", json.dumps(target, sort_keys=True))
    if not args.inspect_only:
        deploy(target)


if __name__ == "__main__":
    main()
