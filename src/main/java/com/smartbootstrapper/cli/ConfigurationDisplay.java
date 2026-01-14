package com.smartbootstrapper.cli;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.model.BootstrapResult;
import com.smartbootstrapper.model.ProjectConfiguration;
import com.smartbootstrapper.model.ValidationResult;
import com.smartbootstrapper.validation.ValidationResultFormatter;

/**
 * Displays configuration summaries and results.
 */
public class ConfigurationDisplay {

    private final boolean useColors;
    private final ValidationResultFormatter validationFormatter;

    public ConfigurationDisplay() {
        this(true);
    }

    public ConfigurationDisplay(boolean useColors) {
        this.useColors = useColors;
        this.validationFormatter = new ValidationResultFormatter(useColors);
    }

    /**
     * Displays a formatted configuration summary.
     *
     * @param config The project configuration
     * @return Formatted string
     */
    public String displaySummary(ProjectConfiguration config) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append(bold("=== Configuration Summary ==="));
        sb.append("\n\n");

        sb.append(formatRow("Group ID", config.getGroupId()));
        sb.append(formatRow("Artifact ID", config.getArtifactId()));
        sb.append(formatRow("Project Name", config.getProjectName()));
        sb.append(formatRow("Version", config.getVersion()));
        sb.append(formatRow("Spring Boot", config.getSpringBootVersion()));
        sb.append(formatRow("Java Version", config.getJavaVersion()));
        sb.append(formatRow("Dependencies", formatDependencies(config)));
        sb.append(formatRow("Old Package", config.getOldPackage()));
        sb.append(formatRow("New Package", config.getNewPackage()));
        sb.append(formatRow("Target Directory", config.getTargetDirectory()));

        sb.append("\n");

        return sb.toString();
    }

    /**
     * Displays validation results.
     *
     * @param result The validation result
     * @return Formatted string
     */
    public String displayValidationResults(ValidationResult result) {
        return validationFormatter.format(result);
    }

    /**
     * Displays the final success summary.
     *
     * @param result The bootstrap result
     * @return Formatted string
     */
    public String displaySuccessSummary(BootstrapResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append(colorGreen(bold("=== Bootstrap Complete ==="))).append("\n\n");

        sb.append(colorGreen(Constants.SYMBOL_CHECK)).append(" Project successfully created!\n\n");

        sb.append(bold("Summary:")).append("\n");
        sb.append(formatBullet("Location: " + result.getTargetDirectory()));

        if (result.getHarvestResult() != null) {
            sb.append(formatBullet("Files harvested: " + result.getHarvestResult().getSuccessfulFiles()));
        }

        if (result.getRefactorResult() != null) {
            sb.append(formatBullet("Files refactored: " + result.getRefactorResult().getTotalRefactored()));
            sb.append(formatBullet("  - Java files: " + result.getRefactorResult().getJavaFilesRefactored()));
            sb.append(formatBullet("  - Properties files: " + result.getRefactorResult().getPropertiesFilesRefactored()));
            sb.append(formatBullet("  - XML files: " + result.getRefactorResult().getXmlFilesRefactored()));
        }

        sb.append(formatBullet("Duration: " + result.getFormattedDuration()));

        sb.append("\n");
        sb.append(bold("Next steps:")).append("\n");
        sb.append(formatBullet("cd " + result.getTargetDirectory()));
        sb.append(formatBullet("./mvnw spring-boot:run"));

        sb.append("\n");

        return sb.toString();
    }

    /**
     * Displays an error summary.
     *
     * @param result The bootstrap result
     * @return Formatted string
     */
    public String displayErrorSummary(BootstrapResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append(colorRed(bold("=== Bootstrap Failed ==="))).append("\n\n");

        sb.append(colorRed(Constants.SYMBOL_CROSS)).append(" ");
        sb.append(result.getErrorMessage()).append("\n\n");

        if (result.getDuration() != null) {
            sb.append(formatBullet("Duration: " + result.getFormattedDuration()));
        }

        sb.append("\n");
        sb.append("The operation has been rolled back. No changes were made.\n");
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Displays a confirmation prompt.
     *
     * @return Formatted string
     */
    public String displayConfirmationPrompt() {
        return "\n" + bold("Proceed with bootstrap?");
    }

    private String formatRow(String label, String value) {
        return String.format("  %-18s %s%n", colorCyan(label + ":"), value);
    }

    private String formatBullet(String text) {
        return "  " + Constants.SYMBOL_BULLET + " " + text + "\n";
    }

    private String formatDependencies(ProjectConfiguration config) {
        if (config.getDependencies().isEmpty()) {
            return "(none)";
        }
        return String.join(", ", config.getDependencies());
    }

    // Color helper methods

    private String bold(String text) {
        return useColors ? Constants.ANSI_BOLD + text + Constants.ANSI_RESET : text;
    }

    private String colorRed(String text) {
        return useColors ? Constants.ANSI_RED + text + Constants.ANSI_RESET : text;
    }

    private String colorGreen(String text) {
        return useColors ? Constants.ANSI_GREEN + text + Constants.ANSI_RESET : text;
    }

    private String colorCyan(String text) {
        return useColors ? Constants.ANSI_CYAN + text + Constants.ANSI_RESET : text;
    }
}
