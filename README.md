# HULFT MCP Server

Java MCP server for Claude using SSE transport.

## Build & Run

```bash
./gradlew build
java -jar build/libs/hulft-mcp-1.0.0.jar
```

Server runs on http://localhost:3333

## Claude Configuration

Add via Claude Settings > Connectors:
- URL: `https://localhost:3333/mcp`
- No auth required

## Available Tools

- **echo**: Echoes back input text

## Endpoints

- `GET /mcp/v1/tools` - List available tools
- `POST /mcp/v1/call-tool` - Execute a tool
