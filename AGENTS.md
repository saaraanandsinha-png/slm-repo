# Agent Instructions

## Git Commits
Always use **Conventional Commits** format for all commits:

```
<type>(<scope>): <short description>
```

### Types:
- `feat` – a new feature
- `fix` – a bug fix
- `chore` – dependency updates, config changes
- `refactor` – code change that is not a fix or feature
- `style` – formatting, missing semicolons, etc
- `docs` – documentation changes
- `test` – adding or updating tests

### Examples:
```
feat(notifications): add WhatsApp notification reader
fix(ui): correct AM/PM locale display issue
chore(deps): update Room to 2.8.4 and KSP to 2.2.10-2.0.2
refactor(viewmodel): migrate from StateFlow to Room database
```

## Git Strategy
Always use **rebase** instead of merge commits:
- Never use `git merge` — use `git rebase` instead
- Never create merge commits
- Keep commit history clean and linear

```bash
# Instead of this ❌
git merge main

# Always do this ✅
git rebase main
```
