#!/bin/bash

# Code Quality Check Script
# Run all quality checks and generate reports

set -e

echo "🔍 Running comprehensive code quality checks..."

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

FAILED=0

# Function to run check
run_check() {
    local name=$1
    local command=$2
    
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${YELLOW}Running: $name${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    
    if eval $command; then
        echo -e "${GREEN}✓ $name passed${NC}"
        return 0
    else
        echo -e "${RED}✗ $name failed${NC}"
        FAILED=$((FAILED + 1))
        return 1
    fi
}

# Clean build
echo -e "${YELLOW}Cleaning build...${NC}"
./gradlew clean --quiet

# 1. Compilation
run_check "Compilation" "./gradlew compileJava compileTestJava"

# 2. Checkstyle
run_check "Checkstyle (Main)" "./gradlew checkstyleMain"
run_check "Checkstyle (Test)" "./gradlew checkstyleTest"

# 3. PMD
run_check "PMD (Main)" "./gradlew pmdMain"
run_check "PMD (Test)" "./gradlew pmdTest"

# 4. Unit Tests
run_check "Unit Tests" "./gradlew test"

# 5. Architecture Tests
run_check "Architecture Tests" "./gradlew test --tests ArchitectureTest"

# 6. QA Test Suite
run_check "QA Test Suite" "./gradlew test --tests QATestSuite"

# 7. Code Coverage
run_check "Code Coverage Report" "./gradlew jacocoTestReport"
run_check "Coverage Verification" "./gradlew jacocoTestCoverageVerification" || true

# Generate reports
echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Generating Reports...${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Report locations
echo -e "\n${GREEN}📊 Reports Generated:${NC}"
echo -e "  • Test Results:    ${BLUE}build/reports/tests/test/index.html${NC}"
echo -e "  • Coverage:        ${BLUE}build/reports/jacoco/test/html/index.html${NC}"
echo -e "  • Checkstyle:      ${BLUE}build/reports/checkstyle/main.html${NC}"
echo -e "  • PMD:             ${BLUE}build/reports/pmd/main.html${NC}"

# Summary
echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Summary${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ All checks passed!${NC}"
    exit 0
else
    echo -e "${RED}❌ $FAILED check(s) failed${NC}"
    exit 1
fi
