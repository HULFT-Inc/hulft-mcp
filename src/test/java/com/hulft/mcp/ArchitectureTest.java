package com.hulft.mcp;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

public class ArchitectureTest {
    
    private final JavaClasses classes = new ClassFileImporter()
        .importPackages("com.hulft.mcp");
    
    @Test
    public void noClassesShouldDependOnUpperPackages() {
        noClasses()
            .that().resideInAPackage("..mcp..")
            .should().dependOnClassesThat().resideInAPackage("..mcp..")
            .check(classes);
    }
    
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
            .should().accessTargetWhere(target ->
                target.getOwner().getName().equals("java.lang.System") &&
                (target.getName().equals("out") || target.getName().equals("err")))
            .because("Use logging framework instead")
            .check(classes);
    }
    
    @Test
    public void noClassesShouldThrowGenericExceptions() {
        noClasses()
            .should().throwExceptionOfType(Exception.class)
            .orShould().throwExceptionOfType(RuntimeException.class)
            .because("Throw specific exceptions")
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
    public void classesShouldNotDependOnAWSSDKDirectly() {
        // Allow only specific classes to use AWS SDK
        noClasses()
            .that().resideOutsideOfPackage("..mcp..")
            .should().dependOnClassesThat().resideInAPackage("software.amazon.awssdk..")
            .because("AWS SDK should be encapsulated")
            .check(classes);
    }
    
    @Test
    public void utilityClassesShouldBeFinal() {
        classes()
            .that().haveSimpleNameEndingWith("Utils")
            .or().haveSimpleNameEndingWith("Helper")
            .should().haveModifier(com.tngtech.archunit.core.domain.JavaModifier.FINAL)
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
    
    @Test
    public void testClassesShouldResideInTestPackage() {
        classes()
            .that().haveSimpleNameEndingWith("Test")
            .should().resideInAPackage("..test..")
            .check(classes);
    }
    
    @Test
    public void noClassesShouldAccessStandardStreamsDirectly() {
        noClasses()
            .should().accessClassesThat().haveFullyQualifiedName("java.io.PrintStream")
            .because("Use logging framework instead of System.out/err")
            .check(classes);
    }
}
