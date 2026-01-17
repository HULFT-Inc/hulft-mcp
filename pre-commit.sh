#!/bin/bash

# Pre-commit hook for code quality checks
# Install: cp pre-commit.sh .git/hooks/pre-commit && chmod +x .git/hooks/pre-commit

set -e

echo "🔍 Running pre-commit checks..."

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo -e "${RED}✗ gradlew not found${NC}"
    exit 1
fi

# 1. Compile check
echo -e "\n${YELLOW}1. Compiling code...${NC}"
if ./gradlew compileJava compileTestJava --quiet; then
    echo -e "${GREEN}✓ Compilation successful${NC}"
else
    echo -e "${RED}✗ Compilation failed${NC}"
    exit 1
fi

# 2. Checkstyle
echo -e "\n${YELLOW}2. Running Checkstyle...${NC}"
if ./gradlew checkstyleMain checkstyleTest --quiet; then
    echo -e "${GREEN}✓ Checkstyle passed${NC}"
else
    echo -e "${RED}✗ Checkstyle violations found${NC}"
    echo "Run: ./gradlew checkstyleMain to see details"
    exit 1
fi

# 3. PMD
echo -e "\n${YELLOW}3. Running PMD...${NC}"
if ./gradlew pmdMain pmdTest --quiet; then
    echo -e "${GREEN}✓ PMD passed${NC}"
else
    echo -e "${RED}✗ PMD violations found${NC}"
    echo "Run: ./gradlew pmdMain to see details"
    exit 1
fi

# 4. Unit tests
echo -e "\n${YELLOW}4. Running unit tests...${NC}"
if ./gradlew test --quiet; then
    echo -e "${GREEN}✓ All tests passed${NC}"
else
    echo -e "${RED}✗ Tests failed${NC}"
    echo "Run: ./gradlew test to see details"
    exit 1
fi

# 5. Code coverage
echo -e "\n${YELLOW}5. Checking code coverage...${NC}"
if ./gradlew jacocoTestCoverageVerification --quiet; then
    echo -e "${GREEN}✓ Coverage threshold met${NC}"
else
    echo -e "${YELLOW}⚠ Coverage below threshold (continuing)${NC}"
fi

# 6. Architecture tests
echo -e "\n${YELLOW}6. Running architecture tests...${NC}"
if ./gradlew test --tests ArchitectureTest --quiet; then
    echo -e "${GREEN}✓ Architecture rules passed${NC}"
else
    echo -e "${RED}✗ Architecture violations found${NC}"
    exit 1
fi

echo -e "\n${GREEN}✅ All pre-commit checks passed!${NC}"
echo -e "Proceeding with commit...\n"

exit 0
