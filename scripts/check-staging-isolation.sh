#!/usr/bin/env bash
# Requires the Android SDK/JDK used by the normal build. No credentials are needed.
set -euo pipefail
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"
case_log=$(mktemp)
trap 'rm -f "$case_log"' EXIT
for endpoint in MISSING https://api.gridfall.site https://api.gridfall.site. https://API.GRIDFALL.SITE https://api.gridfall.site:443 http://staging.example.invalid; do
  args=()
  if [[ "$endpoint" != MISSING ]]; then args+=("-PgridfallStagingApiBaseUrl=$endpoint"); fi
  if bash ./gradlew :app:validateStagingIsolation "${args[@]}" --max-workers=2 --no-daemon --console=plain > "$case_log" 2>&1; then
    printf 'Unsafe staging endpoint accepted: %s\n' "$endpoint"; exit 1
  fi
  if ! grep -q 'Staging requires a non-production HTTPS' "$case_log"; then
    cat "$case_log"; exit 1
  fi
  printf 'Rejected unsafe staging endpoint: %s\n' "$endpoint"
done
