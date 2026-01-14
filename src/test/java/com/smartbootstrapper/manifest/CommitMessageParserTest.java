package com.smartbootstrapper.manifest;

import com.smartbootstrapper.exception.ManifestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommitMessageParserTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldParseSingleCommitMessage() throws IOException {
        String content = """
                1. d3d5969bd815e3b89a66182d7901719a5276e4a5

                chore(init): bootstrap Spring Boot project
                - add git configuration (.gitattributes, .gitignore)
                - add maven wrapper (mvnw, mvnw.cmd) with version 3.3.4
                """;

        Path commitMdPath = tempDir.resolve("COMMIT.md");
        Files.writeString(commitMdPath, content);

        CommitMessageParser parser = CommitMessageParser.parse(commitMdPath);

        assertEquals(1, parser.size());
        assertTrue(parser.hasMessage(1));
        assertEquals("chore(init): bootstrap Spring Boot project\n" +
                "- add git configuration (.gitattributes, .gitignore)\n" +
                "- add maven wrapper (mvnw, mvnw.cmd) with version 3.3.4",
                parser.getMessage(1).orElseThrow());
    }

    @Test
    void shouldParseMultipleCommitMessages() throws IOException {
        String content = """
                1. d3d5969bd815e3b89a66182d7901719a5276e4a5

                chore(init): bootstrap Spring Boot project
                - add git configuration

                ---
                2. ef5eb38b3d3385110a8f6802cfed07324699b071

                ci: add gitlab pipeline
                - add gitlab-ci.yaml

                ---
                3. fc14b4123e206a888e42b863f64045e67286f96a

                feat(logging): add logging
                """;

        Path commitMdPath = tempDir.resolve("COMMIT.md");
        Files.writeString(commitMdPath, content);

        CommitMessageParser parser = CommitMessageParser.parse(commitMdPath);

        assertEquals(3, parser.size());
        assertTrue(parser.hasMessage(1));
        assertTrue(parser.hasMessage(2));
        assertTrue(parser.hasMessage(3));

        assertTrue(parser.getMessage(1).orElseThrow().startsWith("chore(init):"));
        assertTrue(parser.getMessage(2).orElseThrow().startsWith("ci:"));
        assertTrue(parser.getMessage(3).orElseThrow().startsWith("feat(logging):"));
    }

    @Test
    void shouldReturnEmptyOptionalForNonExistentSequence() throws IOException {
        String content = """
                1. abc1234

                First commit message
                """;

        Path commitMdPath = tempDir.resolve("COMMIT.md");
        Files.writeString(commitMdPath, content);

        CommitMessageParser parser = CommitMessageParser.parse(commitMdPath);

        assertTrue(parser.getMessage(1).isPresent());
        assertTrue(parser.getMessage(2).isEmpty());
        assertTrue(parser.getMessage(99).isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenGetMessageOrThrowFails() throws IOException {
        String content = """
                1. abc1234

                First commit message
                """;

        Path commitMdPath = tempDir.resolve("COMMIT.md");
        Files.writeString(commitMdPath, content);

        CommitMessageParser parser = CommitMessageParser.parse(commitMdPath);

        assertThrows(ManifestException.class, () -> parser.getMessageOrThrow(99));
    }

    @Test
    void shouldThrowExceptionForNonExistentFile() {
        Path nonExistent = tempDir.resolve("does-not-exist.md");

        assertThrows(ManifestException.class, () ->
                CommitMessageParser.parse(nonExistent));
    }

    @Test
    void shouldHandleEmptyFile() throws IOException {
        Path commitMdPath = tempDir.resolve("COMMIT.md");
        Files.writeString(commitMdPath, "");

        CommitMessageParser parser = CommitMessageParser.parse(commitMdPath);

        assertEquals(0, parser.size());
    }

    @Test
    void shouldHandleContentWithoutDelimiters() throws IOException {
        String content = """
                1. abc1234

                Single commit without delimiter at end
                - detail 1
                - detail 2
                """;

        Path commitMdPath = tempDir.resolve("COMMIT.md");
        Files.writeString(commitMdPath, content);

        CommitMessageParser parser = CommitMessageParser.parse(commitMdPath);

        assertEquals(1, parser.size());
        assertTrue(parser.getMessage(1).orElseThrow().contains("detail 1"));
    }

    @Test
    void shouldParseContentUsingStaticMethod() {
        String content = """
                1. abc1234

                First message

                ---
                2. def5678

                Second message
                """;

        Map<Integer, String> messages = CommitMessageParser.parseContent(content);

        assertEquals(2, messages.size());
        assertEquals("First message", messages.get(1));
        assertEquals("Second message", messages.get(2));
    }

    @Test
    void shouldGetAllMessages() throws IOException {
        String content = """
                1. abc1234

                First

                ---
                2. def5678

                Second
                """;

        Path commitMdPath = tempDir.resolve("COMMIT.md");
        Files.writeString(commitMdPath, content);

        CommitMessageParser parser = CommitMessageParser.parse(commitMdPath);
        Map<Integer, String> allMessages = parser.getAllMessages();

        assertEquals(2, allMessages.size());
        assertTrue(allMessages.containsKey(1));
        assertTrue(allMessages.containsKey(2));
    }

    @Test
    void shouldHandleNonSequentialNumbers() throws IOException {
        String content = """
                1. abc1234

                First

                ---
                5. def5678

                Fifth

                ---
                10. aaa9012

                Tenth
                """;

        Path commitMdPath = tempDir.resolve("COMMIT.md");
        Files.writeString(commitMdPath, content);

        CommitMessageParser parser = CommitMessageParser.parse(commitMdPath);

        assertEquals(3, parser.size());
        assertTrue(parser.hasMessage(1));
        assertFalse(parser.hasMessage(2));
        assertTrue(parser.hasMessage(5));
        assertTrue(parser.hasMessage(10));
    }

    @Test
    void shouldSkipSectionsWithoutValidSequenceLine() throws IOException {
        String content = """
                Some header text

                ---
                1. abc1234

                Valid message

                ---
                Invalid section without sequence

                ---
                2. def5678

                Another valid message
                """;

        Path commitMdPath = tempDir.resolve("COMMIT.md");
        Files.writeString(commitMdPath, content);

        CommitMessageParser parser = CommitMessageParser.parse(commitMdPath);

        assertEquals(2, parser.size());
        assertTrue(parser.hasMessage(1));
        assertTrue(parser.hasMessage(2));
    }
}
