# AGENTS.md

Keep this file current: update it in the same change whenever something it describes changes, and remove stale info rather than appending to it.

## Workflow

- After every change, run `./gradlew spotlessApply` and make sure everything compiles.
- Run tests with `./gradlew test`.
- Never edit `CHANGELOG.md`; semantic-release writes it.
- Never run `publish.sh`.

## Code style

- Write self-explanatory code with sensible function and variable names.
- Comments are rare and short. Add one only when something needs clarifying or the code is complicated.
- Be concise, in code, docs and messages alike.
- Lombok is configured with chained accessors (setters return `this`); follow `lombok.config`.

## Git and PRs

- Commit messages are a single-line conventional commit, concise, e.g. `fix: Decode a decimal N without throwing NumberFormatException`.
- The commit type drives the release: `fix` is a patch, `feat` is a minor, and `!` is a major.
- No commit footers, and no `Co-Authored-By` lines.
- Never force-push. Preserve branch history on PRs.
- PR titles are conventional commits (enforced by `semantic-pr.yml`).
- PR bodies state the problem and the solution concisely, with evidence where relevant, and assume the reader knows the existing code.
