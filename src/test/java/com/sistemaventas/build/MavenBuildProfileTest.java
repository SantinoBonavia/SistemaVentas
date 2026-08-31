package com.sistemaventas.build;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MavenBuildProfileTest {

    private static final Path POM = Path.of("pom.xml");

    @Test
    void declaresJUnitForTheDefaultDeterministicTestLifecycle() throws IOException {
        String pom = Files.readString(POM);

        assertTrue(pom.contains("<artifactId>junit-jupiter</artifactId>"));
        assertTrue(pom.contains("<scope>test</scope>"));
    }

    @Test
    void confinesMysqlIntegrationRequirementsToTheOptInProfile() throws IOException {
        String pom = Files.readString(POM);

        assertTrue(pom.contains("<id>mysql-it</id>"));
        assertTrue(pom.contains("<artifactId>maven-failsafe-plugin</artifactId>"));
        assertTrue(pom.contains("TEST_DB_URL"));
        assertTrue(pom.contains("TEST_DB_USER"));
        assertTrue(pom.contains("TEST_DB_PASSWORD"));
        assertTrue(pom.contains("mysql-it requires TEST_DB_URL, TEST_DB_USER, and TEST_DB_PASSWORD."));
    }
}
