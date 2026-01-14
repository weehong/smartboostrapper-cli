# syntax=docker/dockerfile:1

#-------------------------------------------------------------------------------#
#                            Extraction Stage                                   #
#-------------------------------------------------------------------------------#
FROM amazoncorretto:21-alpine AS extractor

WORKDIR /extract

# Expect the jar to be in target/ folder relative to build context
# The CI pipeline passes 'target/' as artifact.
# Local build requires 'mvn package' first.
COPY target/*.jar app.jar

RUN java -Djarmode=layertools -jar app.jar extract

#-------------------------------------------------------------------------------#
#                             Runtime Stage                                     #
#-------------------------------------------------------------------------------#
FROM amazoncorretto:21-alpine

LABEL maintainer="vernonweehongkoh.developer@outlook.com" \
      version="1.0" \
      description="Spring Boot - Upmatches Application"

# Create non-root user
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -s /bin/sh -D appuser

WORKDIR /app

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Copy Spring Boot layers
COPY --from=extractor --chown=appuser:appgroup /extract/dependencies/ ./
COPY --from=extractor --chown=appuser:appgroup /extract/spring-boot-loader/ ./
COPY --from=extractor --chown=appuser:appgroup /extract/snapshot-dependencies/ ./
COPY --from=extractor --chown=appuser:appgroup /extract/application/ ./

USER appuser

EXPOSE 8100

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -q --spider http://localhost:8100/actuator/health || exit 1

STOPSIGNAL SIGTERM

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]
