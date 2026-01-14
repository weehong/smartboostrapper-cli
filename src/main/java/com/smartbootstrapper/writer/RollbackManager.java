package com.smartbootstrapper.writer;

import com.smartbootstrapper.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages rollback of filesystem changes.
 * Tracks created files, modified files, and created directories for reversal.
 */
public class RollbackManager {

    private static final Logger logger = LoggerFactory.getLogger(RollbackManager.class);

    private final List<Change> changes = new ArrayList<>();
    private Consumer<String> progressCallback;
    private boolean rollbackPerformed = false;

    /**
     * Sets a callback for rollback progress updates.
     *
     * @param callback Consumer that receives progress messages
     */
    public void setProgressCallback(Consumer<String> callback) {
        this.progressCallback = callback;
    }

    /**
     * Records that a new file was created.
     *
     * @param path Path to the created file
     */
    public void recordFileCreated(Path path) {
        changes.add(new Change(ChangeType.FILE_CREATED, path, null));
        logger.debug("Recorded file creation: {}", path);
    }

    /**
     * Records that an existing file was modified.
     *
     * @param path            Path to the modified file
     * @param originalContent Original content before modification
     */
    public void recordFileModified(Path path, byte[] originalContent) {
        changes.add(new Change(ChangeType.FILE_MODIFIED, path, originalContent));
        logger.debug("Recorded file modification: {}", path);
    }

    /**
     * Records that a new directory was created.
     *
     * @param path Path to the created directory
     */
    public void recordDirectoryCreated(Path path) {
        changes.add(new Change(ChangeType.DIRECTORY_CREATED, path, null));
        logger.debug("Recorded directory creation: {}", path);
    }

    /**
     * Performs rollback of all recorded changes in reverse order.
     *
     * @return true if rollback completed successfully, false if errors occurred
     */
    public boolean rollback() {
        if (rollbackPerformed) {
            logger.warn("Rollback already performed");
            return true;
        }

        logger.info("Starting rollback of {} changes", changes.size());
        reportProgress("Starting rollback...");

        boolean success = true;
        List<Change> reversedChanges = new ArrayList<>(changes);
        Collections.reverse(reversedChanges);

        for (Change change : reversedChanges) {
            try {
                rollbackChange(change);
            } catch (Exception e) {
                logger.error("Failed to rollback change: {} - {}", change, e.getMessage());
                reportProgress(String.format(Constants.ERROR_ROLLBACK, e.getMessage()));
                success = false;
            }
        }

        rollbackPerformed = true;
        changes.clear();

        if (success) {
            logger.info("Rollback completed successfully");
            reportProgress("Rollback completed successfully");
        } else {
            logger.warn("Rollback completed with errors");
            reportProgress("Rollback completed with some errors");
        }

        return success;
    }

    private void rollbackChange(Change change) throws IOException {
        switch (change.type) {
            case FILE_CREATED -> rollbackFileCreated(change.path);
            case FILE_MODIFIED -> rollbackFileModified(change.path, change.originalContent);
            case DIRECTORY_CREATED -> rollbackDirectoryCreated(change.path);
        }
    }

    private void rollbackFileCreated(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.delete(path);
            logger.debug("Rolled back file creation: {}", path);
            reportProgress(Constants.SYMBOL_CHECK + " Removed created file: " + path.getFileName());
        }
    }

    private void rollbackFileModified(Path path, byte[] originalContent) throws IOException {
        if (originalContent != null) {
            Files.write(path, originalContent);
            logger.debug("Rolled back file modification: {}", path);
            reportProgress(Constants.SYMBOL_CHECK + " Restored modified file: " + path.getFileName());
        }
    }

    private void rollbackDirectoryCreated(Path path) throws IOException {
        if (Files.exists(path) && Files.isDirectory(path)) {
            // Only delete if empty (files should have been deleted first due to reverse order)
            try {
                Files.delete(path);
                logger.debug("Rolled back directory creation: {}", path);
                reportProgress(Constants.SYMBOL_CHECK + " Removed created directory: " + path.getFileName());
            } catch (IOException e) {
                // Directory not empty - this is expected if it contains files we didn't create
                logger.debug("Could not delete directory (not empty?): {}", path);
            }
        }
    }

    private void reportProgress(String message) {
        if (progressCallback != null) {
            progressCallback.accept(message);
        }
    }

    /**
     * Returns the number of recorded changes.
     */
    public int getChangeCount() {
        return changes.size();
    }

    /**
     * Returns whether any changes have been recorded.
     */
    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    /**
     * Returns whether rollback has been performed.
     */
    public boolean isRollbackPerformed() {
        return rollbackPerformed;
    }

    /**
     * Clears all recorded changes without performing rollback.
     * Use with caution - typically only after successful completion.
     */
    public void clear() {
        changes.clear();
        logger.debug("Cleared all recorded changes");
    }

    /**
     * Returns a summary of recorded changes.
     */
    public String getSummary() {
        long files = changes.stream().filter(c -> c.type == ChangeType.FILE_CREATED).count();
        long modified = changes.stream().filter(c -> c.type == ChangeType.FILE_MODIFIED).count();
        long dirs = changes.stream().filter(c -> c.type == ChangeType.DIRECTORY_CREATED).count();

        return String.format("Changes recorded: %d files created, %d files modified, %d directories created",
                files, modified, dirs);
    }

    private enum ChangeType {
        FILE_CREATED,
        FILE_MODIFIED,
        DIRECTORY_CREATED
    }

    private record Change(ChangeType type, Path path, byte[] originalContent) {
        @Override
        public String toString() {
            return type + ": " + path;
        }
    }
}
