# SmartBootstrapper User Guide

This guide provides detailed instructions for using the SmartBootstrapper CLI tool.

## Table of Contents

1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Creating a Manifest](#creating-a-manifest)
4. [Running the Tool](#running-the-tool)
5. [Configuration Options](#configuration-options)
6. [Understanding the Output](#understanding-the-output)
7. [Best Practices](#best-practices)
8. [Advanced Usage](#advanced-usage)
9. [Troubleshooting](#troubleshooting)

## Introduction

SmartBootstrapper is designed to help you quickly create new Spring Boot projects by:

- Harvesting code files from existing Git repositories
- Refactoring package names automatically
- Generating a proper Spring Boot project structure

This is particularly useful when:
- Extracting microservices from a monolith
- Creating new projects based on existing templates
- Migrating code between organizations or packages

## Getting Started

### Prerequisites

1. **Java 21+**: The tool requires Java 21 or later
2. **Maven 3.8+**: For building the tool
3. **Source Repository**: Either a Git repository OR a directory containing commit ZIP files

### Installation

```bash
# Build the project
mvn clean package

# Verify it works
./smartbootstrapper --help
```

## Creating a Manifest

The manifest file tells SmartBootstrapper which files to harvest and where to put them.

### Basic Structure

```yaml
# Path to the source repository (Git directory or ZIP files directory)
sourceRepository: /path/to/your/repository

# Source type: "git" for Git repositories, "zip" for ZIP archives (default: zip)
sourceType: git

# List of files to harvest
files:
  - commit: <git-commit-hash>
    sourcePath: <path-in-source-repo>
    destinationPath: <path-in-new-project>
```

### Source Types

SmartBootstrapper supports two source types:

#### Git Repository (sourceType: git)
Traditional Git repository. Files are extracted directly from Git history.

```yaml
sourceRepository: /path/to/git/repo
sourceType: git
```

#### ZIP Archives (sourceType: zip)
Directory containing ZIP files named with pattern: `{project}-{commitHash}.zip`

```yaml
sourceRepository: /path/to/commits/directory
sourceType: zip
```

Example ZIP file naming:
- `myproject-abc1234567890.zip`
- `myproject-def4567890abc.zip`

The ZIP files should contain the project source code at that specific commit.

### Finding Commit Hashes

Use Git to find the commit hash for a file:

```bash
# Get the latest commit for a file
git log -1 --format="%H" -- path/to/file.java

# Get a specific version
git log --oneline -- path/to/file.java
```

### Example: Extracting a User Service

```yaml
sourceRepository: /home/dev/projects/monolith

files:
  # Domain model
  - commit: a1b2c3d4e5f6
    sourcePath: src/main/java/com/company/domain/User.java
    destinationPath: src/main/java/com/newcompany/userservice/domain/User.java

  # Service layer
  - commit: a1b2c3d4e5f6
    sourcePath: src/main/java/com/company/service/UserService.java
    destinationPath: src/main/java/com/newcompany/userservice/service/UserService.java

  # Repository
  - commit: a1b2c3d4e5f6
    sourcePath: src/main/java/com/company/repository/UserRepository.java
    destinationPath: src/main/java/com/newcompany/userservice/repository/UserRepository.java

  # Controller
  - commit: b2c3d4e5f6a1
    sourcePath: src/main/java/com/company/controller/UserController.java
    destinationPath: src/main/java/com/newcompany/userservice/controller/UserController.java

  # Configuration
  - commit: c3d4e5f6a1b2
    sourcePath: src/main/resources/application-user.properties
    destinationPath: src/main/resources/application.properties

  # Tests
  - commit: a1b2c3d4e5f6
    sourcePath: src/test/java/com/company/service/UserServiceTest.java
    destinationPath: src/test/java/com/newcompany/userservice/service/UserServiceTest.java
```

## Running the Tool

### Basic Execution

```bash
./smartbootstrapper manifest.yaml
```

### With Output Directory

```bash
./smartbootstrapper manifest.yaml -o /path/to/new-project
```

### Validation Only (Dry Run)

```bash
./smartbootstrapper manifest.yaml --dry-run
```

## Configuration Options

When you run the tool, you'll be prompted for various configuration options:

### Group ID
- Format: lowercase with dots (e.g., `com.mycompany`)
- Used for Maven coordinates and default package name

### Artifact ID
- Format: lowercase with hyphens (e.g., `user-service`)
- Used for project directory and JAR name

### Project Name
- Human-readable name (e.g., `User Service`)
- Defaults to formatted artifact ID

### Version
- Semantic version format (e.g., `1.0.0`, `0.0.1-SNAPSHOT`)
- Defaults to `0.0.1-SNAPSHOT`

### Spring Boot Version
- Format: `major.minor.patch` (e.g., `3.2.0`)
- Check start.spring.io for available versions

### Java Version
- Supported: 17, 21, 22, 23
- Defaults to 21

### Dependencies
- Comma-separated list of Spring Boot starters
- Common dependencies:
  - `web` - Spring Web MVC
  - `data-jpa` - Spring Data JPA
  - `security` - Spring Security
  - `actuator` - Spring Actuator
  - `validation` - Bean Validation
  - `lombok` - Lombok

### Old Package
- The package name to refactor FROM
- Must match the package structure in source files

### New Package
- The package name to refactor TO
- Typically derived from Group ID + Artifact ID

## Understanding the Output

### Validation Phase

```
Validation Results

  ✓ Manifest file exists
  ✓ Manifest file is readable
  ✓ Manifest has YAML extension
  ✓ Manifest contains entries (5 entries found)
  ✓ No duplicate destination paths
  ✓ Source repository path is specified
  ✓ Repository path exists
  ✓ Path is a Git repository
  ✓ All commits exist (2/2 commits found)
  ✓ All files exist at specified commits (5/5 files found)
  ✓ Java files can be parsed (3/3 files parseable)

✓ All validation checks passed
```

### Execution Phases

```
[1/4] Downloading Spring Boot skeleton
  ✓ Connecting to Spring Initializr...
  ✓ Downloaded skeleton project
  ✓ Extracting skeleton to target directory...
  ✓ Extracted 12 files
  → Phase complete (4 tasks)

[3/4] Harvesting files from Git history
  ✓ Harvested: User.java
  ✓ Harvested: UserService.java
  ✓ Harvested: UserRepository.java
  → Phase complete (3 tasks)

[4/4] Refactoring and writing files
  ✓ Refactored: User.java
  ✓ Refactored: UserService.java
  ✓ Wrote: User.java
  ✓ Wrote: UserService.java
  → Phase complete (4 tasks)

✓ Bootstrap completed successfully!
```

### Success Summary

```
=== Bootstrap Complete ===

✓ Project successfully created!

Summary:
  • Location: /path/to/new-project
  • Files harvested: 5
  • Files refactored: 4
    - Java files: 3
    - Properties files: 1
    - XML files: 0
  • Duration: 5 seconds

Next steps:
  • cd /path/to/new-project
  • ./mvnw spring-boot:run
```

## Best Practices

### 1. Use Specific Commit Hashes
Always use specific commit hashes rather than branch names:
```yaml
# Good
commit: abc1234567890abcdef1234567890abcdef123456

# Avoid
commit: main  # Not supported
```

### 2. Organize Your Manifest
Group related files together:
```yaml
files:
  # Domain models
  - commit: abc123
    sourcePath: src/main/java/com/old/model/User.java
    destinationPath: src/main/java/com/new/model/User.java
  - commit: abc123
    sourcePath: src/main/java/com/old/model/Role.java
    destinationPath: src/main/java/com/new/model/Role.java

  # Services
  - commit: def456
    sourcePath: src/main/java/com/old/service/UserService.java
    destinationPath: src/main/java/com/new/service/UserService.java
```

### 3. Validate First
Always run with `--dry-run` first:
```bash
./smartbootstrapper manifest.yaml --dry-run --verbose
```

### 4. Keep Package Structure Consistent
Maintain similar package structures between old and new:
```yaml
# Preserves structure
sourcePath: src/main/java/com/old/service/UserService.java
destinationPath: src/main/java/com/new/service/UserService.java
```

### 5. Include Tests
Don't forget to harvest test files:
```yaml
- commit: abc123
  sourcePath: src/test/java/com/old/service/UserServiceTest.java
  destinationPath: src/test/java/com/new/service/UserServiceTest.java
```

## Advanced Usage

### Using Different Commits for Different Files

You can harvest files from different points in history:

```yaml
files:
  # Latest version of the model
  - commit: latest123
    sourcePath: src/main/java/com/old/model/User.java
    destinationPath: src/main/java/com/new/model/User.java

  # Stable version of the service (before breaking changes)
  - commit: stable456
    sourcePath: src/main/java/com/old/service/UserService.java
    destinationPath: src/main/java/com/new/service/UserService.java
```

### Verbose Mode for Debugging

```bash
./smartbootstrapper manifest.yaml --verbose
```

This shows detailed logging including:
- File parsing details
- Refactoring changes
- Git operations

## Troubleshooting

### Error: "Failed to parse Java file"

**Cause**: The Java file has syntax errors or unsupported constructs.

**Solution**:
1. Check the file in the source repository
2. Ensure it compiles successfully
3. Use `--verbose` to see specific parse errors

### Error: "Commit not found"

**Cause**: The commit hash doesn't exist in the repository.

**Solution**:
1. Verify the commit exists: `git cat-file -t <hash>`
2. Check for typos in the commit hash
3. Ensure you're using the correct repository

### Error: "File not found at commit"

**Cause**: The file didn't exist at the specified commit.

**Solution**:
1. Check if the file existed: `git show <hash>:<path>`
2. Find when the file was added: `git log --oneline -- <path>`
3. Use the correct commit hash

### Error: "Package refactoring failed"

**Cause**: The old package name doesn't match the file content.

**Solution**:
1. Check the actual package declaration in the file
2. Ensure old package matches exactly (case-sensitive)
3. Use `--verbose` to see what packages were found

### Rollback Occurred

If the tool encounters an error, it automatically rolls back:
- Created files are deleted
- Modified files are restored
- Created directories are removed

The rollback ensures your filesystem is left in its original state.
