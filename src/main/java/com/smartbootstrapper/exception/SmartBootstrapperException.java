package com.smartbootstrapper.exception;

/**
 * Base exception for all SmartBootstrapper errors.
 */
public class SmartBootstrapperException extends RuntimeException {

    private final String context;

    public SmartBootstrapperException(String message) {
        super(message);
        this.context = null;
    }

    public SmartBootstrapperException(String message, Throwable cause) {
        super(message, cause);
        this.context = null;
    }

    public SmartBootstrapperException(String message, String context) {
        super(message);
        this.context = context;
    }

    public SmartBootstrapperException(String message, String context, Throwable cause) {
        super(message, cause);
        this.context = context;
    }

    public String getContext() {
        return context;
    }

    public boolean hasContext() {
        return context != null && !context.isEmpty();
    }

    @Override
    public String toString() {
        if (hasContext()) {
            return String.format("%s: %s [context: %s]", getClass().getSimpleName(), getMessage(), context);
        }
        return String.format("%s: %s", getClass().getSimpleName(), getMessage());
    }
}
