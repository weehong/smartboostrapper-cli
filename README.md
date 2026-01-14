# SmartBootstrapper CLI

A CLI tool that automates Spring Boot project setup through manifest-driven file harvesting from Git history with package refactoring.

## Features

- **Manifest-Driven**: Define files to harvest from any Git commit in a YAML manifest
- **Package Refactoring**: Automatically refactor Java package names, imports, and references
- **Spring Initializr Integration**: Generate project skeleton from start.spring.io
- **Interactive Prompts**: Guided configuration with validation feedback
- **Dry-Run Mode**: Validate manifest and repository before making changes
- **Automatic Rollback**: Clean up on errors to leave the filesystem unchanged
- **Progress Tracking**: Real-time feedback with phase indicators

## Requirements

- Java 21 or later
- Maven 3.8 or later
- Git repository with the source files

## Installation

### Build from Source

```bash
# Clone the repository
git clone https://github.com/your-org/smartbootstrapper.git
cd smartbootstrapper

# Build the project
mvn clean package

# Make the wrapper script executable
chmod +x smartbootstrapper
```

### Verify Installation

```bash
./smartbootstrapper --version
```

## Usage

### Basic Usage

```bash
./smartbootstrapper manifest.yaml
```

### With Options

```bash
./smartbootstrapper manifest.yaml --output /path/to/output --verbose
```

### Dry-Run Mode

Validate without making changes:

```bash
./smartbootstrapper manifest.yaml --dry-run
```

### Command Line Options

| Option | Description |
|--------|-------------|
| `<manifest>` | Path to the manifest YAML file (required) |
| `-o, --output` | Target directory for the new project (default: current directory) |
| `--group-id` | Maven Group ID (e.g., `com.example`) |
| `--artifact-id` | Maven Artifact ID (e.g., `my-service`) |
| `--new-package` | Target package name to refactor TO (e.g., `com.example.myapp`) |
| `--old-package` | Source package name to refactor FROM (auto-detected if not provided) |
| `--spring-boot-version` | Spring Boot version (e.g., `4.0.0`) |
| `--java-version` | Java version (`17`, `21`, `22`, or `23`) |
| `--dependencies` | Comma-separated list of Spring Boot dependencies |
| `-y, --yes` | Skip confirmation prompt and proceed with bootstrap |
| `-p, --from-project` | Path to existing project to read configuration defaults from |
| `--no-color` | Disable colored output |
| `-v, --verbose` | Enable verbose output |
| `--dry-run` | Validate without executing |
| `-h, --help` | Show help message |
| `-V, --version` | Show version information |

### Non-Interactive Mode

Run the bootstrapper without prompts by providing all required options:

```bash
./smartbootstrapper manifest.yaml \
    --group-id com.acme \
    --artifact-id payment-service \
    --new-package com.acme.payment \
    --spring-boot-version 4.0.0 \
    --java-version 21 \
    --dependencies web,data-jpa,actuator,validation \
    -o /path/to/output \
    -y
```

## Manifest Format

The manifest is a YAML file that defines:
1. The source Git repository path
2. A list of files to harvest with their commit hashes and paths

### Example Manifest

```yaml
sourceRepository: /path/to/source/repository

files:
  - commit: abc1234567890abcdef1234567890abcdef123456
    sourcePath: src/main/java/com/old/service/UserService.java
    destinationPath: src/main/java/com/new/service/UserService.java

  - commit: def5678901234567890abcdef1234567890123456
    sourcePath: src/main/resources/application.properties
    destinationPath: src/main/resources/application.properties
```

### Manifest Fields

| Field | Description |
|-------|-------------|
| `sourceRepository` | Path to the Git repository containing source files |
| `files` | List of file entries to harvest |
| `commit` | Git commit hash (7-40 hex characters) |
| `sourcePath` | Relative path to the file in the source repository |
| `destinationPath` | Relative path for the file in the new project |

## Interactive Configuration

When you run the tool, it will prompt for:

1. **Group ID**: Maven group ID (e.g., `com.example`)
2. **Artifact ID**: Maven artifact ID (e.g., `my-service`)
3. **Project Name**: Human-readable project name
4. **Version**: Project version (e.g., `0.0.1-SNAPSHOT`)
5. **Spring Boot Version**: Target Spring Boot version
6. **Java Version**: Target Java version (17, 21, 22, or 23)
7. **Dependencies**: Comma-separated Spring Boot starters
8. **Old Package**: Package name to refactor from
9. **New Package**: Package name to refactor to

## What Gets Refactored

### Java Files (.java)
- Package declarations
- Import statements
- Fully qualified class references

### Properties Files (.properties, .yml, .yaml)
- Package name references
- Package path references (com/old/package)

### XML Files (.xml)
- Package name references in attributes
- Package path references

## Workflow

1. **Parse Manifest**: Read and validate the manifest file
2. **Collect Configuration**: Interactive prompts for project settings
3. **Validate**: Dry-run validation of manifest and repository
4. **Download Skeleton**: Fetch Spring Boot project from Initializr
5. **Harvest Files**: Extract files from Git history
6. **Refactor**: Update package names in all files
7. **Write Files**: Save refactored files to target directory

## Error Handling

If an error occurs during execution:
1. Progress is halted at the failing step
2. All changes are automatically rolled back
3. Error details are displayed with suggestions
4. The filesystem is left in its original state

## Examples

### Create a New Microservice

```bash
# Create manifest
cat > my-service-manifest.yaml << 'EOF'
sourceRepository: /path/to/monolith
files:
  - commit: abc1234
    sourcePath: src/main/java/com/company/user/UserService.java
    destinationPath: src/main/java/com/company/userservice/service/UserService.java
EOF

# Run bootstrapper
./smartbootstrapper my-service-manifest.yaml -o ./new-user-service
```

### Validate Before Running

```bash
./smartbootstrapper manifest.yaml --dry-run --verbose
```

## Troubleshooting

### Common Issues

**"Manifest file not found"**
- Verify the manifest file path is correct
- Ensure the file has `.yaml` or `.yml` extension

**"Git repository not found"**
- Check the `sourceRepository` path in the manifest
- Ensure the path points to a valid Git repository

**"Commit not found"**
- Verify the commit hash exists in the repository
- Use full or abbreviated (7+ chars) commit hashes

**"File not found at commit"**
- Check the file path is relative (no leading `/`)
- Verify the file existed at the specified commit

**"Failed to parse Java file"**
- Ensure the Java file has valid syntax
- Check for encoding issues (UTF-8 expected)

## Development

### Running Tests

```bash
mvn test
```

### Building

```bash
mvn clean package
```

### Running Locally

```bash
java -jar target/smartbootstrapper-cli-1.0.0.jar manifest.yaml
```

## License

MIT License

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests
5. Submit a pull request
