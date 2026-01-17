# Jules

Jules, the asynchronous coding agent from Google, in the terminal.

## Quick start
```bash
jules # Launch the TUI

# Create a session (defaults to current working directory's repository)
jules new "write unit tests"

# Create a session for a specific repository
jules new --repo torvalds/linux "write unit tests"

# Create 3 parallel sessions for the same task
jules new --repo torvalds/linux --parallel 3 "write unit tests"

# List all sessions
jules remote list --session

# List all repos
jules remote list --repo

# Pull the result of a session
jules remote pull --session 123456

# Create multiple sessions for each task in TODO.md
cat TODO.md | while IFS= read -r line; do\
  jules new "$line";\
done

# Create a session based on the first issue assigned to @me
gh issue list --assignee @me --limit 1 --json title | jq -r '.[0].title' | jules new

# Use Gemini CLI to analyze GitHub issues and send the hardest one to Jules
gemini -p "find the most tedious issue, print it verbatim\n$(gh issue list --assignee @me)" | jules new
```


## Help

```
A CLI for Jules, the asynchronous coding agent from Google.

Usage:
  jules [flags]
  jules [command]

Available Commands:
  completion  Generate the autocompletion script for the specified shell
  help        Help about any command
  login       Login your Google account to use Jules
  logout      Logout your Google account
  remote      Interact with remote sessions, e.g. new/list/pull
  version     Show the version

Flags:
  -h, --help           help for jules
      --theme string   Which theme to use, dark/light (default "dark")

Use "jules [command] --help" for more information about a command.
```
