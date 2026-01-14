package com.smartbootstrapper.cli;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.exception.SmartBootstrapperException;
import com.smartbootstrapper.manifest.ManifestParser;
import com.smartbootstrapper.manifest.ManifestValidator;
import com.smartbootstrapper.model.BootstrapResult;
import com.smartbootstrapper.model.Manifest;
import com.smartbootstrapper.model.ProjectConfiguration;
import com.smartbootstrapper.model.ValidationResult;
import com.smartbootstrapper.orchestrator.BootstrapOrchestrator;
import com.smartbootstrapper.orchestrator.ErrorHandler;
import com.smartbootstrapper.validation.DryRunValidator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Main CLI command for SmartBootstrapper.
 */
@Command(
        name = "smartbootstrapper",
        mixinStandardHelpOptions = true,
        version = "SmartBootstrapper CLI 1.0.0",
        description = "Automates Spring Boot project setup through manifest-driven file harvesting from Git history with package refactoring.",
        subcommands = {InitCommand.class}
)
public class SmartBootstrapperCommand implements Callable<Integer> {

    @Parameters(
            index = "0",
            description = "Path to the manifest YAML file",
            paramLabel = "<manifest>"
    )
    private Path manifestPath;

    @Option(
            names = {"-o", "--output"},
            description = "Target directory for the new project (defaults to current directory)",
            defaultValue = "."
    )
    private Path outputDirectory;

    @Option(
            names = {"--no-color"},
            description = "Disable colored output"
    )
    private boolean noColor;

    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose output"
    )
    private boolean verbose;

    @Option(
            names = {"--dry-run"},
            description = "Validate without executing (dry run mode)"
    )
    private boolean dryRun;

    @Option(
            names = {"-y", "--yes"},
            description = "Skip confirmation prompt and proceed with bootstrap"
    )
    private boolean skipConfirmation;

    // Non-interactive configuration options
    @Option(
            names = {"--group-id"},
            description = "Maven Group ID (e.g., com.example)"
    )
    private String groupId;

    @Option(
            names = {"--artifact-id"},
            description = "Maven Artifact ID (e.g., my-service)"
    )
    private String artifactId;

    @Option(
            names = {"--spring-boot-version"},
            description = "Spring Boot version (e.g., 4.0.0)",
            defaultValue = "4.0.0"
    )
    private String springBootVersion;

    @Option(
            names = {"--java-version"},
            description = "Java version (17, 21, 22, or 23)",
            defaultValue = "21"
    )
    private String javaVersion;

    @Option(
            names = {"--old-package"},
            description = "Source package name to refactor FROM (auto-detected from manifest if not provided)"
    )
    private String oldPackage;

    @Option(
            names = {"--new-package"},
            description = "Target package name to refactor TO (e.g., com.example.myapp)"
    )
    private String newPackage;

    @Option(
            names = {"--dependencies"},
            description = "Comma-separated list of Spring Boot dependencies (e.g., web,data-jpa,actuator)",
            defaultValue = "web"
    )
    private String dependencies;

    @Option(
            names = {"-p", "--from-project"},
            description = "Path to an existing project to read configuration defaults from (pom.xml)"
    )
    private Path fromProjectPath;

    private final PrintWriter out;
    private final PrintWriter err;

    public SmartBootstrapperCommand() {
        this(new PrintWriter(System.out, true), new PrintWriter(System.err, true));
    }

    public SmartBootstrapperCommand(PrintWriter out, PrintWriter err) {
        this.out = out;
        this.err = err;
    }

    @Override
    public Integer call() {
        boolean useColors = !noColor && System.console() != null;
        ErrorHandler errorHandler = new ErrorHandler(useColors, verbose);
        ConfigurationDisplay display = new ConfigurationDisplay(useColors);

        try {
            // Step 1: Validate manifest file exists
            validateManifestFile();

            // Step 2: Parse all manifests (auto-discovers commit-*.yaml files)
            ManifestParser parser = new ManifestParser();
            List<Manifest> manifests = parser.parseAllManifests(manifestPath);

            if (verbose) {
                out.println("Discovered " + manifests.size() + " manifest(s)");
                for (Manifest m : manifests) {
                    out.println("  - Manifest with " + m.size() + " entries" +
                        (m.getSequenceNumber() != null ? " (sequence " + m.getSequenceNumber() + ")" : ""));
                }
            }

            // Step 3: Collect user configuration (interactive or from CLI options)
            String targetDir = outputDirectory.toAbsolutePath().toString();
            ProjectConfiguration config;

            // Use first manifest for configuration (old package detection, etc.)
            Manifest firstManifest = manifests.get(0);

            if (hasNonInteractiveConfig()) {
                // Use command-line options (with auto-detection of old-package if needed)
                config = buildConfigFromOptions(targetDir, firstManifest);
                if (verbose) {
                    out.println("Using non-interactive configuration from command-line options");
                }
            } else {
                // Interactive mode
                InteractivePrompter prompter = new InteractivePrompter(useColors);

                // Load defaults from existing project if --from-project is specified
                ProjectConfigurationReader configReader = null;
                if (fromProjectPath != null) {
                    if (verbose) {
                        out.println("Loading defaults from project: " + fromProjectPath);
                    }
                    configReader = new ProjectConfigurationReader(fromProjectPath);
                    configReader.load();
                }

                config = prompter.promptForConfiguration(targetDir, configReader);
            }

            // Step 4: Display configuration summary
            out.print(display.displaySummary(config));

            // Step 5: Run dry-run validation for all manifests
            out.println("\nValidating configuration...\n");
            DryRunValidator validator = new DryRunValidator();
            boolean allValid = true;
            int totalErrors = 0;

            for (Manifest manifest : manifests) {
                ValidationResult validationResult = validator.validateAll(
                        manifest,
                        config.getOldPackage(),
                        config.getNewPackage()
                );

                // Step 6: Display validation results
                out.print(display.displayValidationResults(validationResult));
                out.flush();

                if (!validationResult.isSuccess()) {
                    allValid = false;
                    totalErrors += validationResult.getErrorCount();
                }
            }

            if (!allValid) {
                err.println(errorHandler.formatError(
                        new SmartBootstrapperException("Validation failed with " + totalErrors + " error(s)")
                ));
                return 2;
            }

            out.println("All " + manifests.size() + " manifests validated successfully.\n");

            // If dry-run mode, exit here
            if (dryRun) {
                out.println("\nDry-run mode: Validation passed. No changes were made.");
                return 0;
            }

            // Step 7: Prompt for confirmation (skip if --yes flag or non-interactive mode)
            if (skipConfirmation || hasNonInteractiveConfig()) {
                out.println("\nProceeding with bootstrap...\n");
            } else {
                out.print(display.displayConfirmationPrompt());
                InteractivePrompter prompter = new InteractivePrompter(useColors);
                if (!prompter.promptConfirmation("")) {
                    out.println("\nOperation cancelled by user.");
                    return 0;
                }
            }

            // Step 8: Execute bootstrap with all manifests
            BootstrapOrchestrator orchestrator = new BootstrapOrchestrator(out, useColors);
            BootstrapResult result = orchestrator.executeMultipleManifests(manifests, config);

            // Step 9: Display result
            if (result.isSuccess()) {
                out.print(display.displaySuccessSummary(result));
                return 0;
            } else {
                out.print(display.displayErrorSummary(result));
                return 1;
            }

        } catch (Exception e) {
            err.println(errorHandler.formatError(e));
            return errorHandler.getExitCode(e);
        }
    }

    private void validateManifestFile() {
        if (manifestPath == null) {
            throw new SmartBootstrapperException("Manifest file path is required");
        }

        if (!Files.exists(manifestPath)) {
            throw new SmartBootstrapperException(
                    String.format(Constants.ERROR_MANIFEST_NOT_FOUND, manifestPath)
            );
        }

        if (!Files.isRegularFile(manifestPath)) {
            throw new SmartBootstrapperException("Manifest path is not a file: " + manifestPath);
        }

        if (!Files.isReadable(manifestPath)) {
            throw new SmartBootstrapperException("Manifest file is not readable: " + manifestPath);
        }

        // Validate extension
        String fileName = manifestPath.getFileName().toString();
        if (!fileName.endsWith(".yaml") && !fileName.endsWith(".yml")) {
            out.println(Constants.ANSI_YELLOW + "Warning: Manifest file does not have .yaml or .yml extension" +
                    Constants.ANSI_RESET);
        }
    }

    /**
     * Check if non-interactive configuration options are provided.
     * Note: --old-package is optional as it can be auto-detected from manifest.
     */
    private boolean hasNonInteractiveConfig() {
        return groupId != null && artifactId != null && newPackage != null;
    }

    /**
     * Build ProjectConfiguration from command-line options.
     * Auto-detects --old-package from manifest if not provided.
     */
    private ProjectConfiguration buildConfigFromOptions(String targetDir, Manifest manifest) {
        if (groupId == null || groupId.isBlank()) {
            throw new SmartBootstrapperException("--group-id is required for non-interactive mode");
        }
        if (artifactId == null || artifactId.isBlank()) {
            throw new SmartBootstrapperException("--artifact-id is required for non-interactive mode");
        }
        if (newPackage == null || newPackage.isBlank()) {
            throw new SmartBootstrapperException("--new-package is required for non-interactive mode");
        }

        // Auto-detect old package from manifest if not provided
        String effectiveOldPackage = oldPackage;
        if (effectiveOldPackage == null || effectiveOldPackage.isBlank()) {
            effectiveOldPackage = manifest.detectBasePackage()
                    .orElseThrow(() -> new SmartBootstrapperException(
                            "Could not auto-detect --old-package from manifest. " +
                            "Please specify it explicitly using --old-package <package.name>"
                    ));
            out.println(Constants.ANSI_CYAN + "Auto-detected old package: " + effectiveOldPackage + Constants.ANSI_RESET);
        }

        // Parse dependencies
        java.util.List<String> depsList = java.util.Arrays.asList(
                dependencies != null ? dependencies.split(",") : new String[]{"web"}
        );

        return new ProjectConfiguration.Builder()
                .groupId(groupId)
                .artifactId(artifactId)
                .projectName(artifactId) // Use artifact ID as name
                .version("0.0.1-SNAPSHOT")
                .springBootVersion(springBootVersion)
                .javaVersion(javaVersion)
                .dependencies(depsList)
                .targetDirectory(targetDir)
                .oldPackage(effectiveOldPackage)
                .newPackage(newPackage)
                .build();
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new SmartBootstrapperCommand())
                .setUsageHelpAutoWidth(true)
                .execute(args);
        System.exit(exitCode);
    }
}
