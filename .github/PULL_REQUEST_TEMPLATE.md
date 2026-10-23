<!--
Rebase merge: every commit of the branch lands as-is on `main` and feeds the
changelog. The PR title never enters the history.
-->

## Summary

<!-- What and why, in a few lines. -->

## Checklist

- [ ] Every commit is DCO-signed (`git commit -s`) and is a valid Conventional Commit (`feat` / `fix` / `test` / `docs` / `chore` / `build` / `deps`), subject ≤ 72 characters, no trailing period.
- [ ] Clean branch history with no merge commits (interactive rebase before opening, updated by rebase) — every commit lands as-is on `main`.
- [ ] `./gradlew test ktlintCheck detekt` passes.
- [ ] If a SQL → FHIR mapping rule changed: the mapping table in `CONTEXT.md` is up to date (and an ADR is added if the change is hard to reverse).
