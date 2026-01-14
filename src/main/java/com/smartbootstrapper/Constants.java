package com.smartbootstrapper;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Application constants and configuration values.
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // ========== Spring Initializr Configuration ==========
    public static final String SPRING_INITIALIZR_URL = "https://start.spring.io";
    public static final String SPRING_INITIALIZR_STARTER_ZIP = SPRING_INITIALIZR_URL + "/starter.zip";

    // ========== Timeout Configuration (milliseconds) ==========
    public static final int HTTP_CONNECT_TIMEOUT = 30_000;
    public static final int HTTP_READ_TIMEOUT = 60_000;
    public static final int HTTP_WRITE_TIMEOUT = 30_000;

    // ========== ANSI Color Codes ==========
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    // ========== Unicode Symbols ==========
    public static final String SYMBOL_CHECK = "\u2713";  // ✓
    public static final String SYMBOL_CROSS = "\u2717";  // ✗
    public static final String SYMBOL_ARROW = "\u2192";  // →
    public static final String SYMBOL_BULLET = "\u2022"; // •
    public static final String SYMBOL_WARNING = "\u26A0"; // ⚠
    public static final String SYMBOL_CHECKBOX_EMPTY = "\u2610"; // ☐
    public static final String SYMBOL_CHECKBOX_CHECKED = "\u2611"; // ☑
    public static final String SYMBOL_RADIO_EMPTY = "\u25CB"; // ○
    public static final String SYMBOL_RADIO_SELECTED = "\u25CF"; // ●
    public static final String SYMBOL_POINTER = "\u25B6"; // ▶

    // ========== Validation Patterns ==========
    public static final Pattern GROUP_ID_PATTERN = Pattern.compile(
            "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$"
    );
    public static final Pattern ARTIFACT_ID_PATTERN = Pattern.compile(
            "^[a-z][a-z0-9]*(-[a-z][a-z0-9]*)*$"
    );
    public static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile(
            "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$"
    );
    public static final Pattern VERSION_PATTERN = Pattern.compile(
            "^\\d+\\.\\d+\\.\\d+(-[A-Za-z0-9]+)?(-SNAPSHOT)?$"
    );
    public static final Pattern SPRING_BOOT_VERSION_PATTERN = Pattern.compile(
            "^\\d+\\.\\d+\\.\\d+$"
    );
    public static final Pattern JAVA_VERSION_PATTERN = Pattern.compile(
            "^(17|21|25)$"
    );
    public static final Pattern GIT_COMMIT_HASH_PATTERN = Pattern.compile(
            "^[a-fA-F0-9]{7,40}$"
    );
    public static final Pattern CONVENTIONAL_COMMIT_PATTERN = Pattern.compile(
            "^(build|chore|ci|docs|feat|fix|perf|refactor|revert|style|test)(\\([a-z0-9-]+\\))?!?: .+"
    );

    // ========== Default Values ==========
    public static final String DEFAULT_JAVA_VERSION = "21";
    public static final String DEFAULT_PROJECT_VERSION = "0.0.1-SNAPSHOT";
    public static final String DEFAULT_PACKAGING = "jar";

    // ========== Available Java Versions ==========
    public static final List<String> SUPPORTED_JAVA_VERSIONS = List.of("17", "21", "25");

    // ========== Available Spring Boot Versions ==========
    public static final List<String> SUPPORTED_SPRING_BOOT_VERSIONS = List.of("4.0.1", "3.5.9");
    public static final String DEFAULT_SPRING_BOOT_VERSION = "4.0.1";

    // ========== All Available Spring Boot Dependencies (from start.spring.io) ==========

    // Developer Tools
    public static final List<String> DEPS_DEVELOPER_TOOLS = List.of(
            "native", "dgs-codegen", "devtools", "lombok", "configuration-processor", "docker-compose", "modulith"
    );

    // Web
    public static final List<String> DEPS_WEB = List.of(
            "web", "webflux", "spring-restclient", "spring-webclient", "graphql", "data-rest",
            "session-data-mongodb", "session-data-redis", "session-hazelcast", "session-jdbc",
            "data-rest-explorer", "hateoas", "web-services", "jersey", "vaadin", "netflix-dgs", "htmx"
    );

    // Template Engines
    public static final List<String> DEPS_TEMPLATE_ENGINES = List.of(
            "thymeleaf", "freemarker", "mustache", "groovy-templates", "jte"
    );

    // Security
    public static final List<String> DEPS_SECURITY = List.of(
            "security", "oauth2-client", "oauth2-authorization-server", "oauth2-resource-server",
            "spring-security-webauthn", "ldap", "data-ldap"
    );

    // SQL
    public static final List<String> DEPS_SQL = List.of(
            "jdbc", "r2dbc", "data-jpa", "data-jdbc", "data-r2dbc", "mybatis", "liquibase", "flyway", "jooq",
            "db2", "derby", "h2", "hsql", "mariadb", "sqlserver", "mysql", "oracle", "postgresql"
    );

    // NoSQL
    public static final List<String> DEPS_NOSQL = List.of(
            "data-redis", "data-redis-reactive", "mongodb", "data-mongodb", "data-mongodb-reactive",
            "elasticsearch", "data-elasticsearch", "cassandra", "data-cassandra", "data-cassandra-reactive",
            "couchbase", "data-couchbase", "data-couchbase-reactive", "neo4j", "data-neo4j"
    );

    // Messaging
    public static final List<String> DEPS_MESSAGING = List.of(
            "integration", "amqp", "amqp-streams", "kafka", "kafka-streams", "activemq", "artemis",
            "pulsar", "pulsar-reactive", "websocket", "rsocket", "camel", "solace"
    );

    // I/O
    public static final List<String> DEPS_IO = List.of(
            "batch", "batch-jdbc", "hazelcast", "validation", "mail", "quartz", "cache", "spring-shell", "spring-grpc"
    );

    // Ops
    public static final List<String> DEPS_OPS = List.of(
            "actuator", "sbom-cyclone-dx", "codecentric-spring-boot-admin-client",
            "codecentric-spring-boot-admin-server", "sentry"
    );

    // Observability
    public static final List<String> DEPS_OBSERVABILITY = List.of(
            "datadog", "dynatrace", "influx", "graphite", "new-relic", "otlp-metrics", "prometheus",
            "datasource-micrometer", "distributed-tracing", "opentelemetry", "wavefront", "zipkin"
    );

    // Testing
    public static final List<String> DEPS_TESTING = List.of(
            "restdocs", "testcontainers", "cloud-contract-verifier", "cloud-contract-stub-runner", "unboundid-ldap"
    );

    // Spring Cloud
    public static final List<String> DEPS_SPRING_CLOUD = List.of(
            "cloud-starter", "cloud-function", "cloud-task"
    );

    // Spring Cloud Config
    public static final List<String> DEPS_SPRING_CLOUD_CONFIG = List.of(
            "cloud-config-client", "cloud-config-server", "cloud-starter-vault-config",
            "cloud-starter-zookeeper-config", "cloud-starter-consul-config"
    );

    // Spring Cloud Discovery
    public static final List<String> DEPS_SPRING_CLOUD_DISCOVERY = List.of(
            "cloud-eureka", "cloud-eureka-server", "cloud-starter-zookeeper-discovery", "cloud-starter-consul-discovery"
    );

    // Spring Cloud Routing
    public static final List<String> DEPS_SPRING_CLOUD_ROUTING = List.of(
            "cloud-gateway", "cloud-gateway-reactive", "cloud-feign", "cloud-loadbalancer"
    );

    // Spring Cloud Circuit Breaker
    public static final List<String> DEPS_SPRING_CLOUD_CIRCUIT_BREAKER = List.of(
            "cloud-resilience4j"
    );

    // Spring Cloud Messaging
    public static final List<String> DEPS_SPRING_CLOUD_MESSAGING = List.of(
            "cloud-bus", "cloud-stream"
    );

    // VMware Tanzu Application Service
    public static final List<String> DEPS_TANZU_APPLICATION_SERVICE = List.of(
            "scs-config-client", "scs-service-registry"
    );

    // VMware Tanzu Spring Enterprise Extensions
    public static final List<String> DEPS_TANZU_ENTERPRISE = List.of(
            "tanzu-governance-starter", "tanzu-scg-access-control", "tanzu-scg-custom",
            "tanzu-scg-graphql", "tanzu-scg-sso", "tanzu-scg-traffic-control", "tanzu-scg-transformation"
    );

    // Microsoft Azure
    public static final List<String> DEPS_AZURE = List.of(
            "azure-support", "azure-active-directory", "azure-cosmos-db", "azure-keyvault", "azure-storage"
    );

    // Google Cloud
    public static final List<String> DEPS_GOOGLE_CLOUD = List.of(
            "cloud-gcp", "cloud-gcp-pubsub", "cloud-gcp-storage"
    );

    // AI
    public static final List<String> DEPS_AI = List.of(
            "spring-ai-anthropic", "spring-ai-azure-openai", "spring-ai-vectordb-azure", "spring-ai-bedrock",
            "spring-ai-bedrock-converse", "spring-ai-deepseek", "spring-ai-elevenlabs", "spring-ai-google-genai",
            "spring-ai-google-genai-embedding", "spring-ai-huggingface", "spring-ai-minimax", "spring-ai-oci-genai",
            "spring-ai-zhipuai", "spring-ai-vectordb-cassandra", "spring-ai-vectordb-chroma",
            "spring-ai-vectordb-couchbase", "spring-ai-vectordb-elasticsearch", "spring-ai-vectordb-gemfire",
            "spring-ai-mcp-server", "spring-ai-mcp-client", "spring-ai-vectordb-milvus", "spring-ai-mistral",
            "spring-ai-vectordb-mongodb-atlas", "spring-ai-vectordb-neo4j", "spring-ai-vectordb-opensearch",
            "spring-ai-vectordb-aws-opensearch", "spring-ai-ollama", "spring-ai-openai",
            "spring-ai-chat-memory-repository-in-memory", "spring-ai-chat-memory-repository-jdbc",
            "spring-ai-chat-memory-repository-cassandra", "spring-ai-chat-memory-repository-mongodb",
            "spring-ai-chat-memory-repository-neo4j", "spring-ai-vectordb-oracle", "spring-ai-vectordb-pgvector",
            "spring-ai-vectordb-pinecone", "spring-ai-postgresml", "spring-ai-vectordb-redis",
            "spring-ai-vectordb-mariadb", "spring-ai-vectordb-azurecosmosdb", "spring-ai-stabilityai",
            "spring-ai-transformers", "spring-ai-vertexai-gemini", "spring-ai-vertexai-embeddings",
            "spring-ai-vectordb-qdrant", "spring-ai-vectordb-typesense", "spring-ai-vectordb-weaviate",
            "spring-ai-markdown-document-reader", "spring-ai-tika-document-reader", "spring-ai-pdf-document-reader",
            "timefold-solver"
    );

    // All dependencies combined (for validation)
    public static final List<String> ALL_DEPENDENCIES = new java.util.ArrayList<>();
    static {
        ALL_DEPENDENCIES.addAll(DEPS_DEVELOPER_TOOLS);
        ALL_DEPENDENCIES.addAll(DEPS_WEB);
        ALL_DEPENDENCIES.addAll(DEPS_TEMPLATE_ENGINES);
        ALL_DEPENDENCIES.addAll(DEPS_SECURITY);
        ALL_DEPENDENCIES.addAll(DEPS_SQL);
        ALL_DEPENDENCIES.addAll(DEPS_NOSQL);
        ALL_DEPENDENCIES.addAll(DEPS_MESSAGING);
        ALL_DEPENDENCIES.addAll(DEPS_IO);
        ALL_DEPENDENCIES.addAll(DEPS_OPS);
        ALL_DEPENDENCIES.addAll(DEPS_OBSERVABILITY);
        ALL_DEPENDENCIES.addAll(DEPS_TESTING);
        ALL_DEPENDENCIES.addAll(DEPS_SPRING_CLOUD);
        ALL_DEPENDENCIES.addAll(DEPS_SPRING_CLOUD_CONFIG);
        ALL_DEPENDENCIES.addAll(DEPS_SPRING_CLOUD_DISCOVERY);
        ALL_DEPENDENCIES.addAll(DEPS_SPRING_CLOUD_ROUTING);
        ALL_DEPENDENCIES.addAll(DEPS_SPRING_CLOUD_CIRCUIT_BREAKER);
        ALL_DEPENDENCIES.addAll(DEPS_SPRING_CLOUD_MESSAGING);
        ALL_DEPENDENCIES.addAll(DEPS_TANZU_APPLICATION_SERVICE);
        ALL_DEPENDENCIES.addAll(DEPS_TANZU_ENTERPRISE);
        ALL_DEPENDENCIES.addAll(DEPS_AZURE);
        ALL_DEPENDENCIES.addAll(DEPS_GOOGLE_CLOUD);
        ALL_DEPENDENCIES.addAll(DEPS_AI);
    }

    // Dependency categories for display
    public static final java.util.Map<String, List<String>> DEPENDENCY_CATEGORIES = new java.util.LinkedHashMap<>();
    static {
        DEPENDENCY_CATEGORIES.put("Developer Tools", DEPS_DEVELOPER_TOOLS);
        DEPENDENCY_CATEGORIES.put("Web", DEPS_WEB);
        DEPENDENCY_CATEGORIES.put("Template Engines", DEPS_TEMPLATE_ENGINES);
        DEPENDENCY_CATEGORIES.put("Security", DEPS_SECURITY);
        DEPENDENCY_CATEGORIES.put("SQL", DEPS_SQL);
        DEPENDENCY_CATEGORIES.put("NoSQL", DEPS_NOSQL);
        DEPENDENCY_CATEGORIES.put("Messaging", DEPS_MESSAGING);
        DEPENDENCY_CATEGORIES.put("I/O", DEPS_IO);
        DEPENDENCY_CATEGORIES.put("Ops", DEPS_OPS);
        DEPENDENCY_CATEGORIES.put("Observability", DEPS_OBSERVABILITY);
        DEPENDENCY_CATEGORIES.put("Testing", DEPS_TESTING);
        DEPENDENCY_CATEGORIES.put("Spring Cloud", DEPS_SPRING_CLOUD);
        DEPENDENCY_CATEGORIES.put("Spring Cloud Config", DEPS_SPRING_CLOUD_CONFIG);
        DEPENDENCY_CATEGORIES.put("Spring Cloud Discovery", DEPS_SPRING_CLOUD_DISCOVERY);
        DEPENDENCY_CATEGORIES.put("Spring Cloud Routing", DEPS_SPRING_CLOUD_ROUTING);
        DEPENDENCY_CATEGORIES.put("Spring Cloud Circuit Breaker", DEPS_SPRING_CLOUD_CIRCUIT_BREAKER);
        DEPENDENCY_CATEGORIES.put("Spring Cloud Messaging", DEPS_SPRING_CLOUD_MESSAGING);
        DEPENDENCY_CATEGORIES.put("VMware Tanzu Application Service", DEPS_TANZU_APPLICATION_SERVICE);
        DEPENDENCY_CATEGORIES.put("VMware Tanzu Enterprise", DEPS_TANZU_ENTERPRISE);
        DEPENDENCY_CATEGORIES.put("Microsoft Azure", DEPS_AZURE);
        DEPENDENCY_CATEGORIES.put("Google Cloud", DEPS_GOOGLE_CLOUD);
        DEPENDENCY_CATEGORIES.put("AI", DEPS_AI);
    }

    // ========== Error Messages ==========
    public static final String ERROR_MANIFEST_NOT_FOUND = "Manifest file not found: %s";
    public static final String ERROR_MANIFEST_PARSE = "Failed to parse manifest file: %s";
    public static final String ERROR_MANIFEST_EMPTY = "Manifest file is empty or contains no entries";
    public static final String ERROR_MANIFEST_INVALID_FORMAT = "Invalid manifest format at line %d: %s";
    public static final String ERROR_GIT_REPO_NOT_FOUND = "Git repository not found at: %s";
    public static final String ERROR_GIT_COMMIT_NOT_FOUND = "Commit not found: %s";
    public static final String ERROR_GIT_FILE_NOT_FOUND = "File not found at commit %s: %s";
    public static final String ERROR_REFACTOR_PARSE = "Failed to parse Java file: %s";
    public static final String ERROR_INITIALIZR_DOWNLOAD = "Failed to download from Spring Initializr";
    public static final String ERROR_INITIALIZR_NETWORK = "Network error connecting to Spring Initializr: %s";
    public static final String ERROR_VALIDATION_FAILED = "Validation failed with %d error(s)";
    public static final String ERROR_ROLLBACK = "Error during rollback: %s";

    // ========== Success Messages ==========
    public static final String SUCCESS_BOOTSTRAP = "Project successfully bootstrapped at: %s";
    public static final String SUCCESS_VALIDATION = "All validation checks passed";
    public static final String SUCCESS_HARVEST = "Successfully harvested %d file(s)";
    public static final String SUCCESS_REFACTOR = "Successfully refactored %d file(s)";

    // ========== Progress Phase Descriptions ==========
    public static final String PHASE_DOWNLOAD_SKELETON = "Downloading Spring Boot skeleton";
    public static final String PHASE_VALIDATE = "Validating manifest and repository";
    public static final String PHASE_HARVEST = "Harvesting files from Git history";
    public static final String PHASE_REFACTOR = "Refactoring and writing files";
    public static final String PHASE_COMMIT = "Creating commit from COMMIT.md";

    // ========== Git Commit Messages ==========
    public static final String INITIAL_COMMIT_MESSAGE = "chore(init): scaffold project from Spring Initializr";

    // ========== File Extensions ==========
    public static final String EXT_JAVA = ".java";
    public static final String EXT_PROPERTIES = ".properties";
    public static final String EXT_YAML = ".yaml";
    public static final String EXT_YML = ".yml";
    public static final String EXT_XML = ".xml";

    // ========== Manifest Field Names ==========
    public static final String MANIFEST_FIELD_SOURCE_REPOSITORY = "sourceRepository";
    public static final String MANIFEST_FIELD_SOURCE_TYPE = "sourceType";
    public static final String MANIFEST_FIELD_SEQUENCE_NUMBER = "sequenceNumber";
    public static final String MANIFEST_FIELD_FILES = "files";
    public static final String MANIFEST_FIELD_COMMIT = "commit";
    public static final String MANIFEST_FIELD_SOURCE_PATH = "sourcePath";
    public static final String MANIFEST_FIELD_DESTINATION_PATH = "destinationPath";
    public static final String MANIFEST_FIELD_TARGET_DIRECTORY = "targetDirectory";

    // ========== Source Types ==========
    public static final String SOURCE_TYPE_GIT = "git";
    public static final String SOURCE_TYPE_ZIP = "zip";
}
