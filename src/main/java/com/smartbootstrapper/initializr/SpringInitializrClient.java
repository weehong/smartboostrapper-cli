package com.smartbootstrapper.initializr;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.exception.InitializrException;
import com.smartbootstrapper.model.ProjectConfiguration;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for interacting with Spring Initializr (start.spring.io).
 */
public class SpringInitializrClient {

    private static final Logger logger = LoggerFactory.getLogger(SpringInitializrClient.class);

    private final OkHttpClient httpClient;
    private final String baseUrl;

    public SpringInitializrClient() {
        this(Constants.SPRING_INITIALIZR_URL);
    }

    public SpringInitializrClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Constants.HTTP_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(Constants.HTTP_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(Constants.HTTP_WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .build();
    }

    /**
     * Downloads a Spring Boot skeleton project from Spring Initializr.
     *
     * @param config The project configuration
     * @return Path to the downloaded ZIP file
     * @throws InitializrException if the download fails
     */
    public Path downloadSkeleton(ProjectConfiguration config) {
        String url = buildRequestUrl(config);
        logger.info("Downloading Spring Boot skeleton from: {}", url);

        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/zip")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new InitializrException(
                        Constants.ERROR_INITIALIZR_DOWNLOAD + ": HTTP " + response.code(),
                        url,
                        response.code()
                );
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new InitializrException(
                        Constants.ERROR_INITIALIZR_DOWNLOAD + ": Empty response",
                        url
                );
            }

            // Save to temporary file
            Path tempFile = Files.createTempFile("spring-boot-", ".zip");
            try (InputStream inputStream = body.byteStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            long fileSize = Files.size(tempFile);
            logger.info("Downloaded skeleton ({} bytes) to: {}", fileSize, tempFile);

            return tempFile;

        } catch (IOException e) {
            throw new InitializrException(
                    String.format(Constants.ERROR_INITIALIZR_NETWORK, e.getMessage()),
                    url,
                    e
            );
        }
    }

    /**
     * Builds the request URL for Spring Initializr based on configuration.
     *
     * @param config The project configuration
     * @return The complete URL string
     */
    String buildRequestUrl(ProjectConfiguration config) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + "/starter.zip").newBuilder();

        // Required parameters
        urlBuilder.addQueryParameter("type", "maven-project");
        urlBuilder.addQueryParameter("language", "java");
        urlBuilder.addQueryParameter("packaging", Constants.DEFAULT_PACKAGING);

        // Project coordinates
        urlBuilder.addQueryParameter("groupId", config.getGroupId());
        urlBuilder.addQueryParameter("artifactId", config.getArtifactId());
        urlBuilder.addQueryParameter("name", config.getProjectName());
        urlBuilder.addQueryParameter("packageName", config.getNewPackage());
        urlBuilder.addQueryParameter("version", config.getVersion());

        // Spring Boot and Java version
        urlBuilder.addQueryParameter("bootVersion", config.getSpringBootVersion());
        urlBuilder.addQueryParameter("javaVersion", config.getJavaVersion());

        // Dependencies
        if (!config.getDependencies().isEmpty()) {
            String dependencies = String.join(",", config.getDependencies());
            urlBuilder.addQueryParameter("dependencies", dependencies);
        }

        return urlBuilder.build().toString();
    }

    /**
     * Tests connectivity to Spring Initializr.
     *
     * @return true if connection is successful, false otherwise
     */
    public boolean testConnection() {
        Request request = new Request.Builder()
                .url(baseUrl)
                .head()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            boolean success = response.isSuccessful();
            logger.debug("Spring Initializr connection test: {}", success ? "OK" : "FAILED");
            return success;
        } catch (IOException e) {
            logger.debug("Spring Initializr connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets available Spring Boot versions from the Initializr API.
     *
     * @return Array of available Spring Boot versions
     */
    public String[] getAvailableSpringBootVersions() {
        return Constants.SUPPORTED_SPRING_BOOT_VERSIONS.toArray(new String[0]);
    }

    /**
     * Gets the base URL being used.
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}
