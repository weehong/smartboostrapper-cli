1. d3d5969bd815e3b89a66182d7901719a5276e4a5

chore(init): bootstrap Spring Boot project
- add git configuration (.gitattributes, .gitignore)
- add maven wrapper (mvnw, mvnw.cmd) with version 3.3.4
- configure pom.xml with spring boot 4.0.1 and java 21
- create main application class and test
- configure application properties with app name
- add development environment setup documentation

---
2. ef5eb38b3d3385110a8f6802cfed07324699b071

ci: add gitlab pipeline and deployment configuration
- add gitlab-ci.yaml with build, test, analysis, publish stages
- add qodana and sonarcloud quality analysis configuration
- add docker deployment (Dockerfile, compose, docker.sh)
- add environment file templates (.env.compose, .env.docker)
- migrate application.properties to yaml with test profile
- update pom.xml with jacoco and checkstyle plugins
- update .gitignore for qodana and sonar artifacts
- add project documentation structure

---
3. fc14b4123e206a888e42b863f64045e67286f96a

feat(logging): add aop-based logging with observability
- add logaspect for method execution monitoring and metrics
- add spring security, actuator, and micrometer dependencies
- add logback configuration with mdc context and file rotation
- add configuration properties for logging aspect threshold
- add checkstyle suppressions for test files
- update documentation for new observability stack
---
4. 4aab003b52219114dff2f8693a9613da000f409a

feat(database): add hikaricp datasource configuration
- add database configuration with hikaricp connection pooling
- add database property classes for spring configuration binding
- add database type enum with driver class resolution
- add database constants for default pool settings
- add postgresql, h2, and mysql driver support
- add comprehensive test coverage for database components
- add spring-data-jpa, lombok, and database driver dependencies
- update application yaml with datasource and jpa settings
---
5. e9a95d5467f59091d44ed26feca457bf2503d5f8

feat(core): add exception handling and service executor
- add global exception handler with problem detail responses
- add base exception and custom exception types
- add service operation executor with mdc tracing
- add operation status enum and executor constants
- add comprehensive test coverage for all components
- update pom.xml with required dependencies
---
6. 934851238e4fa373202e2cbbcc6f233f0b712ddc

feat(shared): add generic helpers and response wrappers
- add repositoryhelper with findbyidorthrow and findbyuuidorthrow
- add validationhelper for pagination, sorting, id and uuid validation
- add apiresponse record with success and error factory methods
- add pagedresponse record with page mapping support
- add comprehensive unit tests for all helpers and responses
---