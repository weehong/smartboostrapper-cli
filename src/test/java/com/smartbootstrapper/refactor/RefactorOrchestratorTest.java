package com.smartbootstrapper.refactor;

import com.smartbootstrapper.model.RefactorResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RefactorOrchestratorTest {

    private RefactorOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new RefactorOrchestrator();
    }

    // ==================== Path Transformation Tests ====================

    @Test
    void shouldTransformMainJavaPath() {
        String oldPath = "src/main/java/com/example/myapp/service/UserService.java";
        String result = orchestrator.transformDestinationPath(oldPath, "com.example.myapp", "com.newcompany.newapp");

        assertEquals("src/main/java/com/newcompany/newapp/service/UserService.java", result);
    }

    @Test
    void shouldTransformTestJavaPath() {
        String oldPath = "src/test/java/com/example/myapp/service/UserServiceTest.java";
        String result = orchestrator.transformDestinationPath(oldPath, "com.example.myapp", "com.newcompany.newapp");

        assertEquals("src/test/java/com/newcompany/newapp/service/UserServiceTest.java", result);
    }

    @Test
    void shouldTransformRootPackagePath() {
        String oldPath = "src/main/java/com/example/myapp/Application.java";
        String result = orchestrator.transformDestinationPath(oldPath, "com.example.myapp", "com.newcompany.newapp");

        assertEquals("src/main/java/com/newcompany/newapp/Application.java", result);
    }

    @Test
    void shouldNotTransformUnrelatedPath() {
        String oldPath = "src/main/java/com/other/package/SomeClass.java";
        String result = orchestrator.transformDestinationPath(oldPath, "com.example.myapp", "com.newcompany.newapp");

        assertEquals("src/main/java/com/other/package/SomeClass.java", result);
    }

    @Test
    void shouldNotTransformResourcePath() {
        String oldPath = "src/main/resources/application.properties";
        String result = orchestrator.transformDestinationPath(oldPath, "com.example.myapp", "com.newcompany.newapp");

        assertEquals("src/main/resources/application.properties", result);
    }

    @Test
    void shouldReturnOriginalWhenPackagesAreSame() {
        String oldPath = "src/main/java/com/example/myapp/Service.java";
        String result = orchestrator.transformDestinationPath(oldPath, "com.example.myapp", "com.example.myapp");

        assertEquals(oldPath, result);
    }

    @Test
    void shouldHandleNullPath() {
        String result = orchestrator.transformDestinationPath(null, "com.old", "com.updated");
        assertNull(result);
    }

    @Test
    void shouldHandleNullOldPackage() {
        String oldPath = "src/main/java/com/example/Service.java";
        String result = orchestrator.transformDestinationPath(oldPath, null, "com.updated");
        assertEquals(oldPath, result);
    }

    @Test
    void shouldHandleNullNewPackage() {
        String oldPath = "src/main/java/com/example/Service.java";
        String result = orchestrator.transformDestinationPath(oldPath, "com.old", null);
        assertEquals(oldPath, result);
    }

    @Test
    void shouldTransformDeepNestedPath() {
        String oldPath = "src/main/java/com/upmatches/app/shared/core/constants/AppConstants.java";
        String result = orchestrator.transformDestinationPath(oldPath, "com.upmatches.app", "com.upmatches.api");

        assertEquals("src/main/java/com/upmatches/api/shared/core/constants/AppConstants.java", result);
    }

    @Test
    void shouldTransformPathWithDifferentPackageDepths() {
        // Shorter new package
        String oldPath = "src/main/java/com/example/myapp/deep/nested/Service.java";
        String result = orchestrator.transformDestinationPath(oldPath, "com.example.myapp", "com.short");

        assertEquals("src/main/java/com/short/deep/nested/Service.java", result);
    }

    @Test
    void shouldTransformPathWithLongerNewPackage() {
        // Longer new package
        String oldPath = "src/main/java/com/old/Service.java";
        String result = orchestrator.transformDestinationPath(oldPath, "com.old", "com.longer.package.name");

        assertEquals("src/main/java/com/longer/package/name/Service.java", result);
    }

    // ==================== Full Refactoring with Path Transformation Tests ====================

    @Test
    void shouldRefactorFilesWithPathTransformation() {
        String javaSource =
                "package com.old.service;\n" +
                "\n" +
                "import com.old.model.User;\n" +
                "\n" +
                "public class UserService {\n" +
                "    private User user;\n" +
                "}\n";

        Map<String, byte[]> files = new HashMap<>();
        files.put("src/main/java/com/old/service/UserService.java", javaSource.getBytes(StandardCharsets.UTF_8));

        RefactorResult result = orchestrator.refactorFilesWithPathTransformation(files, "com.old", "com.updated");

        assertTrue(result.isSuccess());
        assertTrue(result.hasRefactoredFilesWithContent());

        Map<String, byte[]> refactoredFiles = result.getRefactoredFilesWithContent();

        // Check that the path was transformed
        assertTrue(refactoredFiles.containsKey("src/main/java/com/updated/service/UserService.java"));
        assertFalse(refactoredFiles.containsKey("src/main/java/com/old/service/UserService.java"));

        // Check that the content was refactored
        String refactoredContent = new String(refactoredFiles.get("src/main/java/com/updated/service/UserService.java"), StandardCharsets.UTF_8);
        assertTrue(refactoredContent.contains("package com.updated.service;"));
        assertTrue(refactoredContent.contains("import com.updated.model.User;"));
    }

    @Test
    void shouldRefactorMultipleFilesWithPathTransformation() {
        String serviceSource =
                "package com.example.app.service;\n" +
                "\n" +
                "public class MyService {\n" +
                "}\n";

        String controllerSource =
                "package com.example.app.controller;\n" +
                "\n" +
                "import com.example.app.service.MyService;\n" +
                "\n" +
                "public class MyController {\n" +
                "    private MyService service;\n" +
                "}\n";

        Map<String, byte[]> files = new HashMap<>();
        files.put("src/main/java/com/example/app/service/MyService.java", serviceSource.getBytes(StandardCharsets.UTF_8));
        files.put("src/main/java/com/example/app/controller/MyController.java", controllerSource.getBytes(StandardCharsets.UTF_8));

        RefactorResult result = orchestrator.refactorFilesWithPathTransformation(files, "com.example.app", "com.newcompany.newapp");

        assertTrue(result.isSuccess());
        assertEquals(2, result.getJavaFilesRefactored());

        Map<String, byte[]> refactoredFiles = result.getRefactoredFilesWithContent();

        // Check paths were transformed
        assertTrue(refactoredFiles.containsKey("src/main/java/com/newcompany/newapp/service/MyService.java"));
        assertTrue(refactoredFiles.containsKey("src/main/java/com/newcompany/newapp/controller/MyController.java"));

        // Check content was refactored
        String controllerContent = new String(refactoredFiles.get("src/main/java/com/newcompany/newapp/controller/MyController.java"), StandardCharsets.UTF_8);
        assertTrue(controllerContent.contains("package com.newcompany.newapp.controller;"));
        assertTrue(controllerContent.contains("import com.newcompany.newapp.service.MyService;"));
    }

    @Test
    void shouldHandleMixedFileTypes() {
        String javaSource =
                "package com.old.service;\n" +
                "\n" +
                "public class Service {\n" +
                "}\n";

        String propertiesSource = "app.package=com.old.service\n";

        Map<String, byte[]> files = new HashMap<>();
        files.put("src/main/java/com/old/service/Service.java", javaSource.getBytes(StandardCharsets.UTF_8));
        files.put("src/main/resources/application.properties", propertiesSource.getBytes(StandardCharsets.UTF_8));

        RefactorResult result = orchestrator.refactorFilesWithPathTransformation(files, "com.old", "com.updated");

        assertTrue(result.isSuccess());
        assertEquals(1, result.getJavaFilesRefactored());
        assertEquals(1, result.getPropertiesFilesRefactored());

        Map<String, byte[]> refactoredFiles = result.getRefactoredFilesWithContent();

        // Java path should be transformed
        assertTrue(refactoredFiles.containsKey("src/main/java/com/updated/service/Service.java"));

        // Properties path should NOT be transformed (it's in resources, not java)
        assertTrue(refactoredFiles.containsKey("src/main/resources/application.properties"));
    }

    @Test
    void shouldPreserveNonJavaFilesPath() {
        String xmlSource = "<beans><bean class=\"com.old.Service\"/></beans>";

        Map<String, byte[]> files = new HashMap<>();
        files.put("src/main/resources/beans.xml", xmlSource.getBytes(StandardCharsets.UTF_8));

        RefactorResult result = orchestrator.refactorFilesWithPathTransformation(files, "com.old", "com.updated");

        assertTrue(result.isSuccess());

        Map<String, byte[]> refactoredFiles = result.getRefactoredFilesWithContent();

        // XML path should NOT be transformed
        assertTrue(refactoredFiles.containsKey("src/main/resources/beans.xml"));
    }
}
