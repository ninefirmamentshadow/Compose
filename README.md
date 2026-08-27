# Drafts

**Drafts** is an offline Android writing and consistency-checking tool for composing one source draft and rendering it into multiple platform formats without sending the text anywhere.

It is designed for fast mobile drafting, repeatable formatting, and catching obvious consistency mistakes before text is copied out of the app.

[Download the current Android build](https://github.com/ninefirmamentshadow/Compose/releases/tag/compose-latest)

> **Current release status:** the public release page always exposes an installable debug APK. Once release-signing secrets are configured, the same workflow also publishes the signed production APK.

## Why Drafts exists

A single piece of copy often gets rewritten repeatedly for different platforms, which creates drift: different numbers, different contact text, inconsistent descriptors, over-limit headlines, or edits that accidentally change more than one variable during a test.

Drafts keeps one source draft and performs the mechanical work locally on the device.

## Features

### Compose

Six source fields: three headline segments rendered live as `NAME | CATEGORY | FILTER`, plus three body fields.

Choose a platform and the same source re-renders into that platform's shape with a live character count. Copy the headline, body, or full rendered draft to the clipboard.

### Check

Two checks run against the current draft:

- **Consistency** compares rate-shaped numbers, a bio descriptor, a contact handle, and a contact instruction against the canonical values configured by the user.
- **Lint** applies an editable rule set for mechanical writing and exposure checks.

Findings carry a severity, the offending substring, and the source field. Tapping a finding jumps back to the relevant text.

### Tests

Tracks headline tests by platform, date range, and inquiry count. If a new test changes more than one headline segment relative to the previous test on that platform, Drafts warns that the result will not isolate a single variable.

### Scripts

Six locally stored labelled replies. Tap to copy; edit to rewrite.

## Privacy and security model

Drafts is intentionally small and local-first:

- **No `INTERNET` permission.**
- **No location permission.**
- **No accounts, analytics, telemetry, ads, or cloud sync.**
- **No auto-posting, platform APIs, or scraping.**
- **No notifications or background alarms.**
- **No seeded personal or client data.**
- Clipboard copy is the only output path.
- Android backup and cloud transfer are disabled in the manifest.

The app is not an encrypted records vault. Draft text and configuration are stored locally in the app database, so device access remains the primary exposure boundary.

## Install

### Current public build

Open the current release:

**https://github.com/ninefirmamentshadow/Compose/releases/tag/compose-latest**

Assets use these names:

- `Drafts-current.apk` — signed production build when release signing is configured.
- `Drafts-current-debug.apk` — installable debug build, always produced as the fallback.

Android may ask you to allow installs from the browser or file manager opening the APK. The production package is `com.drafts.compose`; the debug package is `com.drafts.compose.debug`, so both can coexist on one device.

## Editing the lint rules

Rules live in:

```text
app/src/main/java/com/drafts/compose/core/lint/LintRules.kt
```

`LintRules.DEFAULT` is a plain list. The current engine supports:

| Rule shape | Purpose |
| --- | --- |
| `PatternRule` | flag every match of one regex |
| `SentencePairRule` | flag a sentence where two regexes both match |
| `EmojiCeilingRule` | flag emoji above a per-scope budget |
| `ParagraphCeilingRule` | flag paragraphs above a budget |

Rules can be scoped to `HEADLINE`, `BODY`, or `ANY`.

The shipped rules currently include checks for real-time location phrasing, price/offer proximity, defensive phrasing, emoji ceilings, paragraph count, and question-shaped boundaries.

## Design constraints

These are intentional boundaries, not missing features:

- No symbol mapping, shorthand encoding, or words-to-symbols conversion.
- No seeded listing copy; the app formats what the user writes.
- No client records or free-text inquiry log.
- No network stack.
- No location collection.
- No automatic publishing.

## Build from source

Requirements:

- JDK 17
- Android SDK platform 35

From the repository root:

```sh
./gradlew test
./gradlew assembleDebug
```

The debug APK is produced under:

```text
app/build/outputs/apk/debug/
```

## CI and releases

`.github/workflows/build.yml` runs on pushes, pull requests, and manual dispatch.

The build pipeline:

1. Runs the unit tests.
2. Builds the debug APK.
3. Builds a signed release APK when signing secrets are present.
4. Publishes the current APK to the stable `compose-latest` GitHub Release.

A failing test stops the build before release publication.

## Maintainer: configure release signing

The workflow expects four **repository-level GitHub Actions secrets**:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | base64-encoded Android signing keystore |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | signing-key alias |
| `KEY_PASSWORD` | signing-key password |

In GitHub, open **Settings → Secrets and variables → Actions → New repository secret** and add each value separately.

If an existing production build has already been distributed, preserve and reuse its original signing keystore. Android treats the signing key as application identity; replacing it prevents the new APK from updating an installed copy signed by the old key.

For a first production key, one standard JDK command is:

```sh
keytool -genkeypair -v \
  -keystore drafts-release.jks \
  -alias drafts \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Then encode the keystore as one line:

```sh
base64 -w 0 drafts-release.jks > drafts-release.jks.b64
```

Use the contents of `drafts-release.jks.b64` as `KEYSTORE_BASE64`. Keep the original `.jks` and its passwords in durable offline backup. **Never commit the keystore, its base64 representation, or its passwords to this repository.**

After adding the secrets, manually re-run the `build` workflow. The next successful run will publish `Drafts-current.apk` on the stable release page.

## Project structure

```text
app/src/main/java/com/drafts/compose/
  core/                     pure Kotlin checking/rendering logic
    Findings.kt
    ScopedText.kt
    TextScan.kt
    render/Renderer.kt
    lint/LintRules.kt
    lint/LintEngine.kt
    check/ConsistencyChecker.kt
    check/Checks.kt
    tests/TestGuard.kt
  data/                     Room entities, DAOs, migrations
  ui/                       single activity, four fragments, ViewBinding
app/src/test/               unit tests for core logic
```

Database migrations are additive. `fallbackToDestructiveMigration` is deliberately not used; a missing migration should fail during development rather than silently wipe user data.

## License

No open-source license is currently declared in this repository. Public source visibility does not, by itself, grant reuse or redistribution rights beyond what applicable law provides.
