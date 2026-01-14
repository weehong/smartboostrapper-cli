package com.smartbootstrapper.exception;

/**
 * Exception for Git repository operation errors.
 */
public class GitException extends SmartBootstrapperException {

    private final String repositoryPath;
    private final String commitHash;
    private final String filePath;

    public GitException(String message) {
        super(message);
        this.repositoryPath = null;
        this.commitHash = null;
        this.filePath = null;
    }

    public GitException(String message, Throwable cause) {
        super(message, cause);
        this.repositoryPath = null;
        this.commitHash = null;
        this.filePath = null;
    }

    public GitException(String message, String repositoryPath) {
        super(message, "repository: " + repositoryPath);
        this.repositoryPath = repositoryPath;
        this.commitHash = null;
        this.filePath = null;
    }

    public GitException(String message, String repositoryPath, Throwable cause) {
        super(message, "repository: " + repositoryPath, cause);
        this.repositoryPath = repositoryPath;
        this.commitHash = null;
        this.filePath = null;
    }

    public GitException(String message, String repositoryPath, String commitHash, String filePath) {
        super(message, buildContext(repositoryPath, commitHash, filePath));
        this.repositoryPath = repositoryPath;
        this.commitHash = commitHash;
        this.filePath = filePath;
    }

    public GitException(String message, String repositoryPath, String commitHash, String filePath, Throwable cause) {
        super(message, buildContext(repositoryPath, commitHash, filePath), cause);
        this.repositoryPath = repositoryPath;
        this.commitHash = commitHash;
        this.filePath = filePath;
    }

    private static String buildContext(String repositoryPath, String commitHash, String filePath) {
        StringBuilder sb = new StringBuilder();
        if (repositoryPath != null) {
            sb.append("repository: ").append(repositoryPath);
        }
        if (commitHash != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("commit: ").append(commitHash.length() > 7 ? commitHash.substring(0, 7) : commitHash);
        }
        if (filePath != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("file: ").append(filePath);
        }
        return sb.toString();
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getFilePath() {
        return filePath;
    }
}
