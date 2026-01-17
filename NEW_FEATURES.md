# New Features Added

## 1. ✅ TAR Archive Support

**Implementation:**
- Added Apache Commons Compress library
- Supports `.tar` and `.tar.gz` files
- Extracts all files into single job folder
- Works alongside existing ZIP support

**Usage:**
```json
{
  "files": [{
    "filename": "documents.tar.gz",
    "type": "archive",
    "content": "base64..."
  }]
}
```

**Code:**
```java
private static int extractTar(Path tarFile, Path destDir) {
    // Handles both .tar and .tar.gz
    // Extracts all files preserving directory structure
}
```

---

## 2. ✅ OCR Confidence Scores

**Implementation:**
- Tracks Textract confidence per LINE block
- Calculates average confidence across document
- Stored in metadata as `ocrConfidence`
- Range: 0-100 (percentage)

**Example Output:**
```json
{
  "filename": "invoice.pdf",
  "ocrConfidence": 99.527405,
  "extractedFields": {...}
}
```

**Benefits:**
- Quality assurance for OCR results
- Flag low-confidence documents for review
- Track extraction reliability over time

---

## 3. ✅ Custom Extraction Schemas

**Implementation:**
- Runtime schema management
- Per-document-type JSON schemas
- Overrides built-in schemas
- Stored in-memory (ConcurrentHashMap)

**New Tools:**

### `add_schema`
Add custom extraction schema:
```json
{
  "name": "add_schema",
  "arguments": {
    "doc_type": "CUSTOM_RECEIPT",
    "schema": "{\"store\": \"string\", \"date\": \"string\", \"total\": \"string\"}"
  }
}
```

### `list_schemas`
List all custom schemas:
```json
{
  "name": "list_schemas",
  "arguments": {}
}
```
Returns:
```
Custom schemas:
- CUSTOM_RECEIPT
- CUSTOM_INVOICE
```

### `get_schema`
Get schema for document type:
```json
{
  "name": "get_schema",
  "arguments": {
    "doc_type": "CUSTOM_RECEIPT"
  }
}
```

**Use Cases:**
- Industry-specific documents
- Custom invoice formats
- Proprietary document types
- A/B testing different schemas
- Client-specific requirements

**Example Workflow:**
```bash
# 1. Add custom schema
add_schema(doc_type="MEDICAL_CLAIM", schema="{...}")

# 2. Upload document
upload_files(files=[...])

# 3. Bedrock uses custom schema for extraction
# Result: fields matching your custom schema
```

---

## Complete Feature Set

### Document Processing
- ✅ PDF, Excel, Image, ZIP, TAR support
- ✅ Text extraction (Textract + POI)
- ✅ Table extraction (Tabula)
- ✅ OCR with confidence scores
- ✅ Document classification (3-method consensus)
- ✅ Structured field extraction (Bedrock)
- ✅ Markdown conversion

### Async & Jobs
- ✅ Background processing
- ✅ Job status tracking
- ✅ Thread pool executor

### Customization
- ✅ Custom extraction schemas
- ✅ Runtime schema management
- ✅ Built-in + custom schema support

### Output Formats
- ✅ Raw text
- ✅ Structured JSON
- ✅ Markdown
- ✅ Extracted fields
- ✅ Classification with confidence
- ✅ OCR confidence scores

---

## Testing Results

### OCR Confidence
```json
{
  "filename": "invoice_production.pdf",
  "ocrConfidence": 99.527405  // 99.5% confidence!
}
```

### Custom Schema
```bash
✓ Schema added for: CUSTOM_RECEIPT
✓ Schema retrieved successfully
✓ Used in Bedrock extraction
```

### TAR Extraction
```bash
✓ Supports .tar and .tar.gz
✓ Extracts all files
✓ Preserves directory structure
```

---

## Next Steps (Optional)

1. **Persist schemas** - Save to database/file
2. **Schema validation** - Validate JSON schemas
3. **Confidence thresholds** - Auto-flag low confidence docs
4. **TAR.BZ2 support** - Add bzip2 compression
5. **Schema versioning** - Track schema changes
6. **Batch schema upload** - Import multiple schemas
7. **Schema templates** - Pre-built industry schemas
