#!/bin/bash

URL="https://637eda0beff1.ngrok-free.app"

echo "=== Testing MCP Server ==="
echo

echo "1. Check OAuth metadata:"
curl -s "$URL/.well-known/oauth-authorization-server" | jq .
echo

echo "2. Initialize connection:"
curl -s -X POST "$URL/mcp" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "ngrok-skip-browser-warning: true" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | jq .
echo

echo "3. Send initialized notification:"
curl -s -X POST "$URL/mcp" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "ngrok-skip-browser-warning: true" \
  -d '{"jsonrpc":"2.0","method":"notifications/initialized"}' -w "\nStatus: %{http_code}\n"
echo

echo "4. List tools:"
curl -s -X POST "$URL/mcp" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "ngrok-skip-browser-warning: true" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' | jq .
echo

echo "5. Call echo tool:"
curl -s -X POST "$URL/mcp" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "ngrok-skip-browser-warning: true" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"echo","arguments":{"text":"Hello MCP!"}}}' | jq .
echo

echo "6. List resources:"
curl -s -X POST "$URL/mcp" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "ngrok-skip-browser-warning: true" \
  -d '{"jsonrpc":"2.0","id":4,"method":"resources/list"}' | jq .
echo

echo "7. Read resource:"
curl -s -X POST "$URL/mcp" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "ngrok-skip-browser-warning: true" \
  -d '{"jsonrpc":"2.0","id":5,"method":"resources/read","params":{"uri":"file:///example.txt"}}' | jq .
echo

echo "8. List prompts:"
curl -s -X POST "$URL/mcp" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "ngrok-skip-browser-warning: true" \
  -d '{"jsonrpc":"2.0","id":6,"method":"prompts/list"}' | jq .
echo

echo "9. Get prompt:"
curl -s -X POST "$URL/mcp" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "ngrok-skip-browser-warning: true" \
  -d '{"jsonrpc":"2.0","id":7,"method":"prompts/get","params":{"name":"code-review","arguments":{"code":"function test() { return 1; }"}}}' | jq .
