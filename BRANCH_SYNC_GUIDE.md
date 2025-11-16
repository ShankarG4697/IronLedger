# Branch Synchronization Guide

## Why Can't I Create a PR from Master to Development?

The `master` and `development` branches in this repository have **unrelated histories** - they don't share a common ancestor commit. This happens when:

1. The `master` branch has a "grafted" history (shallow clone with no parent commits)
2. The `development` branch has a complete history chain
3. GitHub cannot compute a proper diff between unrelated branches
4. Creating a PR between unrelated histories would result in merge conflicts for every file

## Current Branch Status

```
development branch:
  273bf93 Add README.md for IronLedger project (root commit)
  c3fdc8c Add Maven and Java related files to .gitignore
  03cd923 Initial project setup with Spring Boot and Account entity
  8bac2bd Update .gitignore to remove trailing slashes for IDE directories
  f63c309 Add JWT-based authentication and user session management (current HEAD)

master branch:
  d4c5f66 Merge pull request #4 (no parent - grafted history)
```

## Recommended Solutions

### Option 1: Sync Development to Master (Recommended)

If you want to keep both branches and maintain their current state, sync `development` to `master` by merging or rebasing:

```bash
# Switch to development branch
git checkout development

# Merge master into development (preserves history)
git merge master --allow-unrelated-histories

# Or rebase development onto master (cleaner history)
git rebase master

# Push the changes
git push origin development
```

After this, both branches will share a common history and you can create PRs in either direction.

### Option 2: Recreate Master from Development

If `development` has the authoritative code and `master` was created incorrectly:

```bash
# Backup current master (optional)
git branch master-backup master

# Reset master to development
git checkout master
git reset --hard development
git push --force origin master
```

⚠️ **Warning:** This will overwrite all commits in `master` and may break any PRs or references to the old master commits.

### Option 3: Recreate Development from Master

If `master` has the authoritative code and you want to recreate `development`:

```bash
# Backup current development (optional)
git branch development-backup development

# Recreate development from master
git checkout -B development master
git push --force origin development
```

⚠️ **Warning:** This will overwrite all commits in `development`.

### Option 4: Manual Merge with Unrelated Histories

For a one-time sync when you want to preserve both histories:

```bash
# From development branch
git checkout development
git merge master --allow-unrelated-histories -m "Sync master changes into development"

# Resolve any conflicts, then push
git push origin development
```

This creates a merge commit that joins the two unrelated histories.

## Best Practices Going Forward

1. **Choose a primary branch**: Decide whether `master` or `development` (or `main`) is your primary branch
2. **Use consistent branching model**: Use either:
   - **Trunk-based**: All work merges to `main`/`master` directly
   - **GitFlow**: `development` → `master` for releases
   - **GitHub Flow**: Feature branches → `main`/`master` directly

3. **Avoid shallow clones for primary branches**: Ensure primary branches maintain full history

4. **Regular syncing**: Merge changes between branches regularly to avoid divergence

## Verifying Branch Relationships

To check if branches share a common ancestor:

```bash
# Should return a commit SHA if branches are related
git merge-base master development

# If it returns nothing or an error, branches are unrelated
```

To see the divergence:

```bash
# Commits in master not in development
git log master..development --oneline

# Commits in development not in master
git log development..master --oneline
```

## Need Help?

If you're unsure which option to choose:

1. **If development has all the latest code**: Use Option 1 (merge master into development) or Option 2
2. **If master has all the latest code**: Use Option 3
3. **If both have important changes**: Use Option 1 or 4 and resolve conflicts carefully

Always make backups of important branches before performing force pushes or history rewrites.
