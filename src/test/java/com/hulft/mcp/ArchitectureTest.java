package com.hulft.mcp;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

public class ArchitectureTest {
    
    private final JavaClasses classes = new ClassFileImporter()
        .importPackages("com.hulft.mcp");
    
    @Test
    public void servicesShouldNotDependOnControllers() {
        noClasses()
            .that().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller")
            .check(classes);
    }
    
    @Test
    public void noClassesShouldUseSystemOutOrErr() {
        noClasses()
            .should().accessField("java.lang.System", "out")
            .orShould().accessField("java.lang.System", "err")
            .because("Use logging framework instead")
            .check(classes);
    }
    
    @Test
    public void fieldsShouldNotBePublic() {
        fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..mcp..")
            .and().areNotStatic()
            .and().areNotFinal()
            .should().notBePublic()
            .because("Fields should be private with getters/setters")
            .check(classes);
    }
    
    @Test
    public void utilityClassesShouldBeFinal() {
        classes()
            .that().haveSimpleNameEndingWith("Utils")
            .or().haveSimpleNameEndingWith("Helper")
            .should().beAnnotatedWith("final")
            .because("Utility classes should not be extended")
            .check(classes);
    }
    
    @Test
    public void constantsShouldBeStaticFinal() {
        fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..mcp..")
            .and().haveNameMatching("^[A-Z][A-Z0-9_]*$")
            .should().beStatic()
            .andShould().beFinal()
            .because("Constants should be static final")
            .check(classes);
    }
}