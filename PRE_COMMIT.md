# Pre-commit Hooks

This project uses [pre-commit](https://pre-commit.com/) to enforce code quality and prevent direct commits to protected branches.

## Installation

```bash
# Install pre-commit
pip install pre-commit

# Install the git hooks
pre-commit install
```

## Hooks Configured

- **no-commit-to-branch**: Prevents direct commits to `main` branch
- **trailing-whitespace**: Removes trailing whitespace
- **end-of-file-fixer**: Ensures files end with a newline
- **check-yaml**: Validates YAML files
- **check-added-large-files**: Prevents large files (>1MB)
- **check-merge-conflict**: Detects merge conflict markers
- **detect-private-key**: Prevents committing private keys

## Usage

Pre-commit hooks run automatically on `git commit`. If a hook fails, the commit is blocked.

### Bypass (Emergency Only)

```bash
git commit --no-verify -m "emergency fix"
```

### Run Manually

```bash
# Run on all files
pre-commit run --all-files

# Run on staged files
pre-commit run
```

## Workflow

1. Create feature branch: `git checkout -b feature/my-feature`
2. Make changes and commit (hooks run automatically)
3. Push branch: `git push origin feature/my-feature`
4. Create Pull Request to `main`
5. Merge via PR (bypasses local hooks)

## Protected Branches

- `main` - Production, no direct commits allowed
- Use Pull Requests for all changes to main
