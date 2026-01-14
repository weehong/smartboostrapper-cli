package com.smartbootstrapper.exception;

/**
 * Exception for manifest parsing and validation errors.
 */
public class ManifestException extends SmartBootstrapperException {

    private final String manifestPath;
    private final Integer lineNumber;

    public ManifestException(String message) {
        super(message);
        this.manifestPath = null;
        this.lineNumber = null;
    }

    public ManifestException(String message, String manifestPath) {
        super(message, "manifest: " + manifestPath);
        this.manifestPath = manifestPath;
        this.lineNumber = null;
    }

    public ManifestException(String message, String manifestPath, int lineNumber) {
        super(message, String.format("manifest: %s, line: %d", manifestPath, lineNumber));
        this.manifestPath = manifestPath;
        this.lineNumber = lineNumber;
    }

    public ManifestException(String message, String manifestPath, Throwable cause) {
        super(message, "manifest: " + manifestPath, cause);
        this.manifestPath = manifestPath;
        this.lineNumber = null;
    }

    public ManifestException(String message, String manifestPath, int lineNumber, Throwable cause) {
        super(message, String.format("manifest: %s, line: %d", manifestPath, lineNumber), cause);
        this.manifestPath = manifestPath;
        this.lineNumber = lineNumber;
    }

    public String getManifestPath() {
        return manifestPath;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public boolean hasLineNumber() {
        return lineNumber != null;
    }
}
