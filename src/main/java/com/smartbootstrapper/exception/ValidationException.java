package com.smartbootstrapper.exception;

import com.smartbootstrapper.model.ValidationResult;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception for validation failure errors.
 */
public class ValidationException extends SmartBootstrapperException {

    private final ValidationResult validationResult;

    public ValidationException(String message) {
        super(message);
        this.validationResult = null;
    }

    public ValidationException(String message, ValidationResult validationResult) {
        super(message, buildContext(validationResult));
        this.validationResult = validationResult;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.validationResult = null;
    }

    public ValidationException(String message, ValidationResult validationResult, Throwable cause) {
        super(message, buildContext(validationResult), cause);
        this.validationResult = validationResult;
    }

    private static String buildContext(ValidationResult result) {
        if (result == null) {
            return null;
        }
        List<ValidationResult.ValidationError> errors = result.getErrors();
        if (errors.isEmpty()) {
            return "no specific errors";
        }
        return errors.stream()
                .map(ValidationResult.ValidationError::getMessage)
                .limit(3)
                .collect(Collectors.joining("; "));
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    public boolean hasValidationResult() {
        return validationResult != null;
    }

    public int getErrorCount() {
        return validationResult != null ? validationResult.getErrorCount() : 0;
    }

    public List<ValidationResult.ValidationError> getErrors() {
        return validationResult != null ? validationResult.getErrors() : List.of();
    }
}
