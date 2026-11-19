Community Maps public testing beta for Pixel 9 Pro / GrapheneOS and other supported Android devices.

- Browse, search and sort community maps; preview boards and custom block pools.
- Publish saved custom maps with public/unlisted sharing codes, and import maps for offline play.
- Rate and report maps, and manage your own publications.
- Import & Play now closes Settings and enters gameplay, including after reset confirmation.
- Improved pagination, errors, duplicate-request protection and account isolation.
- Password fields use the password keyboard. Debug-only QA login safeguards do not change release behavior.

**Beta limitations:** authenticated two-account production E2E (publishing, ownership,
real-user import counts, ratings, reporting and cleanup) remains outstanding.
Pixel 9 Pro / GrapheneOS and physical-device release behavior have not yet been verified.
Unit/local-server and emulator tests do not replace these checks. Existing score-forgery
limitations are unchanged; this release does not claim new anti-cheat protections.
The app uses the real production API https://api.gridfall.site; clearly label any temporary
test publications and unpublish only content you created. No backend deployment is included.

Install: open this release on the phone, download **Gridfall-Community-Maps-Beta.apk**,
allow your browser/file manager to install unknown apps when Android asks, and choose
**Update** for an existing Gridfall installation. The package and production signing
certificate are retained. **Do not uninstall or clear app data.** If Android reports a
signature conflict, stop and report it. No computer or Android debugging is required.
You can revoke the browser's install permission afterward.

This beta uses versionCode 22, higher than public 1.7.1 (21). Returning to that older APK
is a downgrade and is not a supported in-place update. Wait for a stable release with a
higher versionCode to retain the normal update path.

The prerelease tag is `beta-community-maps-20261119`; it does not match `v*` and does not
trigger the production R2 publisher. The website's production APK is not replaced.
