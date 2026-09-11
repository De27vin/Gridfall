# Gridfall

Gridfall is a tactical block-puzzle game for Android. Place shapes, clear rows and columns, complete optional contracts, use special jokers, and compete on global leaderboards.

The game includes three board styles:

- **Rush (7×7):** two playable pieces with a rolling next-piece preview
- **Classic (8×8):** the balanced three-piece experience
- **Marathon (10×10):** a larger board with four-piece batches

## Toolstack

- **Android:** Kotlin, Jetpack Compose, Material 3, Android Gradle Plugin
- **Authentication and networking:** Firebase Authentication, OkHttp
- **API:** Node.js, TypeScript, Fastify, Zod
- **Database:** PostgreSQL, Prisma ORM
- **Testing:** JUnit, Vitest, AndroidX Test, Espresso
- **Infrastructure:** Docker Compose, Cloudflare Workers/Pages, Wrangler
- **Website:** HTML, CSS, JavaScript

## Development

Build and test the Android app:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Run the API locally:

```bash
cd api
npm install
npm run dev
```

Signed release builds require the local keystore configuration described in [docs/signed-release-apk.md](docs/signed-release-apk.md).

## Download

The current Android build is available at [gridfall.site](https://gridfall.site).
