# Commit policy: rebase merge, Conventional Commits per commit, DCO signoff

**Status:** accepted (see [wayfinder map #70](https://github.com/davidouagne/sashimi/issues/70), tickets [#71](https://github.com/davidouagne/sashimi/issues/71), [#75](https://github.com/davidouagne/sashimi/issues/75))

The policy exists so that version and changelog can be automated (release-please, see ADR-0003) and contributions carry explicit provenance.

## 1. Merge method: rebase only

`main` accepts only **Rebase and merge**. Merge commits and squash merges are disabled at repository level and by the `main` ruleset (`allowed_merge_methods: ["rebase"]`, required linear history; `docs/rulesets/main.json`, ADR-0003 §5). Every commit of a PR branch lands as-is on `main` and is read by release-please. A branch is updated by rebase, never by merging `main` into it.

Rejected alternatives:

- **Squash only**: the changelog would depend on the PR title alone. Rebase keeps a granular Conventional Commit log, at the cost of requiring a clean branch history (§2).
- **Merge commit**: GitHub always puts the PR title in the merge commit; release-please reads it as one more Conventional Commit and cannot ignore merge commits, so every changelog entry would appear twice.

The Dependabot auto-merge workflow uses `gh pr merge --auto --rebase` accordingly.

## 2. Conventional Commits checked per commit

Since every merged commit feeds the changelog, **every commit** of a PR must be a valid Conventional Commit, checked in CI by `commitlint` (`wagoid/commitlint-github-action`, config `.commitlintrc.yml`) on `git rev-list --no-merges BASE..HEAD`. The PR title is not checked: it never enters the history.

Config: `type-enum = [feat, fix, test, docs, chore, build, deps]`, `header-max-length: 72`, `subject-full-stop: never`, `type-empty` / `subject-empty: never`, free scopes, no `subject-case`, no body/footer line-length limit (Dependabot bodies exceed it). YAML rather than a JavaScript config file: the repository has no Node toolchain, and the action bundles `@commitlint/config-conventional`.

The type list is deliberately short — no `ci`, `refactor`, `perf` or `revert`, although sashimi's earlier history used `ci:` and `security:`: under release-please's `java` strategy those types would mostly be hidden changelog noise. CI changes are `build:` or `chore:`, security fixes are `fix:`, refactors are `chore:`.

**`deps`**: sashimi ships an **uber-jar** that bundles its dependencies, so a dependency fix (security or otherwise) only reaches users through a new release. `deps` is visible in the changelog and releasable under release-please; Dependabot's `gradle` updates use it. Consequence: only `feat`, `fix` and `deps` trigger a release (`docs` is hidden, ADR-0003 §2).

## 3. DCO signoff

Every commit must carry a `Signed-off-by:` trailer (Developer Certificate of Origin), checked by an inline script (`git rev-list --no-merges` + `grep -E '^Signed-off-by: .+ <.+>$'`). The repository setting "Require contributors to sign off on web-based commits" is enabled for commits made through the GitHub UI.

Rejected alternative: requiring **GPG/S-MIME commit signatures** — high friction for external contributors; the DCO covers the provenance intent.

## 4. Robots

- **Dependabot**: its commits already carry `Signed-off-by: dependabot[bot]`; its `commit-message.prefix` is `deps` for the `gradle` ecosystem (`deps: bump …`, releasable) and `build` for `github-actions` (`build(deps): …`, hidden), both in the `type-enum`.
- **release-please**: its `signoff` config option adds the trailer to its `chore(main): release …` commit.

No `if:` bypass: humans and robots go through the same two checks (`dco`, `commitlint`).

## Consequences

- Both checks live in `.github/workflows/commit-policy.yml` (workflow "Commit policy", jobs `dco` and `commitlint`), replacing `dco.yml`. The `dco` job name, and therefore the required status-check context, is unchanged.
- **Not retroactive**: checks are bounded to `BASE..HEAD`; existing history (merge commits, non-conventional `Bump …` commits) is not rewritten.
- `dco` and `commitlint` are required status checks of the `main` ruleset (ADR-0003 §5).
- `CONTRIBUTING.md` § "Commit style" and `.github/PULL_REQUEST_TEMPLATE.md` document `git commit -s` and the clean-history requirement.
