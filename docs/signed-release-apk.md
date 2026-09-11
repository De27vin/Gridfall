# Signed Release APK

Gridfall release signing is configured in `app/build.gradle.kts` from a local
root-level `keystore.properties` file. The file and all `*.keystore` files are
Git-ignored and must not be committed.

1. Create the keystore at `keystore/gridfall-release.keystore` with alias
   `gridfall-release`.
2. Copy `keystore.properties.example` to `keystore.properties`.
3. Replace the two `CHANGE_ME` values with the keystore and key passwords.
4. Build the signed APK:

   ```powershell
   .\gradlew.bat :app:assembleRelease --console=plain
   ```

The signed APK is written to:

```text
app/build/outputs/apk/release/app-release.apk
```

Release and staging use `https://api.gridfall.site`. Debug continues to use
`http://192.168.222.172:8080`.

## Automated publishing

`.github/workflows/publish-android-apk.yml` builds and uploads the signed APK
to the Cloudflare R2 object `gridfall.apk` when a `v*` tag is pushed. It can
also be started manually from the repository's Actions tab.

Configure these GitHub repository secrets under **Settings → Secrets and
variables → Actions**:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `GOOGLE_SERVICES_JSON_BASE64`
- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_ACCOUNT_ID`
- `CLOUDFLARE_R2_BUCKET`

The Cloudflare token needs R2 object read/write access. Create the keystore
secret on Linux with:

```bash
base64 -w 0 keystore/gridfall-release.keystore
```

Create the Firebase configuration secret with:

```bash
base64 -w 0 app/google-services.json
```

After the secrets are configured, publish a release with:

```bash
git tag v1.4.0
git push origin v1.4.0
```
