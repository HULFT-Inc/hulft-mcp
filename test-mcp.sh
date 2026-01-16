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
