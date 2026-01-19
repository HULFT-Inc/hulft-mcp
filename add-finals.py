#!/usr/bin/env python3
import re

file_path = "src/main/java/com/hulft/mcp/MCPServer.java"

with open(file_path, 'r') as f:
    content = f.read()

# Add final to method parameters
content = re.sub(r'\b(String|Context|Map<String, Object>|Object|List<[^>]+>|byte\[\]|int|boolean|long|double|float) (\w+)(\s*[,)])', r'final \1 \2\3', content)

# Add final to local variable declarations (but not reassignments)
# Match: Type varName = value
content = re.sub(r'^(\s+)(String|Map<String, Object>|List<[^>]+>|Object|Javalin|byte\[\]|int|boolean|long|double|float|StringBuilder) (\w+) = ', r'\1final \2 \3 = ', content, flags=re.MULTILINE)

with open(file_path, 'w') as f:
    f.write(content)

print("Added final keywords to MCPServer.java")
