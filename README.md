# CommitGenerator

An opinionated Maven/Spring Boot CLI to generate Git commit messages from staged changes. Uses JGit to read `git diff --staged` and a simple heuristic generator to produce Conventional Commit style messages. Optionally performs the commit.

## Features

- Automatically generates commit messages in Conventional Commit format
- Analyzes staged changes to determine the type of commit
- Installs as a Git hook to automatically generate messages
- Works on both Windows and Unix systems

## Installation

### Option 1: Download and Use JAR Directly

1. Download the latest JAR from the [Releases](https://github.com/Sarthak1008/CommitGenerator/releases) page
2. Place the JAR in your project directory
3. Install the Git hook (see below)

### Option 2: Build from Source

```bash
git clone https://github.com/Sarthak1008/CommitGenerator.git
cd CommitGenerator
mvn -q -DskipTests package
```

### Option 3: Add as Maven Dependency

To use CommitGenerator as a dependency in your Maven project, add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>com.AutoCommitter</groupId>
    <artifactId>CommitGenerator</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Option 4: Use as Maven Plugin

You can also configure CommitGenerator as a Maven plugin in your project:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.AutoCommitter</groupId>
            <artifactId>CommitGenerator</artifactId>
            <version>0.0.1-SNAPSHOT</version>
            <executions>
                <execution>
                    <id>install-git-hook</id>
                    <phase>initialize</phase>
                    <goals>
                        <goal>install-hook</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

This will automatically install the git hook during the initialize phase of your Maven build.

Build

```bash
mvn -q -DskipTests package
```

## Usage

### Generate Commit Message Only

```bash
java -jar CommitGenerator-0.0.1-SNAPSHOT.jar generate-commit
```

### Generate and Commit in One Step

```bash
java -jar CommitGenerator-0.0.1-SNAPSHOT.jar generate-commit --commit
```

### Install Git Hook (Recommended)

The easiest way to install the Git hook is to use the built-in command:

```bash
java -jar CommitGenerator-0.0.1-SNAPSHOT.jar install-hook
```

This will automatically create the appropriate hook file for your platform.

### Manual Hook Installation

#### Unix/Linux/Mac

Create `.git/hooks/prepare-commit-msg` (make it executable with `chmod +x`):

```bash
#!/bin/sh
# Only when no message provided
if [ -s "$1" ]; then
  exit 0
fi
JAR=CommitGenerator-0.0.1-SNAPSHOT.jar
# Run quietly (no banner/logs) so only the message is written
MSG=$(java -jar "$JAR" --spring.main.web-application-type=none --spring.main.banner-mode=off --logging.level.root=OFF generate-commit)
echo "$MSG" > "$1"
```

#### Windows (PowerShell)

Create `.git/hooks/prepare-commit-msg.ps1`:

```powershell
Param([string]$CommitMsgFile)
$jar = 'CommitGenerator-0.0.1-SNAPSHOT.jar'
# Skip if there is any non-comment content
$raw = Get-Content -Path $CommitMsgFile -Raw -ErrorAction SilentlyContinue
$lines = @()
if ($null -ne $raw) { $lines = $raw -split "`n" }
$hasText = $lines | Where-Object { $_ -notmatch '^[	 ]*#' } | Where-Object { $_.Trim() -ne '' } | Measure-Object | Select-Object -ExpandProperty Count
if ($hasText -gt 0) { exit 0 }
$MSG = & powershell -NoProfile -Command "java -jar `"$jar`" --spring.main.web-application-type=none --spring.main.banner-mode=off --logging.level.root=OFF generate-commit"
Set-Content -Path $CommitMsgFile -Value $MSG -NoNewline
```

## Example Output

The tool generates commit messages in the Conventional Commit format:

```
feat(auth): 2 added, 1 modified - implement user authentication
```

## License

MIT

Programmatic use

Within the packaged JAR, the command `generate-commit` will print the message and exit.

Notes
- Requires Java 21+ and Git repository in current working directory.
- Uses JGit; no network calls. You can later swap in an AI provider service.

