# Testing Strategy

The project follows a test-driven approach with a focus on high code coverage and automated quality gates.

## Test Configuration

### Unit and Integration Tests
- **Framework**: JUnit 5 with Spring Boot Test.
- **Base Test Class**: `src/test/java/com/upmatches/app/UpmatchesApplicationTests.java`.
- **Annotation**: Uses `@SpringBootTest` and `@ActiveProfiles("test")`.

```java
@SpringBootTest
@ActiveProfiles("test")
class UpmatchesApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

### Coverage Thresholds
The project enforces a **minimum of 80% instruction coverage** verified by JaCoCo.

## JaCoCo Integration

JaCoCo is integrated into the Maven lifecycle to collect coverage data during test execution.

- **Reports Location**: `target/site/jacoco/jacoco.xml`
- **Exclusions**:
  - `*Application.java`
  - `*Constant.java`
  - Classes in `shared` package

## Execution Commands

```bash
# Run tests and generate coverage reports
./mvnw verify

# Run tests without coverage check
./mvnw test
```

## Quality Gates
The CI/CD pipeline validates:
1. All tests pass.
2. SonarCloud quality gate status is "OK".
3. Overall coverage is above 80%.