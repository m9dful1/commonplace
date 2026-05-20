# Commonplace — Android

A digital commonplace book for thinking with Claude, built for Android. A direct port of the [web app](../) to a phone. Same data model, same prompts, same restraint, with a thin onboarding layer for users who are meeting the project for the first time.

If you haven't read it yet, the parent project's `CLAUDE_LOG.md` is the document that explains why this app is the way it is. The on-device **Log** tab renders that file in the app's own typography. The Android port adds its own design and build entries to that file, including a Play-Store-readiness entry that records what was added for users-from-a-Play-Store-listing and why.

## What's in the box

A first-run welcome that walks new users through the project and the API key, then five places, in the same order as the web header:

- **Fragments** — capture a fragment, see them in reverse chronological order.
- **Log** — the `CLAUDE_LOG.md` document, rendered in the app's own typography.
- **Ledger** — Claude's running journal of the shape of the collection. After every fifth fragment Claude considers whether anything has changed enough to warrant a new entry. Most of the time the answer is no and nothing happens.
- **Letters** — written on demand. A 250–400 word epistle from Claude addressing the reader. Opus-tier, slow, kept.
- **Settings** — paste your Anthropic API key, test it, save it. Cost expectations, your-data section with an export button, link to About.

There is no chat view. There are no notifications. There is no pull-to-refresh, no swipe-to-delete, no haptics, no analytics. The app expects you to come to it, not the other way around.

## Stack

- **Kotlin + Jetpack Compose + Material3**, min SDK 26 (Android 8.0)
- **Room** for the SQLite schema (mirrors the web's `src/lib/db.ts` 1:1)
- **OkHttp + kotlinx-serialization** for the Anthropic API (no SDK; just HTTPS + SSE)
- The system serif (`FontFamily.Serif` → Noto Serif on most devices) is used as the body face. To swap in a bundled face like Source Serif 4, edit `ui/theme/Type.kt`.

## How to build

You'll need Android Studio Hedgehog or newer.

1. Open this `Android/` directory in Android Studio. It will sync the Gradle project and download the Gradle wrapper automatically. (If you prefer the command line, run `gradle wrapper` once to materialize `gradlew` and `gradle/wrapper/gradle-wrapper.jar`, then `./gradlew assembleDebug`.)
2. Plug in a phone with developer mode enabled, or start an emulator.
3. Run the app from Android Studio. The first launch shows a three-page welcome that walks through the project and the API key.

The first marginal note costs around USD $0.006; a Letter costs around $0.03 (Opus). Ten dollars of Anthropic credit lasts most users a long time.

## Privacy

This section is the on-device privacy disclosure that ships with the app, repeated here.

**What stays on this device:**
- Every fragment, marginal note, Ledger entry, and Letter
- Your Anthropic API key
- All settings

**What leaves this device:**
- When you ask for a margin note: the fragment, plus a small amount of context (recent fragments and prior margin notes on the same fragment), is sent to Anthropic's Claude API using your own API key.
- The same applies to Ledger updates and to Letters.
- Anthropic processes those calls under their own privacy terms.
- The app does not communicate with any other server.

**What the app collects:** nothing. No analytics, no crash reporting, no telemetry, no advertising IDs. The app does not know who you are.

**Where the data lives:** in this app's internal storage at `/data/data/com.commonplace/databases/commonplace.db`. That file is not readable by other apps and not visible in the Files app. Uninstalling the app deletes it. The app excludes itself from Android's automatic cloud backup and device-transfer (`data_extraction_rules.xml`).

**How to take your data with you:** Settings → *export all data as JSON*. The exporter writes a single JSON file via the system file picker, with the same shape as the web app's database tables, so the structure is recognizable if you ever move to the web version. There is no import feature in v1; the export is "save what you have," not "round-trip between devices regularly."

**The API key, specifically:** It is stored as plain text in the local SQLite database. Encrypting it at rest with `EncryptedSharedPreferences` is documented as a v2 upgrade. If a phone is unlocked and someone has root access, they could read the key out of the database file; on a non-rooted device, the app's internal storage is sandboxed from other apps.

## Publishing notes (Google Play Store)

Adding the project to the Play Store is straightforward in spirit but has some required boxes. A short checklist for whoever does the listing:

- **Application ID:** `com.commonplace`
- **Version:** see `app/build.gradle.kts` (`versionCode`, `versionName`)
- **Privacy Policy:** required by Play Store. The `Privacy` section above and the on-device About screen contain the disclosure. A hosted version (e.g. on GitHub Pages) is what the Play Console asks for.
- **Data safety form:** declare the following:
  - *Data collected:* none, by the app itself.
  - *Data shared with third parties:* the user's fragment text and the configured context (recent fragments / marginalia) is sent to Anthropic when the user requests a margin note, Ledger update, or Letter. The user provides their own API key.
  - *Data is encrypted in transit:* yes, HTTPS to api.anthropic.com.
  - *Data deletion:* uninstalling the app deletes everything; the user can also export their data first via Settings.
- **Content rating:** general; no user-generated content shared between users.
- **Screenshots:** capture the empty Fragments tab, a fragment with a margin note rendered, the Log tab showing the project's history, the Ledger tab, the Letters tab, and the Settings page. The hand-bound-notebook aesthetic is the screenshot pitch — let the typography do the talking.

## What's deferred from the web project

Stranger's Voice, constellation view, embeddings-based connections, search, in-app sync. The web project's closing entry explains why each of these was deferred. The Android port keeps the same posture: do not extend, do not enlarge, do not rebuild the deferred features as "the mobile version."

The `connections` and `claude_log_entries` tables are created in the Room schema for parity with `src/lib/db.ts`, but no DAO or screen surfaces them. Same as the web app.

## What's deliberately different from the web app, on Android

- **No `~/.commonplace/db.sqlite`.** Stored in the app's internal storage (above).
- **No `LEDGER.md` / `LETTERS.md` mirror file.** Android's internal storage is invisible to other apps and to the user; the database is the source of truth, and Settings → Export covers the artifact-on-disk use case the web's mirrors served.
- **`CLAUDE_LOG.md` ships as a read-only asset bundled into the APK.** Updating it requires shipping a new APK. This is correct: log entries are reflective artifacts written by Claudes with build context, not by the runtime model.
- **A welcome screen on first launch.** The web app had no onboarding because its only user was the project's collaborator. The Play Store version has strangers; the welcome screen is the layer between them and the app.
- **Confirmation dialogs for delete.** A tap on a phone is more accidental than a click on a desktop. The Ledger and Letters delete actions show a quiet `AlertDialog`-style confirmation in the app's body serif.
- **Export.** Settings → *export all data as JSON*. The web app deferred this; for a Play Store app where users may switch phones or accidentally uninstall, "let me take my data with me" becomes load-bearing.

## License

Same as the parent project. MIT.
