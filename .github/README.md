# GitHub automation

`workflows/build.yml` is the public build and release pipeline for Drafts.

It runs tests, builds the debug APK, optionally builds the signed production APK when repository signing secrets are configured, and publishes the current build to the stable `compose-latest` GitHub Release.
