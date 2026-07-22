# Gridfall website

Static Cloudflare Pages site for `gridfall.site`.

## Deploy with Cloudflare Pages

Create a Pages project from this repository and configure:

- **Build command:** leave blank
- **Build output directory:** `website`

Cloudflare Pages serves `website/index.html` directly. Point `gridfall.site` and optionally `www.gridfall.site` to that Pages project. Do not alter `api.gridfall.site`; it is the backend API hostname.

## Assets

- `assets/icon.png` is copied from the Android app icon.
- The page uses CSS gameplay mockups, so `assets/preview.png` is optional. Add a real screenshot there later and update `index.html` if desired.
- Place the signed APK at `downloads/gridfall.apk` before deployment. APK files should not be committed.

## Local preview

Open `index.html` directly in a browser, or serve this folder with any static file server.