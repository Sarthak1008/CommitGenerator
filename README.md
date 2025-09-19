CommitGenerator

An opinionated Maven/Spring Boot CLI to generate Git commit messages from staged changes. Uses JGit to read `git diff --staged` and a simple heuristic generator to produce Conventional Commit style messages. Optionally performs the commit.

Install as Maven plugin execution (project-local)

Add to your project's `pom.xml` as a dependency plugin execution via the Spring Boot repackage JAR, or run directly with Maven.

Build

```bash
mvn -q -DskipTests package
```

Run (print message only)

```bash
java -jar target/CommitGenerator-0.0.1-SNAPSHOT.jar generate-commit
```

Run and commit

```bash
java -jar target/CommitGenerator-0.0.1-SNAPSHOT.jar generate-commit --commit
```

Git hook (auto after `git add .`)

Create `.git/hooks/prepare-commit-msg` (make it executable on Unix) to prefill message:

```bash
#!/bin/sh
# Only when no message provided
if [ -s "$1" ]; then
  exit 0
fi
MSG=$(java -jar target/CommitGenerator-0.0.1-SNAPSHOT.jar generate-commit)
echo "$MSG" > "$1"
```

On Windows (PowerShell) for local repo hook `.git/hooks/prepare-commit-msg`:

```powershell
Param(
  [string]$CommitMsgFile
)
if ((Get-Item $CommitMsgFile).Length -gt 0) { exit 0 }
$jar = "target/CommitGenerator-0.0.1-SNAPSHOT.jar"
$cmd = "java -jar `"$jar`" generate-commit"
$MSG = & powershell -NoProfile -Command $cmd
Set-Content -Path $CommitMsgFile -Value $MSG -NoNewline
```

Programmatic use

Within the packaged JAR, the command `generate-commit` will print the message and exit.

Notes
- Requires Java 21+ and Git repository in current working directory.
- Uses JGit; no network calls. You can later swap in an AI provider service.

