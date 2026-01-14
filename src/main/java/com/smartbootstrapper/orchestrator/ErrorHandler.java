package com.smartbootstrapper.orchestrator;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.exception.*;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Handles and formats errors for CLI display.
 */
public class ErrorHandler {

    private final boolean useColors;
    private final boolean verbose;

    public ErrorHandler() {
        this(true, false);
    }

    public ErrorHandler(boolean useColors, boolean verbose) {
        this.useColors = useColors;
        this.verbose = verbose;
    }

    /**
     * Formats an exception for display.
     *
     * @param e The exception to format
     * @return Formatted error message
     */
    public String formatError(Exception e) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append(colorRed(bold(Constants.SYMBOL_CROSS + " Error: ")));
        sb.append(getMainMessage(e));
        sb.append("\n");

        // Add context if available
        String context = getContext(e);
        if (context != null && !context.isEmpty()) {
            sb.append("\n");
            sb.append(colorCyan("Context: ")).append(context);
            sb.append("\n");
        }

        // Add suggestion if available
        String suggestion = getSuggestion(e);
        if (suggestion != null && !suggestion.isEmpty()) {
            sb.append("\n");
            sb.append(colorYellow(Constants.SYMBOL_ARROW + " ")).append(suggestion);
            sb.append("\n");
        }

        // Add stack trace in verbose mode
        if (verbose) {
            sb.append("\n");
            sb.append(colorCyan("Stack trace:")).append("\n");
            sb.append(getStackTrace(e));
        }

        return sb.toString();
    }

    /**
     * Gets the main error message.
     */
    private String getMainMessage(Exception e) {
        if (e instanceof SmartBootstrapperException sbe) {
            return sbe.getMessage();
        }
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    /**
     * Gets context information from the exception.
     */
    private String getContext(Exception e) {
        if (e instanceof SmartBootstrapperException sbe) {
            return sbe.getContext();
        }
        return null;
    }

    /**
     * Gets a suggestion based on the exception type.
     */
    private String getSuggestion(Exception e) {
        if (e instanceof ManifestException) {
            return "Check the manifest file format and ensure all required fields are present.";
        }

        if (e instanceof GitException ge) {
            if (ge.getMessage().contains("not found")) {
                return "Verify the repository path and ensure the commit/file exists.";
            }
            return "Check that the Git repository is accessible and not corrupted.";
        }

        if (e instanceof RefactorException) {
            return "Check that the Java files have valid syntax.";
        }

        if (e instanceof InitializrException ie) {
            if (ie.isNetworkError()) {
                return "Check your internet connection and try again.";
            }
            if (ie.isClientError()) {
                return "Check the project configuration values (version, dependencies, etc.).";
            }
            if (ie.isServerError()) {
                return "Spring Initializr may be temporarily unavailable. Try again later.";
            }
            return "Verify the Spring Initializr URL and your network connection.";
        }

        if (e instanceof ValidationException) {
            return "Review the validation errors and fix the issues before retrying.";
        }

        return null;
    }

    /**
     * Gets the stack trace as a string.
     */
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Categorizes the exception type.
     */
    public ErrorCategory categorize(Exception e) {
        if (e instanceof ManifestException) {
            return ErrorCategory.CONFIGURATION;
        }
        if (e instanceof GitException) {
            return ErrorCategory.GIT;
        }
        if (e instanceof RefactorException) {
            return ErrorCategory.REFACTORING;
        }
        if (e instanceof InitializrException ie) {
            if (ie.isNetworkError()) {
                return ErrorCategory.NETWORK;
            }
            return ErrorCategory.INITIALIZR;
        }
        if (e instanceof ValidationException) {
            return ErrorCategory.VALIDATION;
        }
        return ErrorCategory.UNKNOWN;
    }

    /**
     * Gets the appropriate exit code for an exception.
     */
    public int getExitCode(Exception e) {
        return switch (categorize(e)) {
            case CONFIGURATION -> 1;
            case VALIDATION -> 2;
            case GIT -> 3;
            case REFACTORING -> 4;
            case INITIALIZR -> 5;
            case NETWORK -> 6;
            case UNKNOWN -> 99;
        };
    }

    // Color helper methods

    private String bold(String text) {
        return useColors ? Constants.ANSI_BOLD + text + Constants.ANSI_RESET : text;
    }

    private String colorRed(String text) {
        return useColors ? Constants.ANSI_RED + text + Constants.ANSI_RESET : text;
    }

    private String colorYellow(String text) {
        return useColors ? Constants.ANSI_YELLOW + text + Constants.ANSI_RESET : text;
    }

    private String colorCyan(String text) {
        return useColors ? Constants.ANSI_CYAN + text + Constants.ANSI_RESET : text;
    }

    /**
     * Error categories for classification.
     */
    public enum ErrorCategory {
        CONFIGURATION,
        VALIDATION,
        GIT,
        REFACTORING,
        INITIALIZR,
        NETWORK,
        UNKNOWN
    }
}
