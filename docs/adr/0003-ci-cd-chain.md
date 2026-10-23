# CI/CD chain: continuous integration, automated release, dependencies, security

**Status:** accepted, in progress (see [wayfinder map #70](https://github.com/davidouagne/sashimi/issues/70); sections are added as its tickets land)

Sashimi aligns its CI/CD chain on [datahub-healthdcat-ap-exporter's ADR-0003](https://github.com/davidouagne/datahub-healthdcat-ap-exporter/blob/main/docs/adr/0003-chaine-ci-cd.md). See also ADR-0002 (commit policy).

**What "aligned" means.** The **required status-check contexts** (`CI status`, `dco`, `commitlint`, `dependency-review`), workflow files and workflow `name:`s are identical to the exporter's, so both repos can share the same ruleset shape. The **jobs behind `CI status`** follow sashimi's stack (Kotlin/Gradle) rather than copying the exporter's Python job list: imposing a job whose meaning is specific to another technology would be cargo-culting.

## 1. Continuous integration (`.github/workflows/ci.yml`)

Parallel jobs, `permissions: {}` at workflow level with minimal per-job elevation, and a `concurrency` group cancelling superseded runs of the same ref:

| Job | Content |
|---|---|
| `lint` | `ktlintCheck` (style) + `detekt` (static analysis) |
| `test` | JDK 21; `test` + `jacocoTestReport` + `jacocoTestCoverageVerification` (coverage gate); JaCoCo step summary + HTML artifact; Codecov upload (OIDC) |
| `build` | `quarkusBuild` + smoke of the runner jar: `--help`, then `transcribe` on one fixture DDL, failing if no non-empty `.fsh` is produced |
| `CI status` | aggregator: `needs: [lint, test, build]`, `if: always()`; the single stable required context |

Rejected: a `typecheck` job mirroring the exporter's `mypy --strict`. Kotlin's compiler is the type checker, and every job already compiles; a job named after another stack's tooling would only exist for symmetry.

**JDK**: 21 only, no matrix. Sashimi ships a runner jar (`jvmToolchain(21)`), not a library installed into heterogeneous runtimes as the exporter's Python package is.

**Coverage**: the real gate is `jacocoTestCoverageVerification` (LINE ≥ 85%, `build.gradle.kts`), a floor just below the level measured at adoption (87.5% by JaCoCo), to be raised over time; the exporter gates at 90%. Codecov is **purely informational** (`codecov.yml`: `project` and `patch` `informational: true`, `src/test/**` ignored) so that a Codecov outage or a fork PR without OIDC can never fail CI; the upload still runs with `if: always()` and `fail_ci_if_error: false`. Codecov reports a lower figure than JaCoCo because it counts lines differently; the JaCoCo figure is the one that gates.

**Sashimi-specific extras kept**: the JaCoCo step summary and HTML report artifact (absent from the exporter).

**Cost**: each job sets up Gradle on its own (`gradle/actions/setup-gradle`, cache written on `main` only); wall-clock time stays close to the former single job, billed minutes roughly triple. Accepted.

**Audit** *(ticket [#76](https://github.com/davidouagne/sashimi/issues/76))*: dedicated workflow `.github/workflows/audit.yml` (weekly cron, Monday 06:00 UTC, + `workflow_dispatch`). Rather than a scanner, it reads the repository's **open Dependabot alerts** (`vulnerability-alerts: read`), raised on the dependency graph that `dependency-review.yml` submits on each push to `main` — the same GitHub Advisory data the PR gate uses. The job stays **green** as long as the alerts can be read; open alerts open a `dependencies` issue deduplicated by title (job `open-issue`, `issues: write` isolated). It only turns red if reading the alerts fails. A PR touching `audit.yml` runs the `audit` job (without opening an issue) so changes to it are tested before landing.

Rejected audit tools: **osv-scanner** (needs a committed `gradle.lockfile`, i.e. adopting Gradle dependency locking; build plugins would stay uncovered since Dependabot does not refresh `buildscript-gradle.lockfile`) and **OWASP Dependency-Check** (NVD API key secret + data cache, slow first run, CPE-matching false positives). No advisory audit step in `ci.yml` (the exporter's `pip-audit (advisory)`): `dependency-review` covers PRs. Limitation: for Gradle, Dependabot can rarely open security-update PRs; an alert is fixed through the regular version updates or by hand.

## 1bis. Dependency review (`.github/workflows/dependency-review.yml`)

*(Ticket [#76](https://github.com/davidouagne/sashimi/issues/76); facts from the [Gradle dependency-security research](https://github.com/davidouagne/sashimi/blob/research/gradle-dependency-security/docs/research/gradle-dependency-security.md), [#73](https://github.com/davidouagne/sashimi/issues/73).)*

GitHub's dependency graph does not parse Gradle builds, so `actions/dependency-review-action` sees nothing unless the graph is submitted. The workflow ("Dependency review") runs on `pull_request` and on `push: main`:

- Job `dependency-submission` (`contents: write`): `gradle/actions/dependency-submission@v6`. On `main` it gives the base snapshot (also feeding Dependabot alerts and the audit), on a PR the head snapshot.
- Job `dependency-review` (PR only, `contents: read`, `needs: dependency-submission`): `fail-on-severity: high`, licence deny-list GPL-2.0/3.0 + AGPL-3.0 (all variants; sashimi is MIT), a short `retry-on-snapshot-warnings` for API lag only. The job name is the required context of the `main` ruleset, identical to the exporter's.
- **No silent pass**: when its retry times out, the review action proceeds with whatever it has, so the review only starts after a successful submission (`needs:`).
- **Scope**: submitted Gradle dependencies carry no scope by default. `DEPENDENCY_GRAPH_RUNTIME_INCLUDE_CONFIGURATIONS: ^runtimeClasspath$` marks only what ships in the runner jar as `runtime`; tests and build tooling become `development`, ignored by the default `fail-on-scopes: runtime`. Rejected: `fail-on-scopes: runtime, unknown` without scoping, which would gate on test and lint tooling too.
- **Fork PRs** get a read-only token and cannot submit: `dependency-review` then **fails explicitly** and the maintainer reviews dependency changes by hand, merging through the ruleset's admin bypass. Rejected: the documented two-workflow split (`workflow_run`), whose longer wait reintroduces the timeout silent pass; and skipping, which is a silent pass.

## 2. Versioning and changelog: release-please

*(Ticket [#75](https://github.com/davidouagne/sashimi/issues/75); facts from the [release-please research](https://github.com/davidouagne/sashimi/blob/research/release-please-gradle/docs/research/release-please-gradle.md), [#74](https://github.com/davidouagne/sashimi/issues/74).)*

**Manifest** mode (`release-please-config.json` + `.release-please-manifest.json`), root package `"."`, action `googleapis/release-please-action@v5`.

- `release-type: java` with `skip-snapshot: true`. release-please has no Gradle strategy, and `java` bumps no file by itself; `skip-snapshot` stops the strategy from opening a `X.Y.Z-SNAPSHOT` PR after each release, which would block the next release PR. Rejected: `maven` (only knows `pom.xml`), `simple` with `version-file` (overwrites the whole file).
- **The version lives in `gradle.properties`** (`version=…`), bumped through `extra-files` (`generic` updater) between `x-release-please-start-version` / `x-release-please-end` block markers — an inline marker would become part of the `.properties` value. `-PreleaseVersion` and the `0.1.0-SNAPSHOT` fallback are gone: between releases a local build reports the last released version, as in the exporter.
- **`--version`** reads `quarkus.application.version` (the Gradle project version recorded by the Quarkus build) through a picocli `IVersionProvider`, so `gradle.properties` is the single source of truth; the CI `build` smoke checks that `--version` matches it.
- **Bootstrap**: manifest `1.0.0` matches the existing `v1.0.0` GitHub Release, so release-please is never in bootstrap mode — no `bootstrap-sha`, no `last-release-sha` (the exporter needed `bootstrap-sha` only because it had no prior release). The non-conventional `Bump …` commits after `v1.0.0` are ignored.
- Post-1.0: no `bump-minor-pre-major` options.
- **Changelog sections**: the `java` default (identical to the exporter's `python` default) **except `docs`, hidden** — and since visibility also decides releasability, a `docs:` commit no longer cuts a release whose jar would be identical. Visible / releasable: `feat`, `fix`, `perf`, `deps`, `revert`. `deps` is what Dependabot's `gradle` updates use (ADR-0002 §2): bundled dependencies make their bumps user-facing.
- `include-component-in-tag: false` → tags `vX.Y.Z`.
- `signoff` (root key of the config; the action has no input for it) → the bot's `chore(main): release …` commit passes `dco` (ADR-0002 §4).
- Repository setting: "Allow GitHub Actions to create and approve pull requests" enabled (release-please opens its release PR with `GITHUB_TOKEN`).

## 3. Release publication (`.github/workflows/release.yml`)

One workflow, `on: push: branches: [main]` + `workflow_dispatch`, `permissions: {}`, `concurrency: { group: release, cancel-in-progress: false }`. Three chained jobs: `release-please` → `build` → `smoke`.

- **No PAT nor GitHub App** for release-please: the tag and Release it creates with `GITHUB_TOKEN` trigger no workflow, so the former `on: push: tags: v*` trigger would never fire. `build` and `smoke` live in the same workflow, gated on `release_created` (not on `on: release` / `on: push: tags`).
- **Catch-up**: `workflow_dispatch` with input `publish_tag` (e.g. `v1.0.1`) replays `build` → `smoke` on an existing tag, for when the automatic run failed after the tag was created (`release_created` is only true on the run that creates the release).
- `build` (`contents: write`): checkout of the tag (`persist-credentials: false`), `quarkusBuild`, check that the jar name carries the tag's version, `sha256sum` checksums, `gh release upload --clobber` of the runner jar + `checksums.txt` onto the Release created by release-please.
- `smoke` (`contents: read`, `continue-on-error: true`): downloads the assets from the GitHub Release, verifies the checksum, runs `--version` (must equal the tag), `--help` and `transcribe` on a fixture. Advisory: a published release cannot be undone.
- **Publication target**: GitHub Release assets only — no Maven Central, no container image (out of scope of map #70). So there is no `publish` job and no OIDC `environment`, unlike the exporter's PyPI publication.

**To verify on the first release**: per release-please's docs, a release PR opened or updated with `GITHUB_TOKEN` triggers no CI, which would leave the required checks of the `main` ruleset pending; yet the exporter's release PRs (0.1.7–0.1.9) did show all their checks. If they are missing here, the release PR is merged through the ruleset's Repository-admin bypass.

## Consequences

- The former single `build` job, required by the `main` ruleset, is replaced by `CI status`; the ruleset's required contexts must switch from `build` to `CI status` once this lands (rulesets ticket of map #70).
- The `build` job name survives, but as one leg aggregated by `CI status`, not as a required context.
