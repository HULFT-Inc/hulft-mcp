# AWS Deployment

## Infrastructure

This deployment uses:
- **ECS Fargate** - Serverless container orchestration
- **Application Load Balancer** - Traffic distribution
- **Route53** - DNS management
- **ECR** - Container registry
- **VPC** - 10.50.0.0/16 subnet in prod-vpc

## Prerequisites

1. AWS CLI configured with `predev` profile
2. Docker installed
3. Terraform installed
4. Route53 hosted zone: `hulft.predev.aws`

## Deployment Steps

### 1. Initialize Terraform

```bash
cd terraform
terraform init
```

### 2. Review Plan

```bash
terraform plan
```

### 3. Deploy Infrastructure

```bash
terraform apply
```

### 4. Build and Deploy Application

```bash
cd ..
./deploy.sh
```

## Configuration

Edit `terraform/variables.tf` to customize:

- `vpc_id` - VPC ID (default: vpc-0cac8cd860ede88c5)
- `subnet_cidr` - Subnet CIDR (default: 10.50.0.0/24)
- `domain_name` - DNS name (default: mcp.hulft.predev.aws)
- `desired_count` - Number of tasks (default: 2)
- `cpu` - Fargate CPU (default: 512)
- `memory` - Fargate memory (default: 1024)

## Access

After deployment:

**Service URL:** http://mcp.hulft.predev.aws/mcp

**Health Check:** http://mcp.hulft.predev.aws/mcp (returns 405)

**ECS Console:** https://console.aws.amazon.com/ecs/home?region=us-east-1#/clusters/hulft-mcp-cluster

## Monitoring

- **CloudWatch Logs:** `/ecs/hulft-mcp`
- **Container Insights:** Enabled on cluster
- **ALB Metrics:** Available in CloudWatch

## Scaling

Update desired count:

```bash
cd terraform
terraform apply -var="desired_count=4"
```

## Cleanup

```bash
cd terraform
terraform destroy
```

## Architecture

```
Internet
    ↓
Application Load Balancer (ALB)
    ↓
ECS Fargate Tasks (2x)
    ↓
AWS Services (Textract, Comprehend, Bedrock)
```

## Security

- ALB: Public (ports 80, 443)
- ECS Tasks: Private (only accessible from ALB)
- IAM Roles: Least privilege for AWS services
- Security Groups: Restricted ingress/egress

## Cost Estimate

- **ECS Fargate:** ~$30/month (2 tasks, 512 CPU, 1GB RAM)
- **ALB:** ~$20/month
- **Data Transfer:** Variable
- **AWS Services:** Pay per use

**Total:** ~$50-100/month depending on usage
