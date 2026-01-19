#!/bin/bash
# Auto-fix PMD violations

cd /Users/drewstoneburger/repos/hulft-mcp

echo "Fixing LocalVariableCouldBeFinal violations..."

# Add final to common local variable patterns
sed -i '' 's/^\([[:space:]]*\)String \([a-zA-Z][a-zA-Z0-9]*\) = /\1final String \2 = /g' src/main/java/com/hulft/mcp/MCPServer.java
sed -i '' 's/^\([[:space:]]*\)Map<\([^>]*\)> \([a-zA-Z][a-zA-Z0-9]*\) = /\1final Map<\2> \3 = /g' src/main/java/com/hulft/mcp/MCPServer.java
sed -i '' 's/^\([[:space:]]*\)List<\([^>]*\)> \([a-zA-Z][a-zA-Z0-9]*\) = /\1final List<\2> \3 = /g' src/main/java/com/hulft/mcp/MCPServer.java
sed -i '' 's/^\([[:space:]]*\)int \([a-zA-Z][a-zA-Z0-9]*\) = /\1final int \2 = /g' src/main/java/com/hulft/mcp/MCPServer.java
sed -i '' 's/^\([[:space:]]*\)boolean \([a-zA-Z][a-zA-Z0-9]*\) = /\1final boolean \2 = /g' src/main/java/com/hulft/mcp/MCPServer.java
sed -i '' 's/^\([[:space:]]*\)float \([a-zA-Z][a-zA-Z0-9]*\) = /\1final float \2 = /g' src/main/java/com/hulft/mcp/MCPServer.java
sed -i '' 's/^\([[:space:]]*\)byte\[\] \([a-zA-Z][a-zA-Z0-9]*\) = /\1final byte[] \2 = /g' src/main/java/com/hulft/mcp/MCPServer.java

echo "Testing compilation..."
./gradlew compileJava -q

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
    echo "Checking remaining violations..."
    ./gradlew pmdMain 2>&1 | grep "PMD rule violations"
else
    echo "✗ Compilation failed - reverting"
    git checkout src/main/java/com/hulft/mcp/MCPServer.java
fi
