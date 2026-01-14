# Application Configuration

The system uses Spring Boot's externalized configuration and supports multiple environments via YAML profiles and environment variables.

**Main Configuration File:** `src/main/resources/application.yaml`
```yaml
server:
  port: ${SERVER_PORT:8080}
spring:
  application:
    name: ${APPLICATION_NAME:upmatches}
```

**Test Configuration:** `src/test/resources/application-test.yaml`
```yaml
server:
  port: ${SERVER_PORT:0}  # Random port for tests
spring:
  application:
    name: ${APPLICATION_NAME:upmatches}
```

## Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `SERVER_PORT` | 8080 | Application server port |
| `APPLICATION_NAME` | upmatches | Spring application name |
| `GITHUB_USERNAME` | - | GitHub Packages authentication |
| `GITHUB_TOKEN` | - | GitHub Packages token |
| `SONAR_TOKEN` | - | SonarCloud authentication |
| `DOCKERHUB_USERNAME`| - | Docker Hub username |
| `DOCKERHUB_PASSWORD`| - | Docker Hub password |
| `GF_SECURITY_ADMIN_USER` | vernon | Grafana admin username |
| `GF_SECURITY_ADMIN_PASSWORD` | password | Grafana admin password |
| `K6_PROMETHEUS_RW_SERVER_URL` | http://prometheus:9090/api/v1/write | k6 Prometheus remote write URL |
| `LOKI_ENABLED` | false | Enable Loki logging |
| `LOKI_URL` | http://localhost:3100/loki/api/v1/push | Loki push API URL |
| `LOKI_BATCH_SIZE` | 1000 | Loki log batch size |
| `LOKI_BATCH_TIMEOUT` | 5000 | Loki log batch timeout (ms) |
| `TRACING_ENABLED` | false | Enable distributed tracing |
| `TRACING_SAMPLING_PROBABILITY` | 1.0 | Tracing sampling probability (0.0 to 1.0) |
| `TRACING_OTLP_ENDPOINT` | http://localhost:4317 | OTLP gRPC endpoint for tracing |

## Profiles

- **default**: Standard configuration for development.
- **test**: Activated during test execution using `@ActiveProfiles("test")`.

## Feature Configuration

### Logging Aspect
A configuration namespace `app.logging.aspect` manages Aspect-Oriented Programming (AOP) behavior for logging.

**Location:** `src/main/resources/application.yaml`
```yaml
app:
  logging:
    aspect:
      enabled: true               # Master switch for AOP logging
      slow-execution-threshold-ms: 1000  # Threshold for logging slow method executions
```

### Loki Logging
Configuration for sending logs directly to Grafana Loki.

**Location:** `src/main/resources/application.yaml`
```yaml
app:
  logging:
    loki:
      enabled: false
      url: http://localhost:3100/loki/api/v1/push
      batch-size: 1000
```

### Distributed Tracing
Configuration for OTLP-based distributed tracing.

**Location:** `src/main/resources/application.yaml`
```yaml
app:
  tracing:
    enabled: false
    sampling-probability: 1.0
management:
  otlp:
    tracing:
      endpoint: http://localhost:4317
```

**Test Environment:** `src/test/resources/application-test.yaml`
```yaml
app:
  logging:
    aspect:
      enabled: false  # AOP logging is disabled in test environment
```

### Security & Actuator
With the addition of `spring-boot-starter-security` and `spring-boot-starter-actuator`, the application has security enabled by default and exposes management endpoints. 

**Note:** Further configuration is required to define specific security rules and to expose additional actuator endpoints (like `/actuator/prometheus`).

### Test Context Isolation
The base application test imports a test-specific configuration (`MetricsConfigurationTest`) to ensure a clean context for unit and integration tests, isolating them from production metric beans.

```java
@SpringBootTest
@ActiveProfiles("test")
@Import(MetricsConfigurationTest.class) // Isolates test context
class UpmatchesApplicationTests {
    // ...
}
```