package com.smartbootstrapper.refactor;

import com.smartbootstrapper.exception.RefactorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PackageRefactorServiceTest {

    private PackageRefactorService service;

    @BeforeEach
    void setUp() {
        service = new PackageRefactorService();
    }

    @Test
    void shouldRefactorPackageDeclaration() {
        String source = """
                package com.old.service;

                public class MyService {
                }
                """;

        String result = service.refactorJavaFile(source, "com.old", "com.updated", "MyService.java");

        assertTrue(result.contains("package com.updated.service;"));
        assertFalse(result.contains("com.old"));
    }

    @Test
    void shouldRefactorImportStatements() {
        String source = """
                package com.other;

                import com.old.service.MyService;
                import com.old.repository.MyRepository;
                import java.util.List;

                public class Client {
                    private MyService service;
                }
                """;

        String result = service.refactorJavaFile(source, "com.old", "com.updated", "Client.java");

        assertTrue(result.contains("import com.updated.service.MyService;"));
        assertTrue(result.contains("import com.updated.repository.MyRepository;"));
        assertTrue(result.contains("import java.util.List;"));
        assertFalse(result.contains("import com.old"));
    }

    @Test
    void shouldNotRefactorUnrelatedPackages() {
        String source = """
                package com.different;

                import com.oldername.Something;

                public class Test {
                }
                """;

        String result = service.refactorJavaFile(source, "com.old", "com.updated", "Test.java");

        // Should not change "com.oldername" since it doesn't start with "com.old."
        assertTrue(result.contains("import com.oldername.Something;"));
    }

    @Test
    void shouldReturnOriginalWhenPackagesAreSame() {
        String source = """
                package com.example;

                public class Test {
                }
                """;

        String result = service.refactorJavaFile(source, "com.example", "com.example", "Test.java");

        assertEquals(source.trim(), result.trim());
    }

    @Test
    void shouldThrowExceptionForInvalidJava() {
        String invalidSource = """
                package com.old

                public class Test {
                    // missing semicolon above
                }
                """;

        assertThrows(RefactorException.class, () ->
                service.refactorJavaFile(invalidSource, "com.old", "com.updated", "Test.java"));
    }

    @Test
    void shouldValidateParseability() {
        String validSource = """
                package com.example;

                public class Valid {
                }
                """;

        String invalidSource = """
                package com.example

                public class Invalid {
                }
                """;

        assertTrue(service.canParse(validSource, "Valid.java"));
        assertFalse(service.canParse(invalidSource, "Invalid.java"));
    }

    @Test
    void shouldReturnParseErrors() {
        String invalidSource = """
                package com.example

                public class Test {
                }
                """;

        String errors = service.getParseErrors(invalidSource, "Test.java");

        assertFalse(errors.isEmpty());
    }

    @Test
    void shouldRefactorPackageNameCorrectly() {
        assertEquals("com.updated", service.refactorPackageName("com.old", "com.old", "com.updated"));
        assertEquals("com.updated.service", service.refactorPackageName("com.old.service", "com.old", "com.updated"));
        assertEquals("org.other", service.refactorPackageName("org.other", "com.old", "com.updated"));
        assertEquals("", service.refactorPackageName("", "com.old", "com.updated"));
        assertNull(service.refactorPackageName(null, "com.old", "com.updated"));
    }

    @Test
    void shouldHandleComplexRefactoring() {
        String source = """
                package com.old.controller;

                import com.old.service.UserService;
                import com.old.model.User;
                import com.old.repository.UserRepository;
                import org.springframework.web.bind.annotation.RestController;
                import java.util.List;

                @RestController
                public class UserController {
                    private final UserService service;

                    public UserController(UserService service) {
                        this.service = service;
                    }

                    public List<User> getUsers() {
                        return service.findAll();
                    }
                }
                """;

        String result = service.refactorJavaFile(source, "com.old", "com.newapp", "UserController.java");

        assertTrue(result.contains("package com.newapp.controller;"));
        assertTrue(result.contains("import com.newapp.service.UserService;"));
        assertTrue(result.contains("import com.newapp.model.User;"));
        assertTrue(result.contains("import com.newapp.repository.UserRepository;"));
        assertTrue(result.contains("import org.springframework.web.bind.annotation.RestController;"));
        assertTrue(result.contains("import java.util.List;"));
    }
}
