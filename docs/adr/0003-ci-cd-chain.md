# CI/CD chain: continuous integration, automated release, dependencies, security

**Status:** accepted (see [wayfinder map #70](https://github.com/davidouagne/sashimi/issues/70)). See also ADR-0002 (commit policy).

The chain is shaped by sashimi's stack: a Kotlin/Gradle Quarkus CLI shipped as an **uber-jar** (runner jar) on GitHub Releases. Four status checks gate `main`: `CI status`, `dco`, `commitlint`, `dependency-review`.

## 1. Continuous integration (`.github/workflows/ci.yml`)

Parallel jobs, `permissions: {}` at workflow level with minimal per-job elevation, and a `concurrency` group cancelling superseded runs of the same ref:

| Job | Content |
|---|---|
| `lint` | `ktlintCheck` (style) + `detekt` (static analysis) |
| `test` | JDK 21; `test` + `jacocoTestReport` + `jacocoTestCoverageVerification` (coverage gate); JaCoCo step summary + HTML artifact; Codecov upload (OIDC) |
| `build` | `quarkusBuild` + smoke of the runner jar: `--version` (must equal `gradle.properties`), `--help`, then `transcribe` on one fixture DDL, failing if no non-empty `.fsh` is produced |
| `CI status` | aggregator: `needs: [lint, test, build]`, `if: always()`; the single stable required context |

No separate type-checking job: Kotlin's compiler is the type checker, and every job already compiles.

**JDK**: 21 only, no matrix. Sashimi ships a runner jar (`jvmToolchain(21)`), not a library installed into heterogeneous runtimes.

**Coverage**: the real gate is `jacocoTestCoverageVerification` (LINE ≥ 85%, `build.gradle.kts`), a floor just below the level measured at adoption (87.5% by JaCoCo), to be raised over time. Codecov is **purely informational** (`codecov.yml`: `project` and `patch` `informational: true`) so that a Codecov outage or a fork PR without OIDC can never fail CI; the upload still runs with `if: always()` and `fail_ci_if_error: false`. Codecov reports a lower figure than JaCoCo because it counts lines differently; the JaCoCo figure is the one that gates.

**Cost**: each job sets up Gradle on its own (`gradle/actions/setup-gradle`, cache written on `main` only); wall-clock time stays close to a single job, billed minutes roughly triple. Accepted.

**Audit** (`.github/workflows/audit.yml`, weekly cron Monday 06:00 UTC + `workflow_dispatch`): rather than a scanner, it reads the repository's **open Dependabot alerts** (`vulnerability-alerts: read`), raised on the dependency graph that `dependency-review.yml` submits on each push to `main` — the same GitHub Advisory data the PR gate uses. Like the gate, it only reads `runtime` alerts (`scope=runtime`, see Scope below): build and test tooling never ships in the runner jar, and Dependabot cannot fix those transitive plugin dependencies, so their alerts are dismissed rather than reported. The job stays **green** as long as the alerts can be read; open alerts open a `dependencies` issue deduplicated by title (job `open-issue`, `issues: write` isolated). It only turns red if reading the alerts fails. A PR touching `audit.yml` runs the `audit` job (without opening an issue) so changes to it are tested before landing.

Rejected audit tools: **osv-scanner** (needs a committed `gradle.lockfile`, i.e. adopting Gradle dependency locking; build plugins would stay uncovered since Dependabot does not refresh `buildscript-gradle.lockfile`) and **OWASP Dependency-Check** (NVD API key secret + data cache, slow first run, CPE-matching false positives). No audit step in `ci.yml`: `dependency-review` covers PRs. Limitation: for Gradle, Dependabot can rarely open security-update PRs; an alert is fixed through the regular version updates or by hand.

## 1bis. Dependency review (`.github/workflows/dependency-review.yml`)

GitHub's dependency graph does not parse Gradle builds, so `actions/dependency-review-action` sees nothing unless the graph is submitted. The workflow ("Dependency review") runs on `pull_request` and on `push: main`:

- Job `dependency-submission` (`contents: write`): `gradle/actions/dependency-submission@v6`. On `main` it gives the base snapshot (also feeding Dependabot alerts and the audit), on a PR the head snapshot.
- Job `dependency-review` (PR only, `contents: read`, `needs: dependency-submission`): `fail-on-severity: high`, licence deny-list GPL-2.0/3.0 + AGPL-3.0 (all variants; sashimi is MIT), a short `retry-on-snapshot-warnings` for API lag only.
- **No silent pass**: when its retry times out, the review action proceeds with whatever it has, so the review only starts after a successful submission (`needs:`).
- **Scope**: submitted Gradle dependencies carry no scope by default. `DEPENDENCY_GRAPH_RUNTIME_INCLUDE_CONFIGURATIONS: ^runtimeClasspath$` marks only what ships in the runner jar as `runtime`; tests and build tooling become `development`, ignored by the default `fail-on-scopes: runtime`. Rejected: `fail-on-scopes: runtime, unknown` without scoping, which would gate on test and lint tooling too. Verified on the throwaway probe PR #83 (closed unmerged): `log4j-core 2.14.1` as `implementation` fails the review (critical advisories), as `testImplementation` passes.
- **Missing base snapshot**: on a PR whose base commit has no submitted snapshot, the review waits out its retry then compares against nothing — every dependency of the head counts as added, i.e. a full scan rather than a silent pass.
- **Fork PRs** get a read-only token and cannot submit: `dependency-review` then **fails explicitly** and the maintainer reviews dependency changes by hand, merging through the ruleset's admin bypass. Rejected: the documented two-workflow split (`workflow_run`), whose longer wait reintroduces the timeout silent pass; and skipping, which is a silent pass.

## 2. Versioning and changelog: release-please

**Manifest** mode (`release-please-config.json` + `.release-please-manifest.json`), root package `"."`, action `googleapis/release-please-action@v5`.

- `release-type: java` with `skip-snapshot: true`. release-please has no Gradle strategy, and `java` bumps no file by itself; `skip-snapshot` stops the strategy from opening a `X.Y.Z-SNAPSHOT` PR after each release, which would block the next release PR. Rejected: `maven` (only knows `pom.xml`), `simple` with `version-file` (overwrites the whole file).
- **The version lives in `gradle.properties`** (`version=…`), bumped through `extra-files` (`generic` updater) between `x-release-please-start-version` / `x-release-please-end` block markers — an inline marker would become part of the `.properties` value. There is no `-SNAPSHOT` suffix: between releases a local build reports the last released version.
- **`--version`** reads `quarkus.application.version` (the Gradle project version recorded by the Quarkus build) through a picocli `IVersionProvider`, so `gradle.properties` is the single source of truth; the CI `build` smoke checks that `--version` matches it.
- **Bootstrap**: manifest `1.0.0` matches the existing `v1.0.0` GitHub Release, so release-please is never in bootstrap mode — no `bootstrap-sha`, no `last-release-sha`. The non-conventional `Bump …` commits after `v1.0.0` are ignored.
- Post-1.0: no `bump-minor-pre-major` options.
- **Changelog sections**: the `java` defaults **except `docs`, hidden** — and since visibility also decides releasability, a `docs:` commit does not cut a release whose jar would be identical. Visible / releasable: `feat`, `fix`, `perf`, `deps`, `revert`. `deps` is what Dependabot's `gradle` updates use (ADR-0002 §2): bundled dependencies make their bumps user-facing.
- `include-component-in-tag: false` → tags `vX.Y.Z`.
- `signoff` (root key of the config; the action has no input for it) → the bot's `chore(main): release …` commit passes `dco` (ADR-0002 §4).
- Repository setting: "Allow GitHub Actions to create and approve pull requests" enabled (release-please opens its release PR with `GITHUB_TOKEN`).

## 3. Release publication (`.github/workflows/release.yml`)

One workflow, `on: push: branches: [main]` + `workflow_dispatch`, `permissions: {}`, `concurrency: { group: release, cancel-in-progress: false }`. Three chained jobs: `release-please` → `build` → `smoke`.

- **No PAT nor GitHub App** for release-please: the tag and Release it creates with `GITHUB_TOKEN` trigger no workflow, so a `on: push: tags: v*` trigger would never fire. `build` and `smoke` live in the same workflow, gated on `release_created` (not on `on: release` / `on: push: tags`).
- **Catch-up**: `workflow_dispatch` with input `publish_tag` (e.g. `v1.0.1`) replays `build` → `smoke` on an existing tag, for when the automatic run failed after the tag was created (`release_created` is only true on the run that creates the release).
- `build` (`contents: write`): checkout of the tag (`persist-credentials: false`), `quarkusBuild`, check that the jar name carries the tag's version, `sha256sum` checksums, `gh release upload --clobber` of the runner jar + `checksums.txt` onto the Release created by release-please.
- `smoke` (`contents: read`, `continue-on-error: true`): downloads the assets from the GitHub Release, verifies the checksum, runs `--version` (must equal the tag), `--help` and `transcribe` on a fixture. Advisory: a published release cannot be undone.
- **Publication target**: GitHub Release assets only — no Maven Central, no container image. So there is no publish job and no OIDC `environment`.

**Release PR checks wait for approval.** The release PR is opened and updated by `github-actions[bot]` (`GITHUB_TOKEN`). Its `pull_request` workflows (CI, Commit policy, Dependency review) **are triggered**, but GitHub holds them in `action_required` until a maintainer approves them — under the repository's Actions approval policy for outside contributors (`approval_policy: first_time_contributors`), and on every release PR in practice. The maintainer opens the PR's checks (or the run in the Actions tab) and clicks **Approve and run workflows**; the runs then execute and report the four required checks. Do **not** close and reopen the PR to trigger them: closing cancels the pending runs (they end as `failure` with no job), as happened on the 1.0.1 release PR, and reopening merely starts new runs attributed to the maintainer. The admin bypass is not needed.

## 4. Dependency management (`.github/dependabot.yml`, `dependabot-auto-merge.yml`)

**Dependabot**, ecosystems `gradle` and `github-actions` at the root, weekly on Monday, `open-pull-requests-limit: 5`, `labels: [dependencies]` (+ `github-actions`), `assignees: [davidouagne]`, `cooldown.default-days: 3` on Gradle. Commit prefixes: `deps` for Gradle (releasable, ADR-0002 §2), `build` + scope for Actions.

**Groups by name pattern**, not by `dependency-type`: for Gradle, Dependabot reports every dependency as `direct:production` (test dependencies and plugins included) and does not support `groups.dependency-type`. Instead, one group per **core library** — `quarkus` (`io.quarkus*`: plugin and BOM move together), `hapi-fhir` (`ca.uhn.hapi.fhir*`), `jooq` (`org.jooq*`) — then a `gradle-minor-patch` catch-all; for Actions a single `actions` group. Every group covers minor and patch only: a breaking bump of one core library never blocks the others, and majors arrive as individual PRs.

**Auto-merge** (`dependabot/fetch-metadata` + `gh pr merge --auto --rebase`): **patch and minor everywhere**, core libraries included — a maintainer requirement: Quarkus, HAPI FHIR and jOOQ updates must be tested and integrated without manual work. Every Dependabot PR is gated by `CI status` (unit tests, the fixture goldens comparing generated FSH, and the runner-jar smoke), `dependency-review`, `dco` and `commitlint`; `--auto` merges only once they are green. **Majors stay under human review**: a major Quarkus or HAPI release can pass the tests yet change behaviour the fixtures do not cover, and deserves a read of its release notes. When `fetch-metadata` returns an empty `update-type`, the workflow derives it from the "from X to Y" title (first two version components); an unrecognised title stays under human review.

**Token**: `gh pr merge` authenticates with the **Dependabot** secret `AUTO_MERGE_TOKEN` (fine-grained PAT, permissions *Contents*, *Pull requests* and *Workflows* read/write — *Workflows* because `github-actions` bumps edit `.github/workflows/`). A merge enabled with `GITHUB_TOKEN` triggers no workflow on the resulting push to `main`: release-please would not pick up the releasable `deps:` commit, and `dependency-review.yml` would not refresh the base snapshot. With the PAT the merge is attributed to the maintainer and triggers them like a manual merge. Missing or empty secret: a warning and fallback to `GITHUB_TOKEN`. The PAT expires: renew it before then, otherwise `gh pr merge` fails and the PR waits for a manual merge. The trigger is bounded to `pull_request: branches: [main]` so the PAT only acts under the `main` ruleset's required checks. The "no PAT" rule of §3 still holds for release-please itself.

## 5. Security and branch protection

- **Rulesets are the only protection** of `main` and of release tags; the classic branch protection is gone, because classic protection and a ruleset apply cumulatively and end up contradicting each other. Both rulesets are versioned under `docs/rulesets/` and applied through the API (`PUT /repos/{owner}/{repo}/rulesets/{id}`).
- **Ruleset `main`** (`docs/rulesets/main.json`): PR required (0 approvals, no `CODEOWNERS` — Dependabot auto-merge depends on it), `allowed_merge_methods: ["rebase"]`, review-thread resolution required, `required_linear_history`, `non_fast_forward` and `deletion` blocked, bypass Repository admin. Required status checks (`strict: false`, enforced on create): `CI status`, `dco`, `commitlint`, `dependency-review`. `CI status` aggregates the `ci.yml` jobs (§1); the commit-policy checks (ADR-0002) and `dependency-review` (§1bis) stay separate contexts.
- **Check pinning**: all four required checks are pinned to the GitHub Actions app (`integration_id: 15368`), which stops another app with write access from posting a green status under those names.
- **Ruleset `tags-v`** (`docs/rulesets/tags-v.json`): `refs/tags/v*`, deletion and non-fast-forward blocked, **no creation rule** (release-please creates the tags with `GITHUB_TOKEN`), bypass Repository admin.
- **Admin bypass, the documented escape hatch**: a fork PR whose `dependency-review` fails explicitly (§1bis). Release PRs do not need it: their checks run once approved (§3).
- **Action pinning: `@vN` (major tag) everywhere.** SHA pinning earns its cost on jobs holding OIDC or package-publishing privileges; sashimi has none (`release.yml` holds `contents: write` only and publishes GitHub Release assets). Dependabot's `actions` group keeps the major tags current.
- **CodeQL** default setup, languages **`actions` and `java-kotlin` only**. The repository contains no JavaScript or TypeScript (the commitlint config is YAML, ADR-0002 §2), so no `javascript-typescript` analysis.
- Repository settings: `delete_branch_on_merge` enabled, `default_workflow_permissions: read`, "Allow GitHub Actions to create and approve pull requests" enabled (§2), sign-off required on web-based commits (ADR-0002 §3); secret scanning + push protection, Dependabot alerts and security updates, private vulnerability reporting (`SECURITY.md`).

## Consequences

- The former single `build` job is replaced by `CI status` as the required context; the `build` job name survives as one leg aggregated by `CI status`.
