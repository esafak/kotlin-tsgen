* Inspect available MCP tools before starting. Mention relevant ones to subagents.
* Assume that tests pass in the main branch.
* Follow conventional commits with the following PR template, printed as a code block for easy copying.
* Valid PR scopes are: `frontend`, `backend`, `indexer`, `deps`, `ci`, `skills`. Don't use them in commits.
```
## Why?

## This PR will...
* Bullet list

## Testing
* Move relevant material from previous section here.

## Notes
[things that did not fit above. omit section otherwise]
```
* Do not commit directly on `main`; create or switch to a feature branch first.
* Follow-up commits in the PR do not need to repeat the template and may use regular conventional commits.
* Do not amend or force-push over existing commits once a PR is opened; create a new commit.
* Amending commits is allowed before a PR is created.
* Do not rebase PR branches — rebasing rewrites history and destroys the PR.
* Do not create a new PR to fix a broken one — fix the existing branch and PR instead.
* Do not use --no-verify to bypass hooks; correct the issues.
* Do not download tools; ask the user to install them.
* Keep multiplatform Kotest `FunSpec` tests flat; Kotlin/JS does not support nested `context` blocks.
* Use mise to test and lint; don't call gradle directly.
* Search for Chrome-compatible browsers if Chrome is missing.
