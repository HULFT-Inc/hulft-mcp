# KiroTODO.md - hulft-mcp

**Project**: HULFT Document Processing MCP Server
**Priority**: 🟡 HIGH (Best Implementation - Use as Reference)
**Owner**: TBD
**Started**: 2026-01-22

## 🔴 Week 1: Security & Secrets (Jan 22-29)

### API Key Authentication
- [ ] Create API keys in Secrets Manager
  ```bash
  aws secretsmanager create-secret \
    --name mcp/hulft-mcp/api-keys \
    --secret-string '{"keys":["key1","key2"]}'
  ```
- [ ] Implement `ApiKeyFilter` (copy from mcp-opnova after Week 1)
- [ ] Load keys from Secrets Manager on startup
- [ ] Test unauthorized access returns 401
- [ ] Update client configurations

### Fix Hardcoded AWS Profile
- [ ] Change from `ProfileCredentialsProvider.create("predev")`
- [ ] To: `ProfileCredentialsProvider.create(System.getenv("AWS_PROFILE"))`
- [ ] Set `AWS_PROFILE=predev` in ECS task definition
- [ ] Test with different profiles
- [ ] Deploy to dev

### Rate Limiting
- [ ] Add Guava dependency for `LoadingCache`
- [ ] Implement `RateLimitFilter`
  ```java
  @Filter("/mcp/**")
  public class RateLimitFilter {
      // 60 requests per minute per API key
  }
  ```
- [ ] Test rate limit enforcement
- [ ] Add rate limit headers to response

**Deliverable**: Secure hulft-mcp with authentication

---

## 🟡 Week 2: SSE Implementation (Jan 29 - Feb 5)

### SSE Endpoint
- [ ] Remove 405 response from `handleGet()`
- [ ] Implement SSE streaming
  ```java
  private static void handleSSE(Context ctx) {
      String sessionId = UUID.randomUUID().toString();
      sessions.put(sessionId, new Session());

      ctx.contentType("text/event-stream");
      // Send endpoint event
      // Keep connection alive
  }
  ```
- [ ] Add session cleanup (30 min timeout)
- [ ] Test with curl
- [ ] Test with Claude Desktop

### Session Management Enhancement
- [ ] Add session expiration (currently missing)
- [ ] Implement session cleanup task
- [ ] Add session metrics to health endpoint
- [ ] Test session lifecycle

**Deliverable**: Full SSE support for desktop clients

---

## 🟢 Week 3: Extract to mcp-common (Feb 5-12)

### Identify Reusable Code
- [ ] Protocol classes (already excellent)
- [ ] Error handling
- [ ] Session management
- [ ] Origin validation
- [ ] Health check format

### Create mcp-common Library
- [ ] Create new Gradle project: `mcp-common`
- [ ] Extract protocol classes
  ```
  mcp-common/
  ├── protocol/
  │   ├── MCPRequest.java
  │   ├── MCPResponse.java
  │   └── MCPErrorCode.java
  ├── transport/
  │   ├── HttpTransport.java
  │   └── SSETransport.java
  └── session/
      └── SessionManager.java
  ```
- [ ] Add comprehensive tests
- [ ] Publish to internal Maven/Gradle repo
- [ ] Version: 1.0.0

### Documentation
- [ ] Write mcp-common README
- [ ] Add JavaDoc to all public APIs
- [ ] Create usage examples
- [ ] Document migration guide

**Deliverable**: mcp-common library v1.0.0 published

---

## 🔵 Week 4: Optimization & Polish (Feb 12-19)

### Migrate to mcp-common
- [ ] Add mcp-common dependency to hulft-mcp
- [ ] Replace local classes with mcp-common
- [ ] Test all endpoints
- [ ] Verify no regressions

### Performance Optimization
- [ ] Add caching for tool list
- [ ] Add caching for schemas
- [ ] Optimize Textract calls
- [ ] Add connection pooling metrics

### Monitoring
- [ ] Add CloudWatch custom metrics
- [ ] Add X-Ray tracing
- [ ] Create CloudWatch dashboard
- [ ] Set up alarms for errors

**Deliverable**: Optimized, production-ready server

---

## 📊 Progress Tracking

**Week 1**: 0/3 tasks ⬜⬜⬜
**Week 2**: 0/2 tasks ⬜⬜
**Week 3**: 0/3 tasks ⬜⬜⬜
**Week 4**: 0/4 tasks ⬜⬜⬜⬜

**Overall**: 0/12 major tasks (0%)

---

## 🎯 Success Metrics

- [ ] 0 PMD violations (currently: ✅ 0)
- [ ] 0 Checkstyle violations (currently: ✅ 0)
- [ ] >70% test coverage (currently: ✅ 73.9%)
- [ ] All tests passing (currently: ✅ 54/54)
- [ ] SSE working with Claude Desktop
- [ ] API key authentication enforced
- [ ] mcp-common library published

---

## 🌟 Notes

- **This is the reference implementation** - highest quality code
- Use this as template for other projects
- Already MCP 2025-11-25 compliant ✅
- Already has origin validation ✅
- Already has session management ✅
- Only missing: SSE and authentication

---

## 🔗 References

- Code Analysis: `findings/07-CODE-ANALYSIS.md` (Grade: A-)
- Security: `findings/05-SECURITY-COMPLIANCE.md`
- Standards: `findings/02-MCP-CODING-STANDARDS.md`
