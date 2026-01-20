#!/bin/bash

ENDPOINT="${1:-https://mcp.hulftincpredev.net/mcp}"

echo "Testing MCP endpoint: $ENDPOINT"
echo "================================"
echo ""

# Test 1: Basic connectivity
echo "1. Testing basic connectivity..."
if curl -s --max-time 5 "$ENDPOINT" > /dev/null 2>&1; then
    echo "   ✓ Connection successful"
else
    echo "   ✗ Connection failed (timeout or refused)"
fi
echo ""

# Test 2: tools/list
echo "2. Testing tools/list method..."
RESPONSE=$(curl -s --max-time 10 "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' 2>&1)

if [ $? -eq 0 ] && [ -n "$RESPONSE" ]; then
    echo "   ✓ Response received"
    echo "   Tools available:"
    echo "$RESPONSE" | jq -r '.result.tools[]?.name' 2>/dev/null | head -5 | sed 's/^/     - /'
else
    echo "   ✗ No response or timeout"
fi
echo ""

# Test 3: prompts/list
echo "3. Testing prompts/list method..."
RESPONSE=$(curl -s --max-time 10 "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{"jsonrpc":"2.0","id":2,"method":"prompts/list"}' 2>&1)

if [ $? -eq 0 ] && [ -n "$RESPONSE" ]; then
    echo "   ✓ Response received"
    PROMPT_COUNT=$(echo "$RESPONSE" | jq -r '.result.prompts | length' 2>/dev/null)
    echo "   Prompts available: $PROMPT_COUNT"
else
    echo "   ✗ No response or timeout"
fi
echo ""

echo "================================"
echo "Test complete"
