package com.smartbootstrapper.orchestrator;

import com.smartbootstrapper.Constants;
import com.smartbootstrapper.git.GitRepositoryService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BootstrapOrchestratorTest {

    @TempDir
    Path tempDir;

    @Test
    void initialCommitMessageShouldFollowConventionalCommitFormat() {
        String message = Constants.INITIAL_COMMIT_MESSAGE;

        assertTrue(
                Constants.CONVENTIONAL_COMMIT_PATTERN.matcher(message).matches(),
                "Initial commit message should follow conventional commit format: " + message
        );
    }

    @Test
    void initialCommitMessageShouldStartWithChoreInit() {
        String message = Constants.INITIAL_COMMIT_MESSAGE;

        assertTrue(
                message.startsWith("chore(init):"),
                "Initial commit message should start with 'chore(init):' but was: " + message
        );
    }

    @Test
    void initialCommitMessageShouldMentionSpringInitializr() {
        String message = Constants.INITIAL_COMMIT_MESSAGE;

        assertTrue(
                message.toLowerCase().contains("spring initializr"),
                "Initial commit message should mention Spring Initializr: " + message
        );
    }

    @Test
    void shouldCreateInitialCommitWithCorrectMessage() throws Exception {
        // Create a dummy file in the temp directory
        Path dummyFile = tempDir.resolve("README.md");
        Files.writeString(dummyFile, "# Test Project");

        // Initialize git repository and create commit using GitRepositoryService
        try (GitRepositoryService gitService = GitRepositoryService.init(tempDir)) {
            gitService.addAll();
            String commitHash = gitService.commit(Constants.INITIAL_COMMIT_MESSAGE);

            assertNotNull(commitHash);
            assertFalse(commitHash.isEmpty());
        }

        // Verify the commit message using JGit directly
        try (Git git = Git.open(tempDir.toFile())) {
            Iterable<RevCommit> commits = git.log().setMaxCount(1).call();
            RevCommit latestCommit = commits.iterator().next();

            assertEquals(
                    Constants.INITIAL_COMMIT_MESSAGE,
                    latestCommit.getFullMessage().trim(),
                    "Commit message should match the constant"
            );
        }
    }

    @Test
    void conventionalCommitPatternShouldMatchValidMessages() {
        String[] validMessages = {
                "feat: add new feature",
                "fix: resolve bug",
                "chore(init): scaffold project",
                "docs(readme): update documentation",
                "refactor(core): restructure code",
                "test: add unit tests",
                "ci: update pipeline",
                "build: update dependencies",
                "perf: improve performance",
                "style: format code",
                "revert: undo previous change",
                "feat!: breaking change",
                "fix(api)!: breaking fix"
        };

        for (String message : validMessages) {
            assertTrue(
                    Constants.CONVENTIONAL_COMMIT_PATTERN.matcher(message).matches(),
                    "Should match valid conventional commit: " + message
            );
        }
    }

    @Test
    void conventionalCommitPatternShouldRejectInvalidMessages() {
        String[] invalidMessages = {
                "Add new feature",
                "FEAT: uppercase type",
                "feat add missing colon",
                "invalid: unknown type",
                "feat:",
                "feat: ",
                ": missing type"
        };

        for (String message : invalidMessages) {
            assertFalse(
                    Constants.CONVENTIONAL_COMMIT_PATTERN.matcher(message).matches(),
                    "Should reject invalid conventional commit: " + message
            );
        }
    }
}
