// Commit policy — ADR-0002.
// Checked in CI (wagoid/commitlint-github-action) on the PR's non-merge
// commits: git rev-list --no-merges BASE..HEAD.
export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [2, 'always', ['feat', 'fix', 'test', 'docs', 'chore', 'build']],
    'type-empty': [2, 'never'],
    'subject-empty': [2, 'never'],
    'subject-full-stop': [2, 'never', '.'],
    // No case constraint on the subject (ADR-0002).
    'subject-case': [0],
    'header-max-length': [2, 'always', 72],
    // Free scopes: no imposed list.
    'scope-enum': [0],
    // Generated bodies (Dependabot: markdown tables + compare URLs) exceed
    // 100 characters per line. Body/footer line length is not an ADR-0002
    // constraint, and no `if:` may exempt bots from the checks — so the config
    // must tolerate them.
    'body-max-line-length': [0],
    'footer-max-line-length': [0],
  },
};
