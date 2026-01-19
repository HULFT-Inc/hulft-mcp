#!/bin/bash
# Add final keywords to method parameters and local variables in MCPServer.java

FILE="src/main/java/com/hulft/mcp/MCPServer.java"

# Backup
cp "$FILE" "$FILE.bak"

# Add final to method parameters
sed -i '' 's/\(public\|private\|protected\) \(static \)\?\([a-zA-Z<>_,. ]*\) \([a-zA-Z]*\)(\([^)]*\)String\[\] args/\1 \2\3 \4(final String[] args/g' "$FILE"
sed -i '' 's/\(public\|private\|protected\) \(static \)\?\([a-zA-Z<>_,. ]*\) \([a-zA-Z]*\)(\([^)]*\)Context ctx/\1 \2\3 \4(\5final Context ctx/g' "$FILE"
sed -i '' 's/\(public\|private\|protected\) \(static \)\?\([a-zA-Z<>_,. ]*\) \([a-zA-Z]*\)(\([^)]*\)String \([a-zA-Z]*\),/\1 \2\3 \4(\5final String \6,/g' "$FILE"
sed -i '' 's/\(public\|private\|protected\) \(static \)\?\([a-zA-Z<>_,. ]*\) \([a-zA-Z]*\)(\([^)]*\)String \([a-zA-Z]*\))/\1 \2\3 \4(\5final String \6)/g' "$FILE"
sed -i '' 's/\(public\|private\|protected\) \(static \)\?\([a-zA-Z<>_,. ]*\) \([a-zA-Z]*\)(\([^)]*\)Map<String, Object> \([a-zA-Z]*\)/\1 \2\3 \4(\5final Map<String, Object> \6/g' "$FILE"
sed -i '' 's/\(public\|private\|protected\) \(static \)\?\([a-zA-Z<>_,. ]*\) \([a-zA-Z]*\)(\([^)]*\)Object \([a-zA-Z]*\)/\1 \2\3 \4(\5final Object \6/g' "$FILE"

# Add final to local variables
sed -i '' 's/^\([[:space:]]*\)String \([a-zA-Z]*\) = /\1final String \2 = /g' "$FILE"
sed -i '' 's/^\([[:space:]]*\)Map<String, Object> \([a-zA-Z]*\) = /\1final Map<String, Object> \2 = /g' "$FILE"
sed -i '' 's/^\([[:space:]]*\)List<[^>]*> \([a-zA-Z]*\) = /\1final List<\2> \3 = /g' "$FILE"
sed -i '' 's/^\([[:space:]]*\)Javalin \([a-zA-Z]*\) = /\1final Javalin \2 = /g' "$FILE"
sed -i '' 's/^\([[:space:]]*\)Object \([a-zA-Z]*\) = /\1final Object \2 = /g' "$FILE"

echo "Done! Check $FILE"
