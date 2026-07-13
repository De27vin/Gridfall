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