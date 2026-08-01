# Drafts

A multi-format text composition and consistency-checking tool.

You write one draft. The app renders it into each platform's shape, counts it
against that platform's limits, checks it against a single row of canonical
values, lints it against an editable rule set, and puts the result on the
clipboard. That is the whole app.

Private repo. Sideloaded. Built in CI — there is no local build step in the
normal workflow.

---

## What it does

**COMPOSE** — six source fields: three headline segments rendered live as
`NAME | CATEGORY | FILTER`, and three body fields. Pick a platform; the same
source re-renders into that platform's shape with a live character count that
turns red past the limit. Three copy buttons: headline, body, full.

**CHECK** — two passes over the current draft, re-run on every keystroke.
Each finding carries a severity (BLOCK / WARN), the exact offending substring,
and the field it came from. Tap a finding to jump to that field with the text
selected.

- *Consistency* compares the draft against **canonical values** — three rates,
  a bio descriptor, a contact handle, a contact instruction. A number that reads
  as a rate but matches none of the canonical rates is a BLOCK. A descriptor
  that appears but is not byte-identical is a WARN. A missing or drifted contact
  handle is a BLOCK. Set the canonical values from the button at the top of the
  tab; until they are set, the checker has nothing to compare against and stays
  quiet.
- *Lint* runs pattern rules. See [Editing the rules](#editing-the-rules).

**TESTS** — the headline kill-file. Every headline that ran, on which platform,
over which dates, and how many inquiries came in. Sort by date or by inquiry
count. Starting a test that changes more than one headline segment against the
last test on that platform raises a blocking dialog — *two variables changed,
this test won't tell you anything* — with continue/cancel.

**SCRIPTS** — six labelled replies. Tap to copy, edit to reword. Nothing else.

---

## Editing the rules

All lint rules live in one file:

    app/src/main/java/com/drafts/compose/core/lint/LintRules.kt

`LintRules.DEFAULT` is a plain list. Add an entry, delete an entry, change a
regex, change a severity, change a message — the engine never needs touching,
because it knows the four rule shapes and nothing about what any rule is for:

| shape | flags |
| --- | --- |
| `PatternRule` | every match of one regex |
| `SentencePairRule` | a sentence where two regexes both match |
| `EmojiCeilingRule` | emoji past a per-scope budget |
| `ParagraphCeilingRule` | paragraphs past a budget |

Each rule is scoped to `HEADLINE`, `BODY`, or `ANY`. A rule shape that does not
exist yet needs a new `LintRule` subtype and one `when` branch in `LintEngine`;
that is the only coupling between rules and engine.

Shipped rules:

| id | severity | flags |
| --- | --- | --- |
| `INCALL_PHRASING` | BLOCK | incall / hosting phrasing |
| `REALTIME_LOCATION` | BLOCK | "here now", "in town at", "currently at", "room *N*" |
| `PRICE_NEAR_OFFER` | BLOCK | a price in the same sentence as what it buys |
| `DEFENSIVE_PHRASING` | WARN | "no time wasters", "serious inquiries only", … |
| `EMOJI_HEADLINE` | WARN | more than 2 emoji in the headline |
| `EMOJI_BODY` | WARN | more than 5 emoji in the body |
| `BODY_PARAGRAPHS` | WARN | body past three paragraphs |
| `BOUNDARY_AS_QUESTION` | WARN | a boundary phrased as a question |

One note on `PRICE_NEAR_OFFER`: it pairs a money token with generic duration and
commerce vocabulary — hour, session, booking, includes, package, and so on. It
does not ship a vocabulary of services, and adding one would violate the design
constraint below. If you extend it, extend it with commerce language.

---

## Design constraints

These are load-bearing. They are not features that were skipped:

- **No symbol mapping of any kind.** No emoji substitution, no shorthand, no
  encoding, no words-to-symbols or symbols-to-words conversion. The app moves
  text; it does not translate it.
- **No seeded listing copy.** The app ships with no example content beyond
  neutral filler. It formats what you write; it does not supply anything to say.
- **No client data.** No inquiry contents, no other people's handles, no dates
  tied to individuals. `HeadlineTest` carries an inquiry *count* and no free
  text field, by design.
- **No `INTERNET` permission.** There is no network code. The manifest declares
  no permissions at all. (An APK inspection shows one entry —
  `com.drafts.compose.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — which is a
  signature-level permission AndroidX defines for the app's own broadcast
  receivers. It grants no system capability.)
- **No location permission.**
- **No auto-posting, no platform APIs, no scraping.** The clipboard is the only
  output path.
- **No notifications, no alarms.**
- **No PIN lock, no encrypted database.** It is a notes app and looks like one.
  Contents are marketing copy, not records. Backup and cloud transfer are both
  switched off in the manifest.

---

## Building

### CI (the normal path)

Push to any branch. `.github/workflows/build.yml` runs:

1. `./gradlew test` — unit tests. **Red here stops everything.**
2. `./gradlew assembleDebug` → uploads `drafts-debug`
3. If signing secrets are present: `./gradlew assembleRelease` → uploads
   `drafts-release`

Artifacts are on the run page under **Artifacts**.

### Signing secrets

Set these four in **Settings → Secrets and variables → Actions**:

| secret | what |
| --- | --- |
| `KEYSTORE_BASE64` | the keystore file, base64-encoded |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

To generate a keystore and encode it:

```sh
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 \
        -validity 10000 -alias drafts
base64 -w 0 release.jks > release.jks.b64   # macOS: base64 -i release.jks -o release.jks.b64
```

Paste the contents of `release.jks.b64` into `KEYSTORE_BASE64`. **Keep
`release.jks` somewhere safe and off this repo** — losing it means no future
build can update an installed app; it has to be uninstalled and reinstalled,
which wipes its database. `*.jks`, `*.keystore` and `keystore.properties` are
gitignored.

Without the secrets the workflow still passes and produces the debug APK only.

### Local build (optional)

Requires an Android SDK with platform 35. Point at it with a `local.properties`
containing `sdk.dir=/path/to/android-sdk` (gitignored), then `./gradlew test` /
`./gradlew assembleDebug`. For a local signed release, put a
`keystore.properties` at the repo root:

```properties
storeFile=release.jks
storePassword=…
keyAlias=drafts
keyPassword=…
```

---

## Sideloading onto the phone

The target device is a Samsung A16; anything on API 26+ works.

1. Download the `drafts-release` artifact from the workflow run and unzip it.
2. Move the `.apk` to the phone (USB, or any transfer you already use).
3. On the phone, open the file. Android will ask to allow installs from
   whichever app is opening it — **Settings → Apps → [that app] → Install
   unknown apps** — and then install.
4. It appears in the launcher as **Drafts**.

Every update must be signed with the *same* keystore or Android refuses to
install over the existing app.

Debug and release builds have different application IDs
(`com.drafts.compose.debug` vs `com.drafts.compose`) so both can sit on the
device at once without one overwriting the other's data.

---

## Layout

```
app/src/main/java/com/drafts/compose/
  core/                     pure Kotlin, no Android imports — this is what the tests cover
    Findings.kt             Severity, FieldId, Finding, display ordering
    ScopedText.kt           joined text ⇄ source field offset mapping
    TextScan.kt             sentence / paragraph / emoji segmentation
    render/Renderer.kt      per-register reshaping and character counts
    lint/LintRules.kt       ← the rules file
    lint/LintEngine.kt      four rule shapes, no subject-matter knowledge
    check/ConsistencyChecker.kt
    check/Checks.kt         both passes, one report
    tests/TestGuard.kt      the one-variable rule
  data/                     Room: entities, DAOs, seeding, migration scaffolding
  ui/                       single activity, four fragments, ViewBinding
app/src/test/               137 unit tests, all against core/
```

Migrations are **additive only** — see `data/Migrations.kt` for the convention
and the template. `fallbackToDestructiveMigration` is deliberately never called:
a missing migration should crash in testing, not wipe drafts in the field.
Schemas are exported to `app/schemas/` and committed so migrations can be
diffed.
