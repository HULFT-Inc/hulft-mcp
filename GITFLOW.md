# GitFlow Workflow

This project follows the GitFlow branching model for organized development.

## Branch Structure

### Main Branches

- **`main`** - Production-ready code. Only accepts merges from `release/*` or `hotfix/*` branches.
- **`develop`** - Integration branch for features. Default branch for development.

### Supporting Branches

- **`feature/*`** - New features (branch from `develop`, merge back to `develop`)
- **`release/*`** - Release preparation (branch from `develop`, merge to `main` and `develop`)
- **`hotfix/*`** - Production fixes (branch from `main`, merge to `main` and `develop`)

## Workflow

### Starting a New Feature

```bash
# Create feature branch from develop
git checkout develop
git pull origin develop
git checkout -b feature/my-feature

# Work on feature
git add .
git commit -m "feat: add new feature"

# Push and create PR to develop
git push origin feature/my-feature
```

### Creating a Release

```bash
# Create release branch from develop
git checkout develop
git pull origin develop
git checkout -b release/v2.1.0

# Update version numbers, finalize docs
git commit -m "chore: prepare v2.1.0 release"

# Merge to main
git checkout main
git merge --no-ff release/v2.1.0
git tag -a v2.1.0 -m "Release v2.1.0"

# Merge back to develop
git checkout develop
git merge --no-ff release/v2.1.0

# Push everything
git push origin main develop --tags
git branch -d release/v2.1.0
```

### Hotfix

```bash
# Create hotfix branch from main
git checkout main
git pull origin main
git checkout -b hotfix/v2.0.1

# Fix the issue
git commit -m "fix: critical bug"

# Merge to main
git checkout main
git merge --no-ff hotfix/v2.0.1
git tag -a v2.0.1 -m "Hotfix v2.0.1"

# Merge to develop
git checkout develop
git merge --no-ff hotfix/v2.0.1

# Push everything
git push origin main develop --tags
git branch -d hotfix/v2.0.1
```

## Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `style:` - Code style changes (formatting, etc.)
- `refactor:` - Code refactoring
- `test:` - Adding or updating tests
- `chore:` - Maintenance tasks
- `perf:` - Performance improvements
- `ci:` - CI/CD changes

## Pull Request Process

1. Create feature branch from `develop`
2. Make changes and commit
3. Push branch and create PR to `develop`
4. Wait for CI checks to pass
5. Request code review
6. Address feedback
7. Merge when approved

## CI/CD Integration

### Automated Checks

All PRs and pushes trigger:
- ✅ Build verification
- ✅ Unit tests (54 tests)
- ✅ PMD analysis (0 violations required)
- ✅ Checkstyle analysis (0 violations required)
- ✅ Code coverage (70% minimum)

### Release Process

When a tag is pushed:
1. Build JAR artifact
2. Run full test suite
3. Create GitHub release
4. Attach JAR to release
5. Generate release notes

## Branch Protection Rules

### `main` branch
- Require pull request reviews
- Require status checks to pass
- Require branches to be up to date
- No direct pushes

### `develop` branch
- Require status checks to pass
- Allow direct pushes for maintainers

## Current Status

- **Latest Release:** v2.0.0
- **Active Branch:** main
- **Quality Score:** Triple Perfect (PMD: 0, Checkstyle: 0, Tests: 100%)
- **Coverage:** 73.9%

## Quick Reference

```bash
# Clone repository
git clone git@github.com:HULFT-Inc/hulft-mcp.git

# Setup for development
git checkout develop
./gradlew build

# Run quality checks
./gradlew pmdMain checkstyleMain test

# Create feature
git checkout -b feature/my-feature develop

# Create release
git checkout -b release/v2.1.0 develop
```

## Resources

- [GitFlow Cheatsheet](https://danielkummer.github.io/git-flow-cheatsheet/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [GitHub Flow](https://guides.github.com/introduction/flow/)
