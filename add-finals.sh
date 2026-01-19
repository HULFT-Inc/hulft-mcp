#!/bin/bash
# Add final keywords to variables that are never reassigned

cd /Users/drewstoneburger/repos/hulft-mcp

# Get list of Java files
FILES=$(find src/main/java/com/hulft/mcp -name "*.java" -not -name "MCPServer.java")

for file in $FILES; do
    echo "Processing: $file"
    
    # Backup
    cp "$file" "$file.bak"
    
    # Add final to method parameters (simple cases)
    sed -i '' 's/public \([A-Za-z<>, ]*\) \([a-z][a-zA-Z]*\)(\([A-Za-z<>, ]*\) \([a-z][a-zA-Z]*\))/public \1 \2(final \3 \4)/g' "$file"
    sed -i '' 's/private \([A-Za-z<>, ]*\) \([a-z][a-zA-Z]*\)(\([A-Za-z<>, ]*\) \([a-z][a-zA-Z]*\))/private \1 \2(final \3 \4)/g' "$file"
    
    # Add final to local variable declarations (simple cases)
    sed -i '' 's/^\([[:space:]]*\)\([A-Z][a-zA-Z<>, ]*\) \([a-z][a-zA-Z]*\) = /\1final \2 \3 = /g' "$file"
    sed -i '' 's/^\([[:space:]]*\)int \([a-z][a-zA-Z]*\) = /\1final int \2 = /g' "$file"
    sed -i '' 's/^\([[:space:]]*\)String \([a-z][a-zA-Z]*\) = /\1final String \2 = /g' "$file"
    sed -i '' 's/^\([[:space:]]*\)boolean \([a-z][a-zA-Z]*\) = /\1final boolean \2 = /g' "$file"
    
    # Check if it compiles
    if ! ./gradlew compileJava -q 2>&1 | grep -q "error:"; then
        echo "✓ $file updated successfully"
        rm "$file.bak"
    else
        echo "✗ $file caused errors, reverting"
        mv "$file.bak" "$file"
    fi
done

echo "Done!"
