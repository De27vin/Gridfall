# Gridfall website

Static assets for the existing `gridfall.site` Cloudflare site. The root
`wrangler.jsonc` describes the asset-only `gridfall` Worker; some installations
may use an existing Cloudflare Pages project instead. Never guess the production
target or create a replacement project during a release.

Update `release.json` plus the HTML fallback version/date and feature copy together.
The JSON drives the release highlights and version history. APKs are distributed
from https://apk.gridfall.site/gridfall.apk and are not committed to this folder.
Newsletter signup/links are independent and are not changed by a release.

The official `v*` GitHub publisher first releases the signed APK to GitHub and R2,
then checks the actual live site. If an existing Git-connected deployment already
updated the site, no extra deployment runs. Otherwise
`scripts/release/publish_website.py` requires a unique existing `gridfall.site`
binding, preserves known site aliases, and deploys only that Worker/Pages project.
It refuses unknown/ambiguous targets and does not modify API/tunnel resources.

The R2 token may lack website permissions. Configure a dedicated GitHub secret
`CLOUDFLARE_WEBSITE_API_TOKEN` with read/deploy access to the existing website, and
`CLOUDFLARE_WEBSITE_ACCOUNT_ID` only if different from the existing R2 account.
Never put tokens in source or chat. Without existing-site access or functioning
Git auto-deployment, publication is partial; do not claim a push updated the site.

For local preview, open index.html or serve this directory using a static server.
