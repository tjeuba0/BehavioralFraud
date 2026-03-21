---
description: "Stage changes, create a conventional commit, push to remote, and optionally create a Pull Request."
---

# Commit & Push

## Pre-commit Check

```bash
echo "=== PRE-COMMIT STATUS ==="
echo "Branch: $(git branch --show-current)"
echo ""
echo "Staged:"
git diff --cached --name-only
echo ""
echo "Unstaged:"
git diff --name-only
echo ""
echo "Untracked:"
git ls-files --others --exclude-standard
echo ""
echo "Diff summary:"
git diff --stat
echo ""
echo "=== QUICK BUILD CHECK ==="
if [ -f gradlew ]; then ./gradlew assembleDebug --quiet 2>&1 | tail -3; fi
```

## Commit Process

1. **Stage** all relevant files (`.kt`, `.xml`, `.md`, `.json`, `.toml` — exclude build artifacts)
2. **Generate commit message** using Conventional Commits format:
   - `feat(scope): description` — new feature
   - `fix(scope): description` — bug fix
   - `refactor(scope): description` — code refactoring
   - `test(scope): description` — test additions
   - `docs(scope): description` — documentation
   - `chore(scope): description` — maintenance
3. **Show** the commit message to user for approval before committing
4. **Commit** with the approved message
5. **Push** to remote

## Post-push

```bash
echo "=== PUSHED ==="
git log --oneline -1
echo ""
echo "Branch: $(git branch --show-current)"
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ] && [ "$CURRENT_BRANCH" != "master" ]; then
    echo ""
    echo "This is a feature branch. Create a Pull Request?"
fi
```

If on a feature branch, ask: **"Create a Pull Request to main?"**

If yes:
```bash
gh pr create --title "[commit message]" --body "## Changes\n[list of changes]\n\n## Testing\n- Build: ✅\n- Tests: ✅" --base main
```
