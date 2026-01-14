package com.smartbootstrapper.manifest;

import com.smartbootstrapper.exception.ManifestException;
import com.smartbootstrapper.model.Manifest;
import com.smartbootstrapper.model.ManifestEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ManifestParserTest {

    private ManifestParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new ManifestParser();
    }

    @Test
    void shouldParseValidManifest() throws IOException {
        String yaml = """
                sourceRepository: /path/to/repo
                files:
                  - commit: abc1234
                    sourcePath: src/main/java/com/old/Service.java
                    destinationPath: src/main/java/com/new/Service.java
                  - commit: def5678
                    sourcePath: src/main/resources/application.properties
                    destinationPath: src/main/resources/application.properties
                """;

        Path manifestPath = tempDir.resolve("manifest.yaml");
        Files.writeString(manifestPath, yaml);

        Manifest manifest = parser.parse(manifestPath);

        assertEquals("/path/to/repo", manifest.getSourceRepositoryPath());
        assertEquals(2, manifest.size());

        ManifestEntry first = manifest.getEntries().get(0);
        assertEquals("abc1234", first.getCommitHash());
        assertEquals("src/main/java/com/old/Service.java", first.getSourcePath());
        assertEquals("src/main/java/com/new/Service.java", first.getDestinationPath());
    }

    @Test
    void shouldThrowExceptionForNonExistentFile() {
        Path nonExistent = tempDir.resolve("does-not-exist.yaml");

        ManifestException exception = assertThrows(ManifestException.class, () ->
                parser.parse(nonExistent));

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void shouldThrowExceptionForEmptyManifest() throws IOException {
        Path manifestPath = tempDir.resolve("empty.yaml");
        Files.writeString(manifestPath, "");

        ManifestException exception = assertThrows(ManifestException.class, () ->
                parser.parse(manifestPath));

        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    void shouldThrowExceptionForMissingSourceRepository() {
        String yaml = """
                files:
                  - commit: abc1234
                    sourcePath: src/main/java/Test.java
                    destinationPath: src/main/java/Test.java
                """;

        ByteArrayInputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        ManifestException exception = assertThrows(ManifestException.class, () ->
                parser.parseFromInputStream(stream, "test.yaml"));

        assertTrue(exception.getMessage().contains("sourceRepository"));
    }

    @Test
    void shouldThrowExceptionForMissingFiles() {
        String yaml = """
                sourceRepository: /path/to/repo
                """;

        ByteArrayInputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        ManifestException exception = assertThrows(ManifestException.class, () ->
                parser.parseFromInputStream(stream, "test.yaml"));

        assertTrue(exception.getMessage().contains("files"));
    }

    @Test
    void shouldThrowExceptionForInvalidCommitHash() {
        String yaml = """
                sourceRepository: /path/to/repo
                files:
                  - commit: invalid!@#
                    sourcePath: src/Test.java
                    destinationPath: src/Test.java
                """;

        ByteArrayInputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        ManifestException exception = assertThrows(ManifestException.class, () ->
                parser.parseFromInputStream(stream, "test.yaml"));

        assertTrue(exception.getMessage().contains("Invalid commit hash"));
    }

    @Test
    void shouldThrowExceptionForAbsoluteSourcePath() {
        String yaml = """
                sourceRepository: /path/to/repo
                files:
                  - commit: abc1234
                    sourcePath: /absolute/path/Test.java
                    destinationPath: src/Test.java
                """;

        ByteArrayInputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        ManifestException exception = assertThrows(ManifestException.class, () ->
                parser.parseFromInputStream(stream, "test.yaml"));

        assertTrue(exception.getMessage().contains("relative"));
    }

    @Test
    void shouldThrowExceptionForPathTraversal() {
        String yaml = """
                sourceRepository: /path/to/repo
                files:
                  - commit: abc1234
                    sourcePath: src/../../../etc/passwd
                    destinationPath: src/Test.java
                """;

        ByteArrayInputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        ManifestException exception = assertThrows(ManifestException.class, () ->
                parser.parseFromInputStream(stream, "test.yaml"));

        assertTrue(exception.getMessage().contains(".."));
    }

    @Test
    void shouldAcceptFullAndAbbreviatedCommitHashes() throws IOException {
        String yaml = """
                sourceRepository: /path/to/repo
                files:
                  - commit: abc1234
                    sourcePath: src/Short.java
                    destinationPath: src/Short.java
                  - commit: abc1234567890abcdef1234567890abcdef12345
                    sourcePath: src/Full.java
                    destinationPath: src/Full.java
                """;

        Path manifestPath = tempDir.resolve("manifest.yaml");
        Files.writeString(manifestPath, yaml);

        Manifest manifest = parser.parse(manifestPath);

        assertEquals(2, manifest.size());
        assertEquals("abc1234", manifest.getEntries().get(0).getCommitHash());
        assertEquals("abc1234567890abcdef1234567890abcdef12345",
                manifest.getEntries().get(1).getCommitHash());
    }

    @Test
    void shouldExtractSequenceNumberFromFilename() throws IOException {
        String yaml = """
                sourceRepository: /path/to/repo
                files:
                  - commit: abc1234
                    sourcePath: src/Test.java
                    destinationPath: src/Test.java
                """;

        Path manifestPath = tempDir.resolve("commit-1.yaml");
        Files.writeString(manifestPath, yaml);

        Manifest manifest = parser.parse(manifestPath);

        assertTrue(manifest.hasSequenceNumber());
        assertEquals(1, manifest.getSequenceNumber().orElseThrow());
    }

    @Test
    void shouldExtractSequenceNumberFromYmlExtension() throws IOException {
        String yaml = """
                sourceRepository: /path/to/repo
                files:
                  - commit: abc1234
                    sourcePath: src/Test.java
                    destinationPath: src/Test.java
                """;

        Path manifestPath = tempDir.resolve("commit-5.yml");
        Files.writeString(manifestPath, yaml);

        Manifest manifest = parser.parse(manifestPath);

        assertTrue(manifest.hasSequenceNumber());
        assertEquals(5, manifest.getSequenceNumber().orElseThrow());
    }

    @Test
    void shouldExtractTwoDigitSequenceNumber() throws IOException {
        String yaml = """
                sourceRepository: /path/to/repo
                files:
                  - commit: abc1234
                    sourcePath: src/Test.java
                    destinationPath: src/Test.java
                """;

        Path manifestPath = tempDir.resolve("commit-10.yaml");
        Files.writeString(manifestPath, yaml);

        Manifest manifest = parser.parse(manifestPath);

        assertTrue(manifest.hasSequenceNumber());
        assertEquals(10, manifest.getSequenceNumber().orElseThrow());
    }

    @Test
    void shouldNotExtractSequenceNumberFromNonMatchingFilename() throws IOException {
        String yaml = """
                sourceRepository: /path/to/repo
                files:
                  - commit: abc1234
                    sourcePath: src/Test.java
                    destinationPath: src/Test.java
                """;

        Path manifestPath = tempDir.resolve("my-manifest.yaml");
        Files.writeString(manifestPath, yaml);

        Manifest manifest = parser.parse(manifestPath);

        assertFalse(manifest.hasSequenceNumber());
        assertTrue(manifest.getSequenceNumber().isEmpty());
    }

    @Test
    void shouldExtractSequenceNumberDirectly() {
        assertEquals(1, parser.extractSequenceNumber(Path.of("commit-1.yaml")));
        assertEquals(5, parser.extractSequenceNumber(Path.of("commit-5.yml")));
        assertEquals(99, parser.extractSequenceNumber(Path.of("commit-99.yaml")));
        assertNull(parser.extractSequenceNumber(Path.of("manifest.yaml")));
        assertNull(parser.extractSequenceNumber(Path.of("commit.yaml")));
        assertNull(parser.extractSequenceNumber(Path.of("commit-.yaml")));
    }
}
