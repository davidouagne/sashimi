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

## Consequences

- The former single `build` job, required by the `main` ruleset, is replaced by `CI status`; the ruleset's required contexts must switch from `build` to `CI status` once this lands (rulesets ticket of map #70).
- The `build` job name survives, but as one leg aggregated by `CI status`, not as a required context.
