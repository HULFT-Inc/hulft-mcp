# HULFT MCP Server

[![CI/CD](https://github.com/HULFT-Inc/hulft-mcp/actions/workflows/ci.yml/badge.svg)](https://github.com/HULFT-Inc/hulft-mcp/actions/workflows/ci.yml)
[![Code Quality](https://img.shields.io/badge/PMD-0%20violations-brightgreen)](https://github.com/HULFT-Inc/hulft-mcp)
[![Checkstyle](https://img.shields.io/badge/Checkstyle-0%20violations-brightgreen)](https://github.com/HULFT-Inc/hulft-mcp)
[![Tests](https://img.shields.io/badge/tests-54%2F54%20passing-brightgreen)](https://github.com/HULFT-Inc/hulft-mcp)
[![Coverage](https://img.shields.io/badge/coverage-73.9%25-brightgreen)](https://github.com/HULFT-Inc/hulft-mcp)

Model Context Protocol (MCP) server for document processing with AWS AI services. Provides tools for text extraction, document classification, field extraction, and more.

## 🚀 Quick Start

### Production
```bash
# Access production endpoint
curl https://mcp.hulftincpredev.net/health
```

### Development
```bash
# Access development endpoint
curl https://mcp-dev.hulftincpredev.net/health
```

### Local Development
```bash
# Build and run
./gradlew build
java -jar build/libs/hulft-mcp-1.0.0.jar

# Server starts on http://localhost:3333
```

## 🏗️ Architecture

### Environments

| Environment | Branch | URL | Resources |
|------------|--------|-----|-----------|
| Production | `main` | https://mcp.hulftincpredev.net/mcp | 2 tasks, 512 CPU, 1GB RAM |
| Development | `develop` | https://mcp-dev.hulftincpredev.net/mcp | 1 task, 256 CPU, 512MB RAM |

### Infrastructure

- **Platform**: AWS ECS Fargate
- **Load Balancer**: Application Load Balancer with HTTPS
- **Container**: Docker (linux/amd64)
- **Logging**: CloudWatch Logs with JSON formatting
- **DNS**: Route53 with ACM SSL certificates

## 📋 Available Tools

### Document Processing
- **extract_text** - Extract text from PDF, Word, Excel, images
- **classify_document** - Classify document type using AWS Comprehend/Bedrock
- **extract_fields** - Extract structured fields using AWS Bedrock
- **convert_to_markdown** - Convert documents to Markdown format

### Archive Operations
- **extract_archive** - Extract ZIP/TAR archives
- **list_archive** - List archive contents

### Job Management
- **get_job_status** - Check async job status
- **list_jobs** - List all jobs
- **cancel_job** - Cancel running job

### Schema Management
- **get_schema** - Get field extraction schema
- **list_schemas** - List available schemas

## 🔌 Endpoints

### MCP Protocol
- `POST /mcp` - MCP JSON-RPC requests
- `GET /mcp` - SSE streaming (returns 405 - not supported)

### Health Check
- `GET /health` - Service health status

```json
{
  "status": "healthy",
  "version": "2.1.0",
  "service": "hulft-mcp",
  "timestamp": 1737398400000
}
```

## 🔐 MCP Protocol Compliance

Implements MCP Streamable HTTP specification (2025-11-25):

- ✅ Protocol version validation (`MCP-Protocol-Version` header)
- ✅ Session management (`MCP-Session-Id` header)
- ✅ Accept header negotiation
- ✅ Origin header validation (DNS rebinding protection)
- ✅ JSON-RPC 2.0 error responses
- ⚠️ SSE streaming (optional, returns 405)

See [MCP_COMPLIANCE.md](MCP_COMPLIANCE.md) for details.

## 🛠️ Development

### Prerequisites
- Java 17+
- Docker
- AWS CLI (for deployment)
- Pre-commit (for git hooks)

### Setup

```bash
# Clone repository
git clone https://github.com/HULFT-Inc/hulft-mcp.git
cd hulft-mcp

# Install pre-commit hooks
pip install pre-commit
pre-commit install

# Build
./gradlew build

# Run tests
./gradlew test

# Check code quality
./gradlew pmdMain checkstyleMain
```

### Workflow

1. Create feature branch: `git checkout -b feature/my-feature`
2. Make changes and commit (pre-commit hooks run automatically)
3. Push branch: `git push origin feature/my-feature`
4. Create Pull Request to `main`
5. Merge via PR (triggers deployment)

**Note**: Direct commits to `main` are blocked by pre-commit hooks.

## 🚢 Deployment

### Automated (GitHub Actions)

Deployments are triggered automatically:
- Push to `main` → Production deployment
- Push to `develop` → Development deployment

Workflows use OIDC for AWS authentication (no static credentials).

### Manual Deployment

```bash
# Build Docker image
docker buildx build --platform linux/amd64 -t hulft-mcp:latest .

# Push to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 486560520867.dkr.ecr.us-east-1.amazonaws.com
docker tag hulft-mcp:latest 486560520867.dkr.ecr.us-east-1.amazonaws.com/hulft-mcp:latest
docker push 486560520867.dkr.ecr.us-east-1.amazonaws.com/hulft-mcp:latest

# Update ECS service
aws ecs update-service --cluster hulft-mcp-cluster --service hulft-mcp --force-new-deployment --region us-east-1
```

### Infrastructure (Terraform)

```bash
cd terraform

# Production
terraform workspace select prod
terraform apply -var-file=prod.tfvars

# Development
terraform workspace select default
terraform apply -var-file=dev.tfvars
```

## 📊 Monitoring

### CloudWatch Logs
```bash
# Production logs
aws logs tail /ecs/hulft-mcp --follow --region us-east-1

# Development logs
aws logs tail /ecs/hulft-mcp-dev --follow --region us-east-1
```

### Health Check
```bash
# Check service health
curl https://mcp.hulftincpredev.net/health

# Check ECS service
aws ecs describe-services --cluster hulft-mcp-cluster --services hulft-mcp --region us-east-1
```

## 🏆 Quality Metrics

- ✅ **PMD:** 0 violations (down from 383)
- ✅ **Checkstyle:** 0 violations (down from 46)
- ✅ **Tests:** 54/54 passing (100%)
- ✅ **Coverage:** 73.9% (exceeds 70% threshold)

## 📚 Documentation

- [MCP Compliance](MCP_COMPLIANCE.md) - Protocol compliance details
- [HTTPS Configuration](HTTPS_CONFIGURATION.md) - SSL/TLS setup
- [Deployment Verification](DEPLOYMENT_VERIFICATION.md) - Infrastructure status
- [Pre-commit Hooks](PRE_COMMIT.md) - Git workflow protection
- [AWS Deployment](AWS_DEPLOYMENT.md) - Infrastructure guide

## 🔒 Security

- HTTPS with ACM wildcard certificate
- TLS 1.3/1.2 only
- Origin header validation
- Session management
- No static AWS credentials (OIDC)
- Pre-commit hooks prevent private key commits

## 💰 Cost Estimate

- ECS Fargate: ~$30/month
- Application Load Balancer: ~$20/month
- Data Transfer: ~$5-10/month
- CloudWatch Logs: ~$2/month
- ECR: ~$1/month

**Total**: ~$58-63/month

## 📝 License

MIT License - see [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Make changes with tests
4. Ensure all quality checks pass
5. Submit Pull Request

## 📞 Support

For issues or questions, please open a GitHub issue.
