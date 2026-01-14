package com.smartbootstrapper.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {

    private InputValidator validator;

    @BeforeEach
    void setUp() {
        validator = new InputValidator();
    }

    // Group ID Tests

    @ParameterizedTest
    @ValueSource(strings = {"com.example", "org.mycompany", "io.github.user", "a.b.c"})
    void shouldAcceptValidGroupIds(String groupId) {
        InputValidator.ValidationResult result = validator.validateGroupId(groupId);
        assertTrue(result.valid(), "Should accept: " + groupId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Com.Example", "com-example", "com_example", ".com.example", "com.example.", "123.abc"})
    void shouldRejectInvalidGroupIds(String groupId) {
        InputValidator.ValidationResult result = validator.validateGroupId(groupId);
        assertFalse(result.valid(), "Should reject: " + groupId);
    }

    @Test
    void shouldRejectEmptyGroupId() {
        assertFalse(validator.validateGroupId("").valid());
        assertFalse(validator.validateGroupId("   ").valid());
        assertFalse(validator.validateGroupId(null).valid());
    }

    // Artifact ID Tests

    @ParameterizedTest
    @ValueSource(strings = {"my-app", "userservice", "my-cool-app", "app123"})
    void shouldAcceptValidArtifactIds(String artifactId) {
        InputValidator.ValidationResult result = validator.validateArtifactId(artifactId);
        assertTrue(result.valid(), "Should accept: " + artifactId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"My-App", "my_app", "my.app", "-myapp", "myapp-", "123app"})
    void shouldRejectInvalidArtifactIds(String artifactId) {
        InputValidator.ValidationResult result = validator.validateArtifactId(artifactId);
        assertFalse(result.valid(), "Should reject: " + artifactId);
    }

    // Package Name Tests

    @ParameterizedTest
    @ValueSource(strings = {"com.example.myapp", "org.company", "io.github.user.project"})
    void shouldAcceptValidPackageNames(String packageName) {
        InputValidator.ValidationResult result = validator.validatePackageName(packageName);
        assertTrue(result.valid(), "Should accept: " + packageName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"com.example.class", "org.if.then", "io.public.private"})
    void shouldRejectPackageNamesWithReservedWords(String packageName) {
        InputValidator.ValidationResult result = validator.validatePackageName(packageName);
        assertFalse(result.valid(), "Should reject (reserved word): " + packageName);
    }

    // Version Tests

    @ParameterizedTest
    @ValueSource(strings = {"1.0.0", "0.0.1-SNAPSHOT", "2.3.4-RC1", "1.2.3-beta"})
    void shouldAcceptValidVersions(String version) {
        InputValidator.ValidationResult result = validator.validateVersion(version);
        assertTrue(result.valid(), "Should accept: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.0", "v1.0.0", "1.0.0.0", "abc"})
    void shouldRejectInvalidVersions(String version) {
        InputValidator.ValidationResult result = validator.validateVersion(version);
        assertFalse(result.valid(), "Should reject: " + version);
    }

    // Spring Boot Version Tests

    @ParameterizedTest
    @ValueSource(strings = {"3.2.0", "3.1.5", "2.7.18"})
    void shouldAcceptValidSpringBootVersions(String version) {
        InputValidator.ValidationResult result = validator.validateSpringBootVersion(version);
        assertTrue(result.valid(), "Should accept: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.2", "3.2.0-SNAPSHOT", "3.2.0.RELEASE"})
    void shouldRejectInvalidSpringBootVersions(String version) {
        InputValidator.ValidationResult result = validator.validateSpringBootVersion(version);
        assertFalse(result.valid(), "Should reject: " + version);
    }

    // Java Version Tests

    @ParameterizedTest
    @ValueSource(strings = {"17", "21", "25"})
    void shouldAcceptValidJavaVersions(String version) {
        InputValidator.ValidationResult result = validator.validateJavaVersion(version);
        assertTrue(result.valid(), "Should accept: " + version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"8", "11", "16", "22", "23", "1.8"})
    void shouldRejectInvalidJavaVersions(String version) {
        InputValidator.ValidationResult result = validator.validateJavaVersion(version);
        assertFalse(result.valid(), "Should reject: " + version);
    }

    // Project Name Tests

    @Test
    void shouldAcceptValidProjectNames() {
        assertTrue(validator.validateProjectName("My Application").valid());
        assertTrue(validator.validateProjectName("User Service").valid());
        assertTrue(validator.validateProjectName("app").valid());
    }

    @Test
    void shouldRejectEmptyProjectNames() {
        assertFalse(validator.validateProjectName("").valid());
        assertFalse(validator.validateProjectName("   ").valid());
        assertFalse(validator.validateProjectName(null).valid());
    }

    @Test
    void shouldRejectTooLongProjectNames() {
        String longName = "a".repeat(101);
        assertFalse(validator.validateProjectName(longName).valid());
    }

    // Suggestion Tests

    @Test
    void shouldSuggestGroupId() {
        Optional<String> suggestion = validator.suggestGroupId("COM.Example");
        assertTrue(suggestion.isPresent());
        assertEquals("com.example", suggestion.get());

        suggestion = validator.suggestGroupId("mycompany");
        assertTrue(suggestion.isPresent());
        assertEquals("com.mycompany", suggestion.get());
    }

    @Test
    void shouldSuggestArtifactId() {
        Optional<String> suggestion = validator.suggestArtifactId("My_App");
        assertTrue(suggestion.isPresent());
        assertEquals("my-app", suggestion.get());

        suggestion = validator.suggestArtifactId("UserService");
        assertTrue(suggestion.isPresent());
        assertTrue(suggestion.get().matches("[a-z][a-z0-9-]*"));
    }
}
