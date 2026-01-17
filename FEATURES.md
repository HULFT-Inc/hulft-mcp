# HULFT MCP Server - Feature Summary

## 🚀 Complete Feature Set

### 1. **Async Processing** ✅
- Background job processing with thread pool
- Immediate job ID return
- `check_job` tool to query status
- Usage: `upload_files` with `async: true`

### 2. **Bedrock Structured Extraction** ✅
- Claude-powered field extraction
- JSON schemas for all document types
- Extracts: invoice numbers, dates, customers, items, amounts, etc.
- Example output:
```json
{
  "invoice_number": "INV-2026-001",
  "date": "2026-01-16",
  "customer": "Acme Corp",
  "total_amount": "$5,500.00",
  "items": [...]
}
```

### 3. **Tabula PDF Table Extraction** ✅
- Automatic table detection in PDFs
- Structured row/column data
- Stored in `structuredData.tables`

### 4. **Multi-Format Support**
- **PDFs**: Textract + Tabula tables + Bedrock extraction
- **Excel**: Apache POI + structured sheets/rows
- **Images**: Textract OCR
- **Archives**: ZIP extraction (TAR TODO)

### 5. **Document Classification**
- 3-method consensus: Regex + Comprehend + Bedrock
- Types: INVOICE_PRODUCTION, PURCHASE_ORDER, SCHEDULE_PRODUCTION, CUSTOMS_DECLARATION
- Confidence scoring

### 6. **Output Formats**
Every document gets:
1. **Raw text** (`textractAnalysis`)
2. **Structured JSON** (`structuredData` + `extractedFields`)
3. **Markdown** (`markdown`)
4. **Classification** (`finalClassification`)

## 📊 Complete Metadata Example

```json
{
  "jobId": "uuid",
  "filename": "invoice.xlsx",
  "detectedType": "application/x-tika-ooxml",
  "size": 3536,
  "textractAnalysis": "Purpose: INVOICE_PRODUCTION...",
  "structuredData": {
    "sheets": [{
      "name": "Invoice",
      "rows": [["Purpose:", "INVOICE_PRODUCTION"], ...]
    }],
    "tables": []
  },
  "extractedFields": {
    "invoice_number": "INV-2026-001",
    "date": "2026-01-16",
    "customer": "Acme Corp",
    "total_amount": "$5,500.00",
    "items": [{"name": "Widget A", "amount": "$5,000.00"}]
  },
  "markdown": "# Invoice\n\n| Purpose: | INVOICE_PRODUCTION |...",
  "classification": {
    "regex": {"type": "INVOICE_PRODUCTION", "confidence": 1.0},
    "comprehend": {...},
    "bedrock": {...}
  },
  "finalClassification": {
    "type": "INVOICE_PRODUCTION",
    "confidence": 1.0,
    "method": "consensus"
  }
}
```

## 🛠️ MCP Tools

1. **upload_files** - Upload and process documents (sync or async)
2. **check_job** - Check async job status
3. **list_resources** - List available resources
4. **read_resource** - Read resource content
5. **get_prompt** - Get code review prompt
6. **echo** - Echo test

## 🔧 Tech Stack

- **Javalin** - HTTP server
- **AWS Textract** - OCR and text extraction
- **AWS Comprehend** - Entity detection
- **AWS Bedrock (Claude)** - Classification and field extraction
- **Apache Tika** - File type detection
- **Apache PDFBox** - PDF processing
- **Apache POI** - Excel processing
- **Tabula** - PDF table extraction
- **Gson** - JSON serialization

## 📝 Usage

```bash
# Start server
java -jar build/libs/hulft-mcp-1.0.0.jar

# Server runs on http://localhost:3333/mcp
```

## 🎯 Next Steps

- Add TAR archive support
- Implement caching layer
- Add validation for extracted fields
- Support more document types
- Add batch processing endpoint
