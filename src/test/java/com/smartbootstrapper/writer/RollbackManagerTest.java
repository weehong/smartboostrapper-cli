package com.smartbootstrapper.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RollbackManagerTest {

    private RollbackManager rollbackManager;
    private List<String> progressMessages;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        rollbackManager = new RollbackManager();
        progressMessages = new ArrayList<>();
        rollbackManager.setProgressCallback(progressMessages::add);
    }

    @Test
    void shouldTrackCreatedFiles() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        rollbackManager.recordFileCreated(file);

        assertEquals(1, rollbackManager.getChangeCount());
        assertTrue(rollbackManager.hasChanges());
    }

    @Test
    void shouldRollbackCreatedFiles() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        rollbackManager.recordFileCreated(file);
        boolean success = rollbackManager.rollback();

        assertTrue(success);
        assertFalse(Files.exists(file));
        assertTrue(rollbackManager.isRollbackPerformed());
    }

    @Test
    void shouldRollbackModifiedFiles() throws IOException {
        Path file = tempDir.resolve("test.txt");
        String originalContent = "original";
        Files.writeString(file, originalContent);

        rollbackManager.recordFileModified(file, originalContent.getBytes());

        // Modify the file
        Files.writeString(file, "modified");

        boolean success = rollbackManager.rollback();

        assertTrue(success);
        assertEquals(originalContent, Files.readString(file));
    }

    @Test
    void shouldRollbackCreatedDirectories() throws IOException {
        Path dir = tempDir.resolve("newdir");
        Files.createDirectories(dir);

        rollbackManager.recordDirectoryCreated(dir);
        boolean success = rollbackManager.rollback();

        assertTrue(success);
        assertFalse(Files.exists(dir));
    }

    @Test
    void shouldRollbackInReverseOrder() throws IOException {
        // Create nested structure: dir/subdir/file.txt
        Path dir = tempDir.resolve("dir");
        Path subdir = dir.resolve("subdir");
        Path file = subdir.resolve("file.txt");

        Files.createDirectories(subdir);
        Files.writeString(file, "content");

        // Record in creation order
        rollbackManager.recordDirectoryCreated(dir);
        rollbackManager.recordDirectoryCreated(subdir);
        rollbackManager.recordFileCreated(file);

        boolean success = rollbackManager.rollback();

        assertTrue(success);
        assertFalse(Files.exists(file));
        assertFalse(Files.exists(subdir));
        assertFalse(Files.exists(dir));
    }

    @Test
    void shouldNotRollbackTwice() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        rollbackManager.recordFileCreated(file);
        rollbackManager.rollback();

        // Try to rollback again
        boolean secondRollback = rollbackManager.rollback();

        assertTrue(secondRollback);
        assertEquals(0, rollbackManager.getChangeCount());
    }

    @Test
    void shouldClearChanges() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        rollbackManager.recordFileCreated(file);
        rollbackManager.clear();

        assertEquals(0, rollbackManager.getChangeCount());
        assertFalse(rollbackManager.hasChanges());
    }

    @Test
    void shouldProvideChangeSummary() throws IOException {
        Path file1 = tempDir.resolve("new.txt");
        Path file2 = tempDir.resolve("modified.txt");
        Path dir = tempDir.resolve("newdir");

        Files.writeString(file1, "new");
        Files.writeString(file2, "original");
        Files.createDirectories(dir);

        rollbackManager.recordFileCreated(file1);
        rollbackManager.recordFileModified(file2, "original".getBytes());
        rollbackManager.recordDirectoryCreated(dir);

        String summary = rollbackManager.getSummary();

        assertTrue(summary.contains("1 files created"));
        assertTrue(summary.contains("1 files modified"));
        assertTrue(summary.contains("1 directories created"));
    }

    @Test
    void shouldReportProgressDuringRollback() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "content");

        rollbackManager.recordFileCreated(file);
        rollbackManager.rollback();

        assertFalse(progressMessages.isEmpty());
    }
}
