#!/usr/bin/env python3
import re

file_path = "src/main/java/com/hulft/mcp/MCPServer.java"

with open(file_path, 'r') as f:
    lines = f.readlines()

output = []
for i, line in enumerate(lines):
    # Skip if already has final
    if 'final ' in line:
        output.append(line)
        continue
    
    # Match variable declarations that could be final
    # Pattern: whitespace + Type + varName + = + value
    match = re.match(r'^(\s+)(List<[^>]+>|Map<[^>]+>|StringBuilder|String|byte\[\]|int|boolean|long|double|Path) (\w+) = ', line)
    if match:
        indent = match.group(1)
        type_name = match.group(2)
        var_name = match.group(3)
        rest = line[match.end():]
        
        # Check if this variable is reassigned later (simple heuristic)
        # Look ahead a few lines
        is_reassigned = False
        for j in range(i+1, min(i+20, len(lines))):
            if re.search(rf'\b{var_name}\s*=', lines[j]) and 'final' not in lines[j]:
                is_reassigned = True
                break
        
        if not is_reassigned:
            output.append(f'{indent}final {type_name} {var_name} = {rest}')
        else:
            output.append(line)
    else:
        output.append(line)

with open(file_path, 'w') as f:
    f.writelines(output)

print("Added final keywords where appropriate")
