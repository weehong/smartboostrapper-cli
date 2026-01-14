# Development Environment Setup

This project uses a standard Java/Spring Boot development environment with Maven as the build tool.

### Prerequisites

- **Java 21** or higher
- **Maven** (automatically managed via Maven Wrapper)
- **GitHub Packages Credentials**: Required for downloading custom Checkstyle rules.
  - `GITHUB_USERNAME`
  - `GITHUB_TOKEN`

### Project Structure
... (existing structure)

### Build System

The project uses **Maven Wrapper** (version 3.3.4) which ensures consistent builds across different environments.

#### Repository Configuration

To download dependencies from GitHub Packages, the following repository is configured in `pom.xml`:

```xml
<repository>
    <id>github</id>
    <name>GitHub Packages</name>
    <url>https://maven.pkg.github.com/weehong/checkstyle-rule</url>
</repository>
```

#### Build Commands

```bash
# Build and compile the project
./mvnw clean compile

# Run tests with coverage checks
./mvnw verify

# Run code style validation
./mvnw checkstyle:check

# Run the application locally
./mvnw spring-boot:run
```

### IDE Configuration

The project includes IDE-specific ignore patterns in `.gitignore` for:
- **IntelliJ IDEA** (`.idea/`, `*.iml`)
- **Eclipse/STS** (`.project`, `.classpath`, `.settings/`)
- **VS Code** (`.vscode/`)
- **NetBeans** (`nbproject/`, `build/`)

### Dependencies

The project's `pom.xml` includes a comprehensive set of production-grade Spring Boot starters and testing utilities.

#### Key Dependency Additions:
```xml
<!-- Core Starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aspectj</artifactId>
</dependency>

<!-- Metrics Export -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Enhanced Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.github.valfirst</groupId>
    <artifactId>slf4j-test</artifactId>
    <scope>test</scope>
</dependency>
```

#### Build Plugin Updates:
*   **JaCoCo:** Code coverage reporting now excludes Constant classes (`**/*Constant.class`) in addition to the main Application class.
*   **Checkstyle:** Now references a suppressions file (`configurations/checkstyle-suppressions.xml`) for granular rule control.

### Property Configuration Scan
The main application class now includes `@ConfigurationPropertiesScan`. This enables automatic detection and registration of all classes annotated with `@ConfigurationProperties`, allowing for type-safe binding of properties defined in `application.yaml`.

```java
@SpringBootApplication
@ConfigurationPropertiesScan // Enables external configuration binding
public class UpmatchesApplication {
    // ...
}
```

### Git Configuration

The `.gitattributes` file ensures consistent line endings:
- Unix line endings (`LF`) for shell scripts (`mvnw`)
- Windows line endings (`CRLF`) for batch files (`*.cmd`)

### Getting Started

1. Clone the repository
2. Ensure Java 21 is installed
3. Run `./mvnw spring-boot:run` to start the application
4. Access the application at `http://localhost:8080`

The Maven wrapper will automatically download the required Maven version (3.9.12) on first run if not already available in the local cache.
