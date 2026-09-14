# CI signing

`dubl-debug.keystore` is deliberately committed and is used **only** for the `com.dubl.character.android.dev` development package.

It is not a production secret. Production releases use a separate private keystore stored in GitHub Actions secrets and never committed to the repository.
