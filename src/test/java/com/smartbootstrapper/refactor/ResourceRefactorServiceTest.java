package com.smartbootstrapper.refactor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceRefactorServiceTest {

    private ResourceRefactorService service;

    @BeforeEach
    void setUp() {
        service = new ResourceRefactorService();
    }

    @Test
    void shouldRefactorPropertiesFile() {
        String content = """
                spring.application.name=myapp
                spring.jpa.hibernate.ddl-auto=update
                app.base-package=com.old.service
                app.scan-packages=com.old.controller,com.old.repository
                """;

        String result = service.refactorPropertiesFile(content, "com.old", "com.new");

        assertTrue(result.contains("app.base-package=com.new.service"));
        assertTrue(result.contains("app.scan-packages=com.new.controller,com.new.repository"));
        assertTrue(result.contains("spring.application.name=myapp"));
    }

    @Test
    void shouldRefactorXmlFile() {
        String content = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans">
                    <context:component-scan base-package="com.old.service"/>
                    <bean class="com.old.config.AppConfig"/>
                </beans>
                """;

        String result = service.refactorXmlFile(content, "com.old", "com.new");

        assertTrue(result.contains("base-package=\"com.new.service\""));
        assertTrue(result.contains("class=\"com.new.config.AppConfig\""));
    }

    @Test
    void shouldNotChangeUnrelatedPackages() {
        String content = """
                app.package=com.older.service
                other.package=com.oldish.service
                """;

        String result = service.refactorPropertiesFile(content, "com.old", "com.new");

        assertTrue(result.contains("com.older.service"));
        assertTrue(result.contains("com.oldish.service"));
    }

    @Test
    void shouldHandlePackagePaths() {
        String content = """
                classpath:com/old/service/mapper.xml
                resource.path=com/old/templates
                """;

        String result = service.refactorPropertiesFile(content, "com.old", "com.new");

        assertTrue(result.contains("com/new/service/mapper.xml"));
        assertTrue(result.contains("com/new/templates"));
    }

    @Test
    void shouldReturnOriginalWhenPackagesAreSame() {
        String content = "app.package=com.example";

        String result = service.refactorPropertiesFile(content, "com.example", "com.example");

        assertEquals(content, result);
    }

    @Test
    void shouldHandleEmptyContent() {
        String result = service.refactorPropertiesFile("", "com.old", "com.new");

        assertEquals("", result);
    }

    @Test
    void shouldHandleNullContent() {
        String result = service.refactorPropertiesFile(null, "com.old", "com.new");

        assertNull(result);
    }

    @Test
    void shouldDetermineFileTypesCorrectly() {
        assertTrue(service.shouldRefactor("application.properties"));
        assertTrue(service.shouldRefactor("application.yml"));
        assertTrue(service.shouldRefactor("config.yaml"));
        assertTrue(service.shouldRefactor("beans.xml"));
        assertTrue(service.shouldRefactor("data.json"));
        assertFalse(service.shouldRefactor("image.png"));
        assertFalse(service.shouldRefactor("binary.class"));
        assertFalse(service.shouldRefactor(null));
    }

    @Test
    void shouldRefactorByFileType() {
        String propertiesContent = "app.package=com.old.service";
        String xmlContent = "<bean class=\"com.old.Config\"/>";

        String propsResult = service.refactorByFileType(propertiesContent, "app.properties", "com.old", "com.new");
        String xmlResult = service.refactorByFileType(xmlContent, "beans.xml", "com.old", "com.new");

        assertTrue(propsResult.contains("com.new.service"));
        assertTrue(xmlResult.contains("com.new.Config"));
    }
}
