# Code Quality Tools

## 🛠️ Tools Configured

### 1. Checkstyle (Google Style)
**Purpose:** Enforce coding standards and style consistency

**Configuration:** `config/checkstyle/checkstyle.xml`

**Rules:**
- Line length: 150 characters
- Method length: 150 lines
- File length: 2000 lines
- Parameter count: 7 max
- Naming conventions (camelCase, UPPER_CASE)
- Import organization
- Whitespace rules
- Modifier order

**Run:**
```bash
./gradlew checkstyleMain checkstyleTest
```

**Report:** `build/reports/checkstyle/main.html`

---

### 2. PMD (Static Analysis)
**Purpose:** Detect code smells, bugs, and performance issues

**Configuration:** `config/pmd/ruleset.xml`

**Categories:**
- Best Practices
- Code Style
- Design
- Error Prone
- Performance
- Security

**Rules:**
- Cyclomatic Complexity: 15 max
- NPath Complexity: 200 max
- Unused variables/imports
- Empty catch blocks
- Inefficient string operations

**Run:**
```bash
./gradlew pmdMain pmdTest
```

**Report:** `build/reports/pmd/main.html`

---

### 3. JaCoCo (Code Coverage)
**Purpose:** Measure test coverage

**Configuration:** `build.gradle`

**Thresholds:**
- Minimum coverage: 70%
- Excludes: Main class

**Run:**
```bash
./gradlew test jacocoTestReport
./gradlew jacocoTestCoverageVerification
```

**Report:** `build/reports/jacoco/test/html/index.html`

---

### 4. ArchUnit (Architecture Tests)
**Purpose:** Enforce architectural rules

**Tests:** `src/test/java/com/hulft/mcp/ArchitectureTest.java`

**Rules:**
- No System.out/err usage (use logging)
- No generic exceptions
- Fields should be private
- Constants should be static final
- Utility classes should be final
- AWS SDK encapsulation
- Test classes in test package

**Run:**
```bash
./gradlew test --tests ArchitectureTest
```

---

## 🚀 Quick Start

### Install Pre-commit Hook
```bash
cp pre-commit.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

### Run All Quality Checks
```bash
./quality-check.sh
```

### Run Individual Checks
```bash
# Checkstyle
./gradlew checkstyleMain

# PMD
./gradlew pmdMain

# Tests + Coverage
./gradlew test jacocoTestReport

# Architecture
./gradlew test --tests ArchitectureTest
```

---

## 📊 Reports

After running checks, view reports:

```bash
# Open all reports
open build/reports/tests/test/index.html
open build/reports/jacoco/test/html/index.html
open build/reports/checkstyle/main.html
open build/reports/pmd/main.html
```

---

## 🔧 Configuration

### Adjust Checkstyle Rules
Edit: `config/checkstyle/checkstyle.xml`

Example - Change line length:
```xml
<module name="LineLength">
    <property name="max" value="120"/>
</module>
```

### Adjust PMD Rules
Edit: `config/pmd/ruleset.xml`

Example - Change complexity:
```xml
<rule ref="category/java/design.xml/CyclomaticComplexity">
    <properties>
        <property name="methodReportLevel" value="10"/>
    </properties>
</rule>
```

### Adjust Coverage Threshold
Edit: `build.gradle`

```groovy
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80 // 80%
            }
        }
    }
}
```

---

## 🎯 Best Practices

### Before Committing
1. Run `./quality-check.sh`
2. Fix all violations
3. Ensure tests pass
4. Check coverage report

### During Development
1. Run Checkstyle frequently
2. Fix PMD warnings
3. Write tests for new code
4. Keep methods small (<150 lines)

### Code Review Checklist
- [ ] All quality checks pass
- [ ] Coverage > 70%
- [ ] No Checkstyle violations
- [ ] No PMD warnings
- [ ] Architecture rules followed
- [ ] Tests added for new features

---

## 🐛 Common Issues

### Checkstyle: Line too long
**Fix:** Break line or add to ignore pattern

### PMD: Cyclomatic complexity
**Fix:** Extract methods, simplify logic

### Coverage: Below threshold
**Fix:** Add unit tests

### ArchUnit: System.out usage
**Fix:** Use `log.info()` instead

---

## 📈 Metrics

### Current Status
- **Checkstyle:** Configured ✅
- **PMD:** Configured ✅
- **JaCoCo:** 70% threshold ✅
- **ArchUnit:** 10 rules ✅
- **Pre-commit:** Automated ✅

### Goals
- [ ] 80% code coverage
- [ ] Zero Checkstyle violations
- [ ] Zero PMD warnings
- [ ] All ArchUnit rules passing

---

## 🔗 Resources

- [Checkstyle Docs](https://checkstyle.org/)
- [PMD Rules](https://pmd.github.io/latest/pmd_rules_java.html)
- [JaCoCo Guide](https://www.jacoco.org/jacoco/trunk/doc/)
- [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html)

---

## 🚦 CI/CD Integration

### GitHub Actions Example
```yaml
name: Quality Checks

on: [push, pull_request]

jobs:
  quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run Quality Checks
        run: ./quality-check.sh
      - name: Upload Coverage
        uses: codecov/codecov-action@v2
        with:
          files: ./build/reports/jacoco/test/jacocoTestReport.xml
```

### Jenkins Pipeline Example
```groovy
pipeline {
    agent any
    stages {
        stage('Quality Checks') {
            steps {
                sh './quality-check.sh'
            }
        }
        stage('Publish Reports') {
            steps {
                publishHTML([
                    reportDir: 'build/reports/tests/test',
                    reportFiles: 'index.html',
                    reportName: 'Test Report'
                ])
                jacoco()
            }
        }
    }
}
```
