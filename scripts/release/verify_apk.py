"""Verify a release APK using stable SDK tools and the original public signing identity."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
CERTIFICATE = "cb43563295f8134b162e71dfe43db34f325d4f22336129aa5dc825c6ad221d24"


def verify(apk, version):
    source = (ROOT / "app/build.gradle.kts").read_text()
    code = int(re.search(r"versionCode\s*=\s*(\d+)", source).group(1))
    name = re.search(r'versionName\s*=\s*"([^"]+)"', source).group(1)
    if not re.fullmatch(r"\d+\.\d+\.\d+", version) or version != name or code < 23:
        raise ValueError("Stable tag/source version mismatch or invalid versionCode")
    tools = Path(os.environ["ANDROID_HOME"], "build-tools", "36.0.0")
    pem = subprocess.check_output([str(tools / "apksigner"), "verify", "--verbose", "--print-certs-pem", str(apk)])
    der = subprocess.check_output(["openssl", "x509", "-outform", "DER"], input=pem)
    if hashlib.sha256(der).hexdigest() != CERTIFICATE:
        raise ValueError("Production signing certificate mismatch; refusing publication")
    metadata = subprocess.check_output([str(tools / "aapt"), "dump", "badging", str(apk)], text=True)
    expected = f"package: name='com.example.gridfall' versionCode='{code}' versionName='{version}'"
    if expected not in metadata:
        raise ValueError("APK package/version differs from release source")
    if "application-debuggable" in metadata:
        raise ValueError("Release APK is debuggable")
    config_path = ROOT / "app/google-services.json"
    if config_path.exists():
        config = json.loads(config_path.read_text())
        if config["project_info"]["project_id"] != "gridfall-auth" or not any(
            c["client_info"]["android_client_info"]["package_name"] == "com.example.gridfall" for c in config["client"]):
            raise ValueError("Firebase release project/package mismatch")
    print(f"Verified com.example.gridfall {version} ({code}), certificate {CERTIFICATE}")


if __name__ == "__main__":
    verify(Path(sys.argv[1]), sys.argv[2])
