# Contributing to Sashimi

Thanks for considering a contribution!

## Building and testing locally

```bash
./gradlew build
./gradlew test
```

## Code style

This project uses [ktlint](https://pinterest.github.io/ktlint/) to enforce the
official Kotlin code style. Before opening a pull request, run:

```bash
./gradlew ktlintFormat
```

CI runs `./gradlew ktlintCheck` and fails the build on any violation.

## Commit signoff (DCO)

Every commit must carry a `Signed-off-by` trailer, certifying that you wrote
the change or otherwise have the right to submit it under the
[Developer Certificate of Origin](https://developercertificate.org/). Add it
automatically by committing with `-s`:

```bash
git commit -s -m "your message"
```

A CI check verifies that every commit in a pull request carries a valid
signoff; pull requests with unsigned commits will fail that check.

## Opening a pull request

1. Fork the repository and create a branch from `main`.
2. Make your change, with tests where applicable.
3. Run `./gradlew test ktlintCheck` locally before pushing.
4. Open a pull request describing the change and its motivation.

## Reporting bugs and requesting features

Open a GitHub issue. See `docs/agents/triage-labels.md` for the labels
maintainers use to triage it.
