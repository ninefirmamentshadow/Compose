#!/bin/bash
# SessionStart orientation.
#
# Adapted from Sovereign-Ops. This repository installs nothing at session start;
# the hook only reports repository state so a session opens knowing where it is,
# what is dirty, and how to build and test. Every command is read-only and its
# failure is reported rather than raised.
set -uo pipefail

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0

printf '%s\n' '=== Drafts (compose) session state ==='

printf '\n-- git --\n'
branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"
printf 'branch: %s\n' "$branch"
dirty="$(git status --porcelain 2>/dev/null | wc -l | tr -d ' ')"
printf 'uncommitted paths: %s\n' "$dirty"
git log -1 --format='last commit: %h %s (%cr)' 2>/dev/null || printf 'last commit: (none)\n'

printf '\n-- build & test --\n'
cat <<'NOTE'
Unit tests:   ./gradlew test          (lint engine + consistency checker + render)
Debug APK:    ./gradlew assembleDebug
Release APK:  ./gradlew assembleRelease   (signed only when keystore material is set)
CI:           .github/workflows/build.yml runs tests + both APK paths.
NOTE

printf '\n-- reminders --\n'
cat <<'NOTE'
Read AGENTS.md before editing. The lint engine and the consistency checker are
the load-bearing logic — cover changes to them with unit tests. Canonical values
and rules the operator edits are data, not code: do not hardcode them. Never
publish a debug build as an operational release.
NOTE
