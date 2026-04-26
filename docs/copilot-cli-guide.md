# GitHub Copilot CLI — Complete Guide

## What is GitHub Copilot CLI?

**GitHub Copilot CLI** is a command-line extension that brings AI-powered assistance directly into your terminal. It helps you:

- Translate natural language into shell commands
- Explain what a command does before running it
- Debug shell errors with AI suggestions
- Run `gh copilot` commands from any terminal

---

## Installation

### Prerequisites

| Requirement | Version |
|---|---|
| GitHub CLI (`gh`) | ≥ 2.32.0 |
| GitHub Copilot subscription | Individual / Business / Enterprise |

### Step 1 — Install GitHub CLI

```bash
# macOS (Homebrew)
brew install gh

# Verify
gh --version
```

### Step 2 — Authenticate

```bash
gh auth login
```

Follow the prompts to authenticate via browser or token.

### Step 3 — Install the Copilot CLI Extension

```bash
gh extension install github/gh-copilot
```

### Step 4 — Verify Installation

```bash
gh copilot --version
```

---

## Core Commands

### `gh copilot suggest`

Translates a **natural language description** into a shell command.

```bash
gh copilot suggest "list all running docker containers"
```

**Example output:**
```
Suggestion:
  docker ps

? What would you like to do?
  ❯ Copy command to clipboard
    Explain command
    Execute command
    Exit
```

#### Specify a shell type

```bash
gh copilot suggest -t shell   "find all .log files modified in the last 24 hours"
gh copilot suggest -t git     "undo the last commit but keep the changes"
gh copilot suggest -t gh      "list all open PRs in my repo"
```

| Flag | Target shell |
|------|-------------|
| `-t shell` | General bash/zsh commands |
| `-t git` | Git commands |
| `-t gh` | GitHub CLI (`gh`) commands |

---

### `gh copilot explain`

Explains **what a command does** in plain English.

```bash
gh copilot explain "git rebase -i HEAD~3"
```

**Example output:**
```
This command starts an interactive rebase for the last 3 commits.
It opens an editor where you can reorder, squash, edit, or drop commits
before they are replayed on top of the current branch...
```

#### Pipe a command directly

```bash
gh copilot explain "chmod 755 deploy.sh"
gh copilot explain "awk '{print $1}' access.log | sort | uniq -c | sort -rn"
```

---

## Interactive Workflow

When you use `suggest`, Copilot CLI presents an interactive menu:

```
? What would you like to do?
  ❯ Copy command to clipboard   → copies to clipboard, stays in menu
    Explain command             → shows AI explanation of the suggestion
    Execute command             → runs the command directly in your shell
    Revise command              → refine the suggestion with more context
    Rate response               → thumbs up/down feedback
    Exit
```

---

## Shell Aliases (Optional but Recommended)

Add these aliases to your `~/.zshrc` or `~/.bashrc` for faster access:

```bash
# ~/.zshrc
alias '??'='gh copilot suggest -t shell'
alias 'git?'='gh copilot suggest -t git'
alias 'gh?'='gh copilot suggest -t gh'
```

Reload your shell:

```bash
source ~/.zshrc
```

Now you can use:

```bash
?? "compress the logs folder into a tar.gz file"
git? "create a branch from a specific tag"
gh? "create a pull request with a draft flag"
```

---

## Practical Examples

### Shell Commands

```bash
gh copilot suggest "kill the process running on port 8080"
# → lsof -ti:8080 | xargs kill -9

gh copilot suggest "recursively delete all node_modules folders"
# → find . -name "node_modules" -type d -prune -exec rm -rf {} +

gh copilot suggest "watch CPU and memory usage in real time"
# → top  (or htop if available)
```

### Git Commands

```bash
gh copilot suggest -t git "squash my last 4 commits into one"
# → git rebase -i HEAD~4

gh copilot suggest -t git "find which commit introduced a specific string"
# → git log -S "your string" --all

gh copilot suggest -t git "cherry-pick a range of commits"
# → git cherry-pick A^..B
```

### GitHub CLI Commands

```bash
gh copilot suggest -t gh "create an issue and assign it to me"
# → gh issue create --assignee @me

gh copilot suggest -t gh "merge a PR using squash and delete the branch"
# → gh pr merge --squash --delete-branch

gh copilot suggest -t gh "list failed workflow runs"
# → gh run list --status failure
```

---

## Tips & Best Practices

| Tip | Why |
|-----|-----|
| Be specific in your description | Vague prompts produce generic commands |
| Always use **Explain** before **Execute** for unfamiliar commands | Understand what you're running |
| Use `-t git` or `-t gh` for targeted suggestions | Narrows the model's context |
| Use **Revise** to iterate | Refine suggestions without re-typing |
| Combine with `man` or `--help` | Copilot CLI + official docs = full picture |

---

## Updating the Extension

```bash
gh extension upgrade gh-copilot
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `gh copilot: command not found` | Run `gh extension install github/gh-copilot` |
| Auth errors | Run `gh auth refresh -s copilot` |
| Extension outdated | Run `gh extension upgrade gh-copilot` |
| No Copilot subscription | Check at [github.com/features/copilot](https://github.com/features/copilot) |

---

## Resources

- [Official GitHub Copilot CLI Docs](https://docs.github.com/en/copilot/github-copilot-in-the-cli)
- [GitHub CLI Docs](https://cli.github.com/manual/)
- [gh-copilot Extension on GitHub](https://github.com/github/gh-copilot)

