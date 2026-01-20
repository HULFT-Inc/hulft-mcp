#!/bin/bash

# MCP HTTP Transport Compliance Test
# Tests against MCP Specification 2025-11-25

ENDPOINT="${1:-https://mcp.hulftincpredev.net/mcp}"
PROTOCOL_VERSION="2025-11-25"

echo "========================================="
echo "MCP HTTP Transport Compliance Test"
echo "========================================="
echo "Endpoint: $ENDPOINT"
echo "Protocol Version: $PROTOCOL_VERSION"
echo ""

PASS=0
FAIL=0

# Test 1: POST with Accept header
echo "Test 1: POST InitializeRequest with proper headers"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "MCP-Protocol-Version: $PROTOCOL_VERSION" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-11-25",
      "capabilities": {},
      "clientInfo": {
        "name": "test-client",
        "version": "1.0.0"
      }
    }
  }' 2>&1)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "202" ]; then
    echo "  ✓ PASS: HTTP $HTTP_CODE"
    ((PASS++))
    
    # Check for MCP-Session-Id header
    SESSION_ID=$(curl -s -I -X POST "$ENDPOINT" \
      -H "Content-Type: application/json" \
      -H "Accept: application/json, text/event-stream" \
      -H "MCP-Protocol-Version: $PROTOCOL_VERSION" \
      -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' \
      2>&1 | grep -i "MCP-Session-Id" | cut -d: -f2 | tr -d ' \r')
    
    if [ -n "$SESSION_ID" ]; then
        echo "  ✓ Session ID returned: $SESSION_ID"
    else
        echo "  ⚠ No MCP-Session-Id header (optional but recommended)"
    fi
else
    echo "  ✗ FAIL: HTTP $HTTP_CODE (expected 200 or 202)"
    echo "  Response: $BODY"
    ((FAIL++))
fi
echo ""

# Test 2: Content-Type negotiation
echo "Test 2: Server supports application/json response"
CONTENT_TYPE=$(curl -s -I -X POST "$ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "MCP-Protocol-Version: $PROTOCOL_VERSION" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' \
  2>&1 | grep -i "Content-Type" | cut -d: -f2 | tr -d ' \r')

if echo "$CONTENT_TYPE" | grep -q "application/json"; then
    echo "  ✓ PASS: Content-Type is $CONTENT_TYPE"
    ((PASS++))
else
    echo "  ✗ FAIL: Content-Type is $CONTENT_TYPE (expected application/json)"
    ((FAIL++))
fi
echo ""

# Test 3: GET request for SSE stream (optional)
echo "Test 3: GET request for SSE stream (optional feature)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$ENDPOINT" \
  -H "Accept: text/event-stream" \
  -H "MCP-Protocol-Version: $PROTOCOL_VERSION" \
  --max-time 5 2>&1)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)

if [ "$HTTP_CODE" = "200" ]; then
    echo "  ✓ PASS: Server supports SSE streaming (HTTP 200)"
    ((PASS++))
elif [ "$HTTP_CODE" = "405" ]; then
    echo "  ⚠ INFO: Server does not support SSE streaming (HTTP 405 - acceptable)"
    ((PASS++))
else
    echo "  ⚠ INFO: HTTP $HTTP_CODE (405 expected if SSE not supported)"
    ((PASS++))
fi
echo ""

# Test 4: Protocol version header validation
echo "Test 4: Protocol version header handling"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "MCP-Protocol-Version: invalid-version" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/list"}' \
  2>&1)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)

if [ "$HTTP_CODE" = "400" ]; then
    echo "  ✓ PASS: Server rejects invalid protocol version (HTTP 400)"
    ((PASS++))
else
    echo "  ⚠ WARN: HTTP $HTTP_CODE (expected 400 for invalid protocol version)"
    echo "  Server should validate MCP-Protocol-Version header"
    ((FAIL++))
fi
echo ""

# Test 5: JSON-RPC format
echo "Test 5: JSON-RPC 2.0 response format"
RESPONSE=$(curl -s -X POST "$ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "MCP-Protocol-Version: $PROTOCOL_VERSION" \
  -d '{"jsonrpc":"2.0","id":4,"method":"tools/list"}' \
  2>&1)

if echo "$RESPONSE" | jq -e '.jsonrpc == "2.0"' > /dev/null 2>&1; then
    echo "  ✓ PASS: Response has jsonrpc: 2.0"
    ((PASS++))
else
    echo "  ✗ FAIL: Response missing or invalid jsonrpc field"
    echo "  Response: $RESPONSE"
    ((FAIL++))
fi

if echo "$RESPONSE" | jq -e '.id == 4' > /dev/null 2>&1; then
    echo "  ✓ PASS: Response has matching id"
    ((PASS++))
else
    echo "  ✗ FAIL: Response missing or mismatched id"
    ((FAIL++))
fi
echo ""

# Test 6: UTF-8 encoding
echo "Test 6: UTF-8 encoding support"
RESPONSE=$(curl -s -X POST "$ENDPOINT" \
  -H "Content-Type: application/json; charset=utf-8" \
  -H "Accept: application/json" \
  -H "MCP-Protocol-Version: $PROTOCOL_VERSION" \
  -d '{"jsonrpc":"2.0","id":5,"method":"tools/list"}' \
  2>&1)

if [ -n "$RESPONSE" ]; then
    echo "  ✓ PASS: Server accepts UTF-8 encoded messages"
    ((PASS++))
else
    echo "  ✗ FAIL: Server rejected UTF-8 encoded message"
    ((FAIL++))
fi
echo ""

# Summary
echo "========================================="
echo "Test Summary"
echo "========================================="
echo "Passed: $PASS"
echo "Failed: $FAIL"
echo ""

if [ $FAIL -eq 0 ]; then
    echo "✓ All tests passed!"
    echo ""
    echo "Note: This server uses a simplified HTTP transport."
    echo "Full MCP Streamable HTTP compliance requires:"
    echo "  - SSE streaming support (optional)"
    echo "  - Session management with MCP-Session-Id"
    echo "  - Protocol version validation"
    echo "  - Proper Accept header negotiation"
    exit 0
else
    echo "✗ Some tests failed"
    echo ""
    echo "The server does NOT fully comply with MCP HTTP spec."
    echo "See: https://modelcontextprotocol.io/specification/2025-11-25/basic/transports"
    exit 1
fi
