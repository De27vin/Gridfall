# Production Networking Checklist

Gridfall Android API URLs are configured by Gradle `BuildConfig` fields.

- Debug API URL: `http://192.168.222.172:8080`
- Release API URL placeholder: `https://api.your-domain.ch`
- Update the release URL before shipping a public build.
- Release requires a Synology reverse proxy or equivalent HTTPS endpoint with a valid certificate.
- Release builds must not use a local LAN IP.
- Release builds must not allow cleartext HTTP.
- Debug builds allow cleartext only for `192.168.222.172`.
- `google-services.json` is not a backend secret, but it should still match the intended Firebase app.
- Backend Firebase service account JSON must never be included in the Android app.
- Do not place database passwords, Firebase service account keys, or backend `.env` files in Android resources/assets.
