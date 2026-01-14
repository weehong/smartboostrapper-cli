package com.smartbootstrapper.validation;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.model.ValidationResult;

import java.util.List;

/**
 * Formats validation results for CLI display.
 */
public class ValidationResultFormatter {

    private final boolean useColors;

    public ValidationResultFormatter() {
        this(true);
    }

    public ValidationResultFormatter(boolean useColors) {
        this.useColors = useColors;
    }

    /**
     * Formats the complete validation result for display.
     *
     * @param result The validation result to format
     * @return Formatted string
     */
    public String format(ValidationResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append(formatHeader("Validation Results"));
        sb.append("\n\n");

        // Format checks as a checklist
        sb.append(formatChecklist(result.getChecks()));

        // Format errors if any
        if (result.hasErrors()) {
            sb.append("\n");
            sb.append(formatErrors(result.getErrors()));
        }

        // Format summary
        sb.append("\n");
        sb.append(formatSummary(result));

        return sb.toString();
    }

    /**
     * Formats validation checks as a checklist.
     */
    public String formatChecklist(List<ValidationResult.ValidationCheck> checks) {
        StringBuilder sb = new StringBuilder();

        for (ValidationResult.ValidationCheck check : checks) {
            String symbol = check.isPassed() ? colorGreen(Constants.SYMBOL_CHECK) : colorRed(Constants.SYMBOL_CROSS);
            sb.append("  ").append(symbol).append(" ").append(check.getName());

            if (check.getDetails() != null && !check.getDetails().isEmpty()) {
                sb.append(colorCyan(" (" + check.getDetails() + ")"));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Formats validation errors.
     */
    public String formatErrors(List<ValidationResult.ValidationError> errors) {
        StringBuilder sb = new StringBuilder();

        sb.append(colorRed(bold("Errors:"))).append("\n\n");

        for (int i = 0; i < errors.size(); i++) {
            ValidationResult.ValidationError error = errors.get(i);
            sb.append("  ").append(colorRed(String.valueOf(i + 1))).append(". ");
            sb.append(error.getMessage()).append("\n");

            // Add file/line info if available
            if (error.getFile() != null) {
                sb.append("     ").append(colorCyan("File: ")).append(error.getFile());
                if (error.getLine() != null) {
                    sb.append(":").append(error.getLine());
                }
                sb.append("\n");
            }

            // Add commit info if available
            if (error.getCommit() != null) {
                String shortCommit = error.getCommit().substring(0, Math.min(7, error.getCommit().length()));
                sb.append("     ").append(colorCyan("Commit: ")).append(shortCommit).append("\n");
            }

            // Add context if available
            if (error.getContext() != null && !error.getContext().isEmpty()) {
                sb.append("     ").append(colorYellow(Constants.SYMBOL_ARROW + " ")).append(error.getContext()).append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Formats the validation summary.
     */
    public String formatSummary(ValidationResult result) {
        StringBuilder sb = new StringBuilder();

        if (result.isSuccess()) {
            sb.append(colorGreen(bold(Constants.SYMBOL_CHECK + " " + Constants.SUCCESS_VALIDATION)));
        } else {
            sb.append(colorRed(bold(Constants.SYMBOL_CROSS + " " +
                    String.format(Constants.ERROR_VALIDATION_FAILED, result.getErrorCount()))));
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Formats a simple header.
     */
    public String formatHeader(String text) {
        return bold(text);
    }

    // Color helper methods

    private String colorRed(String text) {
        return useColors ? Constants.ANSI_RED + text + Constants.ANSI_RESET : text;
    }

    private String colorGreen(String text) {
        return useColors ? Constants.ANSI_GREEN + text + Constants.ANSI_RESET : text;
    }

    private String colorYellow(String text) {
        return useColors ? Constants.ANSI_YELLOW + text + Constants.ANSI_RESET : text;
    }

    private String colorCyan(String text) {
        return useColors ? Constants.ANSI_CYAN + text + Constants.ANSI_RESET : text;
    }

    private String bold(String text) {
        return useColors ? Constants.ANSI_BOLD + text + Constants.ANSI_RESET : text;
    }
}
