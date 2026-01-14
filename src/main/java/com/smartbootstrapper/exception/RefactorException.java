package com.smartbootstrapper.exception;

/**
 * Exception for code refactoring errors.
 */
public class RefactorException extends SmartBootstrapperException {

    private final String filePath;
    private final Integer lineNumber;
    private final String oldPackage;
    private final String newPackage;

    public RefactorException(String message) {
        super(message);
        this.filePath = null;
        this.lineNumber = null;
        this.oldPackage = null;
        this.newPackage = null;
    }

    public RefactorException(String message, Throwable cause) {
        super(message, cause);
        this.filePath = null;
        this.lineNumber = null;
        this.oldPackage = null;
        this.newPackage = null;
    }

    public RefactorException(String message, String filePath) {
        super(message, "file: " + filePath);
        this.filePath = filePath;
        this.lineNumber = null;
        this.oldPackage = null;
        this.newPackage = null;
    }

    public RefactorException(String message, String filePath, int lineNumber) {
        super(message, String.format("file: %s, line: %d", filePath, lineNumber));
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.oldPackage = null;
        this.newPackage = null;
    }

    public RefactorException(String message, String filePath, Throwable cause) {
        super(message, "file: " + filePath, cause);
        this.filePath = filePath;
        this.lineNumber = null;
        this.oldPackage = null;
        this.newPackage = null;
    }

    public RefactorException(String message, String filePath, int lineNumber, Throwable cause) {
        super(message, String.format("file: %s, line: %d", filePath, lineNumber), cause);
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.oldPackage = null;
        this.newPackage = null;
    }

    public RefactorException(String message, String filePath, String oldPackage, String newPackage) {
        super(message, String.format("file: %s, refactor: %s -> %s", filePath, oldPackage, newPackage));
        this.filePath = filePath;
        this.lineNumber = null;
        this.oldPackage = oldPackage;
        this.newPackage = newPackage;
    }

    public RefactorException(String message, String filePath, String oldPackage, String newPackage, Throwable cause) {
        super(message, String.format("file: %s, refactor: %s -> %s", filePath, oldPackage, newPackage), cause);
        this.filePath = filePath;
        this.lineNumber = null;
        this.oldPackage = oldPackage;
        this.newPackage = newPackage;
    }

    public String getFilePath() {
        return filePath;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public String getOldPackage() {
        return oldPackage;
    }

    public String getNewPackage() {
        return newPackage;
    }

    public boolean hasLineNumber() {
        return lineNumber != null;
    }
}
