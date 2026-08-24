# AGENTS.md — Drafts execution rules

These rules apply to automated coding agents working anywhere in this
repository. They are the single-app adaptation of the Sovereign-Ops execution
doctrine; Sovereign-Ops remains the umbrella source of truth, and Drafts is
vendored there under `compose/`.

## What this app is

Drafts is a multi-format text composition and consistency-checking tool. You
write one draft; the app renders it into each platform's shape, counts it
against that platform's limits, checks it against a single row of canonical
values, lints it against an editable rule set, and puts the result on the
clipboard. Private repo, sideloaded, built in CI — there is no local build step
in the normal workflow. See `README.md`.

## Repository map

- `app/src/main/java/com/drafts/compose/core/` — the load-bearing logic:
  `render/` (platform shapes), `check/` (consistency), `lint/` (rule engine),
  `tests/` (the headline kill-file guard). This code is pure and unit-tested.
- `app/src/main/java/com/drafts/compose/data/` — Room storage, DAOs, migrations,
  seed. Schema JSON is tracked under `app/schemas/`.
- `app/src/main/java/com/drafts/compose/ui/` — fragments and view models.
- `.github/workflows/build.yml` — the only CI: unit tests, debug APK, and a
  signed release APK when keystore secrets are present.

## Scope and authority

1. Follow the owner's current explicit instruction.
2. Read this file and `README.md` before editing.
3. Prefer the lowest-risk reversible implementation. Mark unresolved values
   `OWNER DECISION`, `LIVE VERIFICATION REQUIRED`, or `TODO` and continue.
4. Green CI is mechanical evidence, not semantic proof. Fix failures
   attributable to the current change without waiting for another prompt.

## Code and data rules

- The `core/` render, check, lint, and test-guard logic must stay deterministic
  and unit-testable without Android. Every change to it lands with tests.
- **Canonical values, platform limits, and lint rules are operator data, not
  code.** They are set and edited at runtime; do not hardcode a rate, a
  descriptor, a contact handle, or a rule literal into the engine.
- The consistency checker exists to catch drift precisely: a bounded, complete
  match beats a lucky substring or a truncated number. Preserve that intent —
  a wrong price must report as a mismatch, not go unevaluated.
- Validate imports before replacing live data; a failed import must leave the
  current data recoverable. Room migrations are append-only against the tracked
  schema.
- Never report success after a failed write, failed migration, or skipped test.
- **Never publish a debug build as an operational release.** The signed release
  path exists for exactly this reason.
- Setup and CI scripts must not print secrets.

## Protected data boundary

Never add, infer, reconstruct, print, or commit the owner's legal identity,
physical location detail, sleeping/real-time location, operational contact
numbers, credentials, or signing/recovery secrets. Keep them out of code,
fixtures, seed data, logs, filenames, commit messages, and build artifacts
unless the owner explicitly provides a sanitized value for that specific output.
Canonical values and rules used as test fixtures must be synthetic.

## Validation

```bash
./gradlew test            # render + check + lint + test-guard logic
./gradlew assembleDebug   # the app compiles
./gradlew assembleRelease # the shipping artifact compiles (signed if keys present)
```

A successful build is not device validation; distinguish the two.

## Git discipline

- Terse commit messages describing the actual diff, without protected literals.
- Stage only files within the intended task scope unless a dependent file (a
  test, a migration, the tracked schema) must change to stay coherent.
- Do not force-push or rewrite published history unless the owner's current
  instruction explicitly requires it.

## Completion checklist

- The requested behavior is implemented, not merely described.
- `./gradlew test` and the relevant assemble task pass, or exact failures named.
- Room schema and migrations stay coherent when entities change.
- No protected identity, location, or contact data was exposed.
- Docs and tests were updated when the implementation changed their contract.
