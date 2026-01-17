# QA Testing Checklist

## 📋 Functional Tests

### Document Upload
- [ ] PDF upload and processing
- [ ] Excel upload and processing
- [ ] Image upload and OCR
- [ ] ZIP archive extraction
- [ ] TAR archive extraction
- [ ] TAR.GZ archive extraction
- [ ] Multiple files in single request
- [ ] Mixed file types (PDF + Excel + Image)

### Text Extraction
- [ ] Textract extracts text from PDF
- [ ] Textract extracts text from images
- [ ] Apache POI extracts text from Excel
- [ ] Multi-page PDF extraction
- [ ] Low-quality image OCR
- [ ] Handwritten text (if supported)

### Document Classification
- [ ] INVOICE_PRODUCTION classification
- [ ] PURCHASE_ORDER classification
- [ ] SCHEDULE_PRODUCTION classification
- [ ] CUSTOMS_DECLARATION classification
- [ ] UNKNOWN type for unrecognized docs
- [ ] Consensus voting works correctly
- [ ] Confidence scores are accurate

### Field Extraction
- [ ] Invoice fields extracted (number, date, customer, total)
- [ ] PO fields extracted (number, vendor, items)
- [ ] Schedule fields extracted (date, product, quantity)
- [ ] Custom schema fields extracted
- [ ] Nested objects (items array) extracted
- [ ] Missing fields handled gracefully

### Table Extraction
- [ ] Tabula extracts tables from PDF
- [ ] Multi-table PDFs handled
- [ ] Table structure preserved
- [ ] Empty tables handled

### Markdown Conversion
- [ ] PDF converted to markdown
- [ ] Excel converted to markdown tables
- [ ] Headers formatted correctly
- [ ] Special characters escaped

### OCR Confidence
- [ ] Confidence score calculated
- [ ] Score between 0-100
- [ ] High-quality docs > 95%
- [ ] Low-quality docs flagged

### Custom Schemas
- [ ] add_schema creates new schema
- [ ] list_schemas shows all schemas
- [ ] get_schema retrieves schema
- [ ] Custom schema used in extraction
- [ ] Built-in schemas still work

### Async Processing
- [ ] Async upload returns job ID
- [ ] check_job returns status
- [ ] Completed jobs return results
- [ ] Failed jobs return error
- [ ] Processing status updates

---

## 🔍 Edge Cases

### Invalid Input
- [ ] Empty file rejected
- [ ] Invalid base64 rejected
- [ ] Corrupted PDF handled
- [ ] Corrupted Excel handled
- [ ] Corrupted image handled
- [ ] Missing required fields rejected
- [ ] Invalid file type rejected

### Large Files
- [ ] 10MB file processes
- [ ] 50MB file processes
- [ ] 100MB file processes (or fails gracefully)
- [ ] Memory doesn't leak
- [ ] Timeout handling

### Special Characters
- [ ] Unicode in filenames
- [ ] Special chars in content
- [ ] Emoji in documents
- [ ] Non-English text

### Boundary Conditions
- [ ] Single character document
- [ ] Empty document
- [ ] Document with only images
- [ ] Document with only tables
- [ ] 1000-page PDF

---

## ⚡ Performance Tests

### Speed
- [ ] Single PDF < 5s
- [ ] Single Excel < 2s
- [ ] Single Image < 3s
- [ ] 10 files < 30s
- [ ] Archive extraction < 10s

### Concurrency
- [ ] 5 concurrent uploads
- [ ] 10 concurrent uploads
- [ ] 50 concurrent uploads
- [ ] No race conditions
- [ ] Thread pool doesn't exhaust

### Memory
- [ ] Memory usage stable
- [ ] No memory leaks
- [ ] Garbage collection works
- [ ] Large files don't OOM

### Scalability
- [ ] 100 documents processed
- [ ] 1000 documents processed
- [ ] Job queue doesn't overflow
- [ ] Disk space managed

---

## 🔒 Security Tests

### Input Validation
- [ ] SQL injection prevented
- [ ] XSS prevented
- [ ] Path traversal prevented
- [ ] File type validation
- [ ] Size limits enforced

### Authentication
- [ ] Unauthorized requests rejected (when implemented)
- [ ] Invalid tokens rejected
- [ ] Rate limiting works

### Data Privacy
- [ ] Sensitive data not logged
- [ ] Files stored securely
- [ ] Metadata protected
- [ ] Temp files cleaned up

---

## 🔄 Integration Tests

### AWS Services
- [ ] Textract connection works
- [ ] Comprehend connection works
- [ ] Bedrock connection works
- [ ] Credentials valid
- [ ] Region configured correctly
- [ ] Error handling for AWS failures

### File System
- [ ] Jobs directory created
- [ ] Files saved correctly
- [ ] Metadata saved correctly
- [ ] Permissions correct
- [ ] Cleanup works

### MCP Protocol
- [ ] JSON-RPC requests handled
- [ ] Responses formatted correctly
- [ ] Error responses valid
- [ ] Notifications handled
- [ ] SSE support works

---

## 🧪 Regression Tests

### After Each Change
- [ ] All existing tests pass
- [ ] No new errors in logs
- [ ] Performance not degraded
- [ ] Memory usage stable
- [ ] API compatibility maintained

---

## 📊 Test Results Template

```
Test Run: [Date/Time]
Environment: [Local/Dev/Staging]
Version: [Git commit hash]

Functional Tests: [X/Y passed]
Edge Cases: [X/Y passed]
Performance Tests: [X/Y passed]
Security Tests: [X/Y passed]
Integration Tests: [X/Y passed]

Total: [X/Y passed] ([%] pass rate)

Failed Tests:
- [Test name]: [Reason]
- [Test name]: [Reason]

Performance Metrics:
- Avg processing time: [X]s
- Max memory usage: [X]MB
- Concurrent capacity: [X] requests

Issues Found:
1. [Issue description]
2. [Issue description]

Recommendations:
1. [Recommendation]
2. [Recommendation]
```

---

## 🚀 Running Tests

### Unit Tests
```bash
./gradlew test
```

### QA Test Suite
```bash
./gradlew test --tests QATestSuite
```

### Specific Test
```bash
./gradlew test --tests QATestSuite.testPDFUploadAndExtraction
```

### With Coverage
```bash
./gradlew test jacocoTestReport
```

### Integration Tests
```bash
# Start server first
java -jar build/libs/hulft-mcp-1.0.0.jar

# Run tests
./gradlew test --tests QATestSuite
```

---

## 📈 Success Criteria

### Must Pass
- All functional tests
- All edge case tests
- No critical security issues
- Performance within SLA

### Should Pass
- 90%+ test coverage
- No memory leaks
- Concurrent handling works
- Error recovery works

### Nice to Have
- 95%+ test coverage
- Sub-second response times
- 100+ concurrent requests
- Zero downtime deployment

---

## 🐛 Bug Report Template

```
Title: [Brief description]

Environment:
- OS: [macOS/Linux/Windows]
- Java Version: [21.0.9]
- Server Version: [commit hash]

Steps to Reproduce:
1. [Step 1]
2. [Step 2]
3. [Step 3]

Expected Behavior:
[What should happen]

Actual Behavior:
[What actually happened]

Logs:
```
[Relevant log entries]
```

Screenshots:
[If applicable]

Severity: [Critical/High/Medium/Low]
Priority: [P0/P1/P2/P3]
```
