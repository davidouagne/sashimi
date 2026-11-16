# Changelog

## [1.0.2](https://github.com/davidouagne/sashimi/compare/v1.0.1...v1.0.2) (2026-11-16)


### Dependencies

* Bump gradle-wrapper ([781fa96](https://github.com/davidouagne/sashimi/commit/781fa96927d6b989c7567b6bf6c69f5f08c9e7fc))
* Bump io.quarkus from 3.40.1.1 to 3.40.1.2 ([5a449c0](https://github.com/davidouagne/sashimi/commit/5a449c0afc7fb422c0d7cf1e9761dc20d829e10c))
* Bump io.quarkus.platform:quarkus-bom from 3.40.1.1 to 3.40.1.2 ([dd86fbb](https://github.com/davidouagne/sashimi/commit/dd86fbb34f2d27a44570255d4355146e10239881))

## [1.0.1](https://github.com/davidouagne/sashimi/compare/v1.0.0...v1.0.1) (2026-10-23)

Maintenance release: refreshed bundled dependencies, a correct `--version`, and the new automated release chain. No change to the `transcribe` CLI flags or to the generated FSH.

### Dependencies

The runner jar bundles its dependencies, so these updates only reach users through a release:

| Dependency | 1.0.0 | 1.0.1 |
|---|---|---|
| Quarkus (plugin + BOM) | 3.39.1 | 3.40.1.1 |
| HAPI FHIR (`hapi-fhir-base`, `hapi-fhir-structures-r4`) | 8.12.0 | 8.12.1 |
| jOOQ | 3.21.7 | 3.21.9 |
| SLF4J API | 2.0.18 | 2.0.20 |
| Kotlin | 2.4.10 | 2.4.21 |
| Gradle wrapper (build only) | 9.7.1 | 9.8.2 |

### Bug Fixes

* `sashimi --version` reports the real build version, read from the Quarkus application config. It used to be hard-coded to `1.0.0`.

### Build & release

* Releases are now cut by release-please from Conventional Commits. The version lives in `gradle.properties`, and each release attaches the runner jar and `checksums.txt`. The jar is then smoke-tested from the release itself (checksum, `--version`, `--help`, `transcribe`).
* CI is split into `lint` (ktlint + detekt), `test` (JaCoCo line-coverage gate at 85%) and `build` (runner-jar smoke), aggregated by `CI status`.
* Each PR gets a dependency review on the submitted Gradle graph: high-severity runtime vulnerabilities and strong-copyleft licences are blocked. Open Dependabot alerts are also audited weekly.
* Dependabot groups Quarkus, HAPI FHIR and jOOQ separately. Their patch and minor updates auto-merge once CI is green, and their bumps now trigger a release (`deps:` commits).
* Commit policy: rebase-only merges, a DCO sign-off and a Conventional Commit for every commit (see `docs/adr/0002-commit-policy.md`). `main` and `v*` tags are protected by rulesets (`docs/rulesets/`).

Details: `docs/adr/0003-ci-cd-chain.md`. Release assets now carry a `checksums.txt` verifiable with `sha256sum -c checksums.txt`.
