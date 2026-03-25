package com.mranalizer.domain.service.project;

import com.mranalizer.domain.model.ChangedFile;
import com.mranalizer.domain.model.project.DetectionPatterns;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactDetectorTest {

    private ArtifactDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ArtifactDetector(new DetectionPatterns(
                List.of("*.feature", "*Steps.java", "*_steps.py", "*_steps.rb", "*.steps.ts", "*Steps.kt"),
                List.of("spec.md", "plan.md", "tasks.md", "research.md", "quickstart.md", "checklist.md")
        ));
    }

    private ChangedFile file(String path) {
        return new ChangedFile(path, 10, 5, "added");
    }

    @Nested
    class BddDetection {

        @Test
        void featureFile() {
            assertTrue(detector.hasBdd(List.of(file("src/test/resources/features/login.feature"))));
        }

        @Test
        void stepsJava() {
            assertTrue(detector.hasBdd(List.of(file("com/mranalizer/bdd/steps/LoginSteps.java"))));
        }

        @Test
        void stepsPython() {
            assertTrue(detector.hasBdd(List.of(file("tests/acceptance/login_steps.py"))));
        }

        @Test
        void stepsRuby() {
            assertTrue(detector.hasBdd(List.of(file("features/step_definitions/login_steps.rb"))));
        }

        @Test
        void stepsTypeScript() {
            assertTrue(detector.hasBdd(List.of(file("tests/login.steps.ts"))));
        }

        @Test
        void stepsKotlin() {
            assertTrue(detector.hasBdd(List.of(file("src/test/kotlin/LoginSteps.kt"))));
        }

        @Test
        void regularJavaFile_notBdd() {
            assertFalse(detector.hasBdd(List.of(file("src/main/java/Service.java"))));
        }

        @Test
        void regularTestFile_notBdd() {
            assertFalse(detector.hasBdd(List.of(file("src/test/java/ServiceTest.java"))));
        }

        @Test
        void emptyFiles() {
            assertFalse(detector.hasBdd(List.of()));
        }

        @Test
        void findBddFiles_returnsPaths() {
            List<ChangedFile> files = List.of(
                    file("src/test/features/login.feature"),
                    file("src/main/java/Service.java"),
                    file("steps/LoginSteps.java")
            );
            List<String> found = detector.findBddFiles(files);
            assertEquals(2, found.size());
            assertTrue(found.contains("src/test/features/login.feature"));
            assertTrue(found.contains("steps/LoginSteps.java"));
        }
    }

    @Nested
    class SddDetection {

        @Test
        void specMd() {
            assertTrue(detector.hasSdd(List.of(file("specs/001-mvp/spec.md"))));
        }

        @Test
        void planMd() {
            assertTrue(detector.hasSdd(List.of(file("specs/005/plan.md"))));
        }

        @Test
        void tasksMd() {
            assertTrue(detector.hasSdd(List.of(file("specs/005/tasks.md"))));
        }

        @Test
        void researchMd() {
            assertTrue(detector.hasSdd(List.of(file("specs/012/research.md"))));
        }

        @Test
        void readmeMd_notSdd() {
            assertFalse(detector.hasSdd(List.of(file("README.md"))));
        }

        @Test
        void regularCode_notSdd() {
            assertFalse(detector.hasSdd(List.of(file("src/main/java/Service.java"))));
        }

        @Test
        void emptyFiles() {
            assertFalse(detector.hasSdd(List.of()));
        }

        @Test
        void findSddFiles_returnsPaths() {
            List<ChangedFile> files = List.of(
                    file("specs/001/spec.md"),
                    file("specs/001/plan.md"),
                    file("src/main/java/Service.java"),
                    file("README.md")
            );
            List<String> found = detector.findSddFiles(files);
            assertEquals(2, found.size());
            assertTrue(found.contains("specs/001/spec.md"));
            assertTrue(found.contains("specs/001/plan.md"));
        }
    }

    @Nested
    class MixedDetection {

        @Test
        void prWithBothBddAndSdd() {
            List<ChangedFile> files = List.of(
                    file("src/test/features/login.feature"),
                    file("specs/005/spec.md"),
                    file("src/main/java/LoginService.java")
            );
            assertTrue(detector.hasBdd(files));
            assertTrue(detector.hasSdd(files));
        }

        @Test
        void prWithNeither() {
            List<ChangedFile> files = List.of(
                    file("src/main/java/Service.java"),
                    file("pom.xml")
            );
            assertFalse(detector.hasBdd(files));
            assertFalse(detector.hasSdd(files));
        }
    }

    @Nested
    class CustomPatterns {

        @Test
        void customBddPattern() {
            ArtifactDetector custom = new ArtifactDetector(new DetectionPatterns(
                    List.of("*.spec.js"),
                    List.of()
            ));
            assertTrue(custom.hasBdd(List.of(file("tests/login.spec.js"))));
            assertFalse(custom.hasBdd(List.of(file("tests/login.test.js"))));
        }

        @Test
        void customSddPattern() {
            ArtifactDetector custom = new ArtifactDetector(new DetectionPatterns(
                    List.of(),
                    List.of("design.md", "architecture.md")
            ));
            assertTrue(custom.hasSdd(List.of(file("docs/design.md"))));
            assertFalse(custom.hasSdd(List.of(file("docs/spec.md"))));
        }
    }
}
