package com.smartbootstrapper.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ManifestTest {

    @Test
    void shouldCreateManifestWithEntries() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc1234", "src/A.java", "src/A.java"),
                new ManifestEntry("def5678", "src/B.java", "src/B.java")
        );

        Manifest manifest = new Manifest(entries, "/path/to/repo");

        assertEquals(2, manifest.size());
        assertEquals("/path/to/repo", manifest.getSourceRepositoryPath());
        assertFalse(manifest.isEmpty());
    }

    @Test
    void shouldReturnUniqueCommitHashes() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc1234", "src/A.java", "src/A.java"),
                new ManifestEntry("abc1234", "src/B.java", "src/B.java"),
                new ManifestEntry("def5678", "src/C.java", "src/C.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        Set<String> uniqueCommits = manifest.getUniqueCommitHashes();

        assertEquals(2, uniqueCommits.size());
        assertTrue(uniqueCommits.contains("abc1234"));
        assertTrue(uniqueCommits.contains("def5678"));
    }

    @Test
    void shouldFindDuplicateDestinations() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc1234", "src/A.java", "src/Same.java"),
                new ManifestEntry("def5678", "src/B.java", "src/Same.java"),
                new ManifestEntry("ghi9012", "src/C.java", "src/Unique.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        List<String> duplicates = manifest.findDuplicateDestinations();

        assertEquals(1, duplicates.size());
        assertEquals("src/Same.java", duplicates.get(0));
    }

    @Test
    void shouldFilterJavaEntries() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "src/A.java", "src/A.java"),
                new ManifestEntry("abc", "src/B.properties", "src/B.properties"),
                new ManifestEntry("abc", "src/C.java", "src/C.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        List<ManifestEntry> javaEntries = manifest.getJavaEntries();

        assertEquals(2, javaEntries.size());
        assertTrue(javaEntries.stream().allMatch(e -> e.getSourcePath().endsWith(".java")));
    }

    @Test
    void shouldFilterPropertiesEntries() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "app.properties", "app.properties"),
                new ManifestEntry("abc", "app.yml", "app.yml"),
                new ManifestEntry("abc", "app.yaml", "app.yaml"),
                new ManifestEntry("abc", "Test.java", "Test.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        List<ManifestEntry> propEntries = manifest.getPropertiesEntries();

        assertEquals(3, propEntries.size());
    }

    @Test
    void shouldFilterXmlEntries() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "beans.xml", "beans.xml"),
                new ManifestEntry("abc", "pom.xml", "pom.xml"),
                new ManifestEntry("abc", "Test.java", "Test.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        List<ManifestEntry> xmlEntries = manifest.getXmlEntries();

        assertEquals(2, xmlEntries.size());
    }

    @Test
    void shouldReturnImmutableEntries() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "src/A.java", "src/A.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");

        assertThrows(UnsupportedOperationException.class, () ->
                manifest.getEntries().add(new ManifestEntry("def", "B.java", "B.java")));
    }

    // ==================== Base Package Detection Tests ====================
    // NOTE: detectBasePackage() uses SOURCE paths (not destination paths) because
    // the actual Java file content contains package declarations matching the source structure.

    @Test
    void shouldDetectBasePackageFromSingleFile() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "src/main/java/com/example/app/service/UserService.java", "dest/UserService.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        Optional<String> basePackage = manifest.detectBasePackage();

        assertTrue(basePackage.isPresent());
        assertEquals("com.example.app.service", basePackage.get());
    }

    @Test
    void shouldDetectCommonBasePackageFromMultipleFiles() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "src/main/java/com/example/app/service/UserService.java", "dest/UserService.java"),
                new ManifestEntry("abc", "src/main/java/com/example/app/controller/UserController.java", "dest/UserController.java"),
                new ManifestEntry("abc", "src/main/java/com/example/app/model/User.java", "dest/User.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        Optional<String> basePackage = manifest.detectBasePackage();

        assertTrue(basePackage.isPresent());
        assertEquals("com.example.app", basePackage.get());
    }

    @Test
    void shouldDetectBasePackageFromDeepNestedPaths() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "src/main/java/com/upmatches/app/shared/core/constants/AppConstants.java", "dest/A.java"),
                new ManifestEntry("abc", "src/main/java/com/upmatches/app/shared/core/utils/StringUtils.java", "dest/B.java"),
                new ManifestEntry("abc", "src/main/java/com/upmatches/app/service/UserService.java", "dest/C.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        Optional<String> basePackage = manifest.detectBasePackage();

        assertTrue(basePackage.isPresent());
        assertEquals("com.upmatches.app", basePackage.get());
    }

    @Test
    void shouldDetectBasePackageFromTestFiles() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "src/test/java/com/example/app/service/UserServiceTest.java", "dest/Test.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        Optional<String> basePackage = manifest.detectBasePackage();

        assertTrue(basePackage.isPresent());
        assertEquals("com.example.app.service", basePackage.get());
    }

    @Test
    void shouldDetectBasePackageFromMixedMainAndTestFiles() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "src/main/java/com/example/app/service/UserService.java", "dest/Service.java"),
                new ManifestEntry("abc", "src/test/java/com/example/app/service/UserServiceTest.java", "dest/Test.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        Optional<String> basePackage = manifest.detectBasePackage();

        assertTrue(basePackage.isPresent());
        assertEquals("com.example.app.service", basePackage.get());
    }

    @Test
    void shouldReturnEmptyWhenNoJavaFiles() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "src/main/resources/application.properties", "dest/application.properties"),
                new ManifestEntry("abc", "src/main/resources/beans.xml", "dest/beans.xml")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        Optional<String> basePackage = manifest.detectBasePackage();

        assertFalse(basePackage.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenNoStandardSourceRoot() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "com/example/app/Service.java", "dest/Service.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        Optional<String> basePackage = manifest.detectBasePackage();

        // No src/main/java or src/test/java prefix, so can't extract package
        assertFalse(basePackage.isPresent());
    }

    @Test
    void shouldReturnEmptyForEmptyManifest() {
        Manifest manifest = new Manifest(Arrays.asList(), "/repo");
        Optional<String> basePackage = manifest.detectBasePackage();

        assertFalse(basePackage.isPresent());
    }

    @Test
    void shouldIgnoreNonJavaFilesWhenDetectingPackage() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "src/main/java/com/example/app/service/UserService.java", "dest/UserService.java"),
                new ManifestEntry("abc", "src/main/resources/application.properties", "dest/application.properties"),
                new ManifestEntry("abc", "src/main/resources/beans.xml", "dest/beans.xml")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        Optional<String> basePackage = manifest.detectBasePackage();

        assertTrue(basePackage.isPresent());
        assertEquals("com.example.app.service", basePackage.get());
    }

    @Test
    void shouldHandleFilesWithNoCommonPackage() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "src/main/java/com/company1/app/Service.java", "dest/A.java"),
                new ManifestEntry("abc", "src/main/java/org/company2/app/Service.java", "dest/B.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        Optional<String> basePackage = manifest.detectBasePackage();

        // No common package between com.company1 and org.company2
        assertFalse(basePackage.isPresent());
    }

    @Test
    void shouldDetectSingleSegmentCommonPackage() {
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "src/main/java/com/app1/Service.java", "dest/A.java"),
                new ManifestEntry("abc", "src/main/java/com/app2/Service.java", "dest/B.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        Optional<String> basePackage = manifest.detectBasePackage();

        assertTrue(basePackage.isPresent());
        assertEquals("com", basePackage.get());
    }

    @Test
    void shouldDetectBasePackageFromSourcePathsMatchingFileContent() {
        // This test verifies the real-world scenario: source paths contain the actual
        // package structure that matches the Java file content, while destination paths
        // may specify a different target structure
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "src/main/java/com/upmatches/app/service/UserService.java",
                                  "src/main/java/com/acme/payment/service/UserService.java"),
                new ManifestEntry("abc", "src/main/java/com/upmatches/app/model/User.java",
                                  "src/main/java/com/acme/payment/model/User.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");
        Optional<String> basePackage = manifest.detectBasePackage();

        // Should detect from SOURCE paths (com.upmatches.app), not destination paths
        assertTrue(basePackage.isPresent());
        assertEquals("com.upmatches.app", basePackage.get());
    }

    @Test
    void shouldDetectDestinationBasePackageSeparateFromSourceBasePackage() {
        // Real-world scenario: source and destination paths have different package structures
        // - Source paths: com.upmatches.app (actual package in file content)
        // - Destination paths: com.example.myapp (intermediate target structure in manifest)
        List<ManifestEntry> entries = Arrays.asList(
                new ManifestEntry("abc", "src/main/java/com/upmatches/app/service/UserService.java",
                                  "src/main/java/com/example/myapp/service/UserService.java"),
                new ManifestEntry("abc", "src/main/java/com/upmatches/app/model/User.java",
                                  "src/main/java/com/example/myapp/model/User.java")
        );

        Manifest manifest = new Manifest(entries, "/repo");

        // detectBasePackage() returns source package (for content refactoring)
        Optional<String> sourcePackage = manifest.detectBasePackage();
        assertTrue(sourcePackage.isPresent());
        assertEquals("com.upmatches.app", sourcePackage.get());

        // detectDestinationBasePackage() returns destination package (for path transformation)
        Optional<String> destPackage = manifest.detectDestinationBasePackage();
        assertTrue(destPackage.isPresent());
        assertEquals("com.example.myapp", destPackage.get());
    }
}
