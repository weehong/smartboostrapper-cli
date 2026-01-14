package com.smartbootstrapper.exception;

/**
 * Exception for Spring Initializr download errors.
 */
public class InitializrException extends SmartBootstrapperException {

    private final String url;
    private final Integer httpStatusCode;

    public InitializrException(String message) {
        super(message);
        this.url = null;
        this.httpStatusCode = null;
    }

    public InitializrException(String message, Throwable cause) {
        super(message, cause);
        this.url = null;
        this.httpStatusCode = null;
    }

    public InitializrException(String message, String url) {
        super(message, "url: " + url);
        this.url = url;
        this.httpStatusCode = null;
    }

    public InitializrException(String message, String url, Throwable cause) {
        super(message, "url: " + url, cause);
        this.url = url;
        this.httpStatusCode = null;
    }

    public InitializrException(String message, String url, int httpStatusCode) {
        super(message, String.format("url: %s, status: %d", url, httpStatusCode));
        this.url = url;
        this.httpStatusCode = httpStatusCode;
    }

    public InitializrException(String message, String url, int httpStatusCode, Throwable cause) {
        super(message, String.format("url: %s, status: %d", url, httpStatusCode), cause);
        this.url = url;
        this.httpStatusCode = httpStatusCode;
    }

    public String getUrl() {
        return url;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public boolean hasHttpStatusCode() {
        return httpStatusCode != null;
    }

    public boolean isNetworkError() {
        return httpStatusCode == null && getCause() != null;
    }

    public boolean isServerError() {
        return httpStatusCode != null && httpStatusCode >= 500;
    }

    public boolean isClientError() {
        return httpStatusCode != null && httpStatusCode >= 400 && httpStatusCode < 500;
    }
}
