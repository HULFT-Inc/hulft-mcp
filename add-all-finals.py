#!/usr/bin/env python3
import re

file_path = "src/main/java/com/hulft/mcp/MCPServer.java"

with open(file_path, 'r') as f:
    content = f.read()

# Add final to variable declarations that don't have it
# Match patterns like: Type varName = value (but not if already has final)
patterns = [
    (r'(\s+)(List<[^>]+>) (\w+) = ', r'\1final \2 \3 = '),
    (r'(\s+)(Map<[^>]+>) (\w+) = ', r'\1final \2 \3 = '),
    (r'(\s+)(Set<[^>]+>) (\w+) = ', r'\1final \2 \3 = '),
    (r'(\s+)(String) (\w+) = ', r'\1final \2 \3 = '),
    (r'(\s+)(byte\[\]) (\w+) = ', r'\1final \2 \3 = '),
    (r'(\s+)(int) (\w+) = ', r'\1final \2 \3 = '),
    (r'(\s+)(boolean) (\w+) = ', r'\1final \2 \3 = '),
    (r'(\s+)(long) (\w+) = ', r'\1final \2 \3 = '),
    (r'(\s+)(double) (\w+) = ', r'\1final \2 \3 = '),
    (r'(\s+)(float) (\w+) = ', r'\1final \2 \3 = '),
]

for pattern, replacement in patterns:
    # Only replace if 'final' is not already present
    content = re.sub(r'(?<!final )' + pattern, replacement, content)

# Remove duplicate finals
content = re.sub(r'final\s+final\s+', 'final ', content)

with open(file_path, 'w') as f:
    f.write(content)

print("Added final keywords")
