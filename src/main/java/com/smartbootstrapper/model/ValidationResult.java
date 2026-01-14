package com.smartbootstrapper.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the result of a validation operation.
 * Contains success status, a list of checks performed, and error details.
 */
public class ValidationResult {

    private final boolean success;
    private final List<ValidationCheck> checks;
    private final List<ValidationError> errors;

    private ValidationResult(boolean success, List<ValidationCheck> checks, List<ValidationError> errors) {
        this.success = success;
        this.checks = Collections.unmodifiableList(new ArrayList<>(checks));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public boolean isSuccess() {
        return success;
    }

    public List<ValidationCheck> getChecks() {
        return checks;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public int getErrorCount() {
        return errors.size();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ValidationResult success(List<ValidationCheck> checks) {
        return new ValidationResult(true, checks, Collections.emptyList());
    }

    public static ValidationResult failure(List<ValidationCheck> checks, List<ValidationError> errors) {
        return new ValidationResult(false, checks, errors);
    }

    public static class Builder {
        private final List<ValidationCheck> checks = new ArrayList<>();
        private final List<ValidationError> errors = new ArrayList<>();

        public Builder addCheck(String name, boolean passed) {
            checks.add(new ValidationCheck(name, passed));
            return this;
        }

        public Builder addCheck(String name, boolean passed, String details) {
            checks.add(new ValidationCheck(name, passed, details));
            return this;
        }

        public Builder addError(ValidationError error) {
            errors.add(error);
            return this;
        }

        public Builder addError(String message) {
            errors.add(new ValidationError(message));
            return this;
        }

        public Builder addError(String message, String context) {
            errors.add(new ValidationError(message, context));
            return this;
        }

        public ValidationResult build() {
            boolean success = errors.isEmpty() && checks.stream().allMatch(ValidationCheck::isPassed);
            return new ValidationResult(success, checks, errors);
        }
    }

    /**
     * Represents a single validation check.
     */
    public static class ValidationCheck {
        private final String name;
        private final boolean passed;
        private final String details;

        public ValidationCheck(String name, boolean passed) {
            this(name, passed, null);
        }

        public ValidationCheck(String name, boolean passed, String details) {
            this.name = name;
            this.passed = passed;
            this.details = details;
        }

        public String getName() {
            return name;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getDetails() {
            return details;
        }

        @Override
        public String toString() {
            String symbol = passed ? "\u2713" : "\u2717";
            String result = symbol + " " + name;
            if (details != null && !details.isEmpty()) {
                result += " (" + details + ")";
            }
            return result;
        }
    }

    /**
     * Represents a validation error with context.
     */
    public static class ValidationError {
        private final String message;
        private final String context;
        private final String file;
        private final Integer line;
        private final String commit;

        public ValidationError(String message) {
            this(message, null, null, null, null);
        }

        public ValidationError(String message, String context) {
            this(message, context, null, null, null);
        }

        public ValidationError(String message, String context, String file, Integer line, String commit) {
            this.message = message;
            this.context = context;
            this.file = file;
            this.line = line;
            this.commit = commit;
        }

        public String getMessage() {
            return message;
        }

        public String getContext() {
            return context;
        }

        public String getFile() {
            return file;
        }

        public Integer getLine() {
            return line;
        }

        public String getCommit() {
            return commit;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(message);
            if (file != null) {
                sb.append(" [file: ").append(file);
                if (line != null) {
                    sb.append(":").append(line);
                }
                sb.append("]");
            }
            if (commit != null) {
                sb.append(" [commit: ").append(commit.substring(0, Math.min(7, commit.length()))).append("]");
            }
            if (context != null) {
                sb.append("\n  Context: ").append(context);
            }
            return sb.toString();
        }
    }
}
