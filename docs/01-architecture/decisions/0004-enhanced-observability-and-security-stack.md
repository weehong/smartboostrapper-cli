# ADR-004: Enhanced Observability and Security Stack

**Date:** 2024-01-15  
**Status:** Accepted  

## Context
The application required improved operational visibility, security, and structured logging to meet production readiness standards. The initial setup lacked comprehensive monitoring, security layers, and testable logging.

## Decision
Integrate a suite of Spring Boot starters and libraries to establish a robust foundation for observability, security, and aspect-oriented programming (AOP).

### Changes
1.  **Dependencies Added (`pom.xml`):**
    *   **Security:** `spring-boot-starter-security`
    *   **Observability:** `spring-boot-starter-actuator` + `micrometer-registry-prometheus`
    *   **AOP & Logging:** `spring-boot-starter-aspectj`, `spring-boot-starter-logging`
    *   **Testing:** `spring-boot-starter-security-test`, `slf4j-test`

2.  **Configuration Properties Scan:** Annotated the main `UpmatchesApplication` class with `@ConfigurationPropertiesScan` to enable binding of external configuration to `@ConfigurationProperties` classes.

3.  **Environment Configuration:** Added application properties (`app.logging.aspect`) to control AOP-based logging behavior, with separate settings for test profiles to avoid interference in unit tests.

4.  **Test Configuration:** Updated the main application test to import a test-specific metrics configuration, ensuring test isolation.

## Consequences
### Positive
*   **Security:** Built-in authentication and authorization framework is now available.
*   **Monitoring:** Actuator endpoints provide health checks, metrics, and environment details. Integration with Prometheus enables time-series monitoring.
*   **Structured Logging:** AOP support allows for cross-cutting concerns like performance logging and audit trails.
*   **Testability:** Dedicated test dependencies allow for verifying security configurations and capturing log outputs in tests.

### Negative
*   **Increased Complexity:** More dependencies to manage and understand.
*   **Performance Overhead:** AOP and security filters introduce minimal runtime overhead.
*   **Configuration:** Requires additional configuration for security rules and actuator endpoint exposure.

## Compliance
This aligns with the project's quality gates, as evidenced by the updated JaCoCo configuration (excluding constants) and Checkstyle suppressions file.
