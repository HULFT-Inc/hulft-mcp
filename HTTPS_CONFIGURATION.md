# HTTPS/SSL Configuration

## Overview

The HULFT MCP service is secured with HTTPS using AWS Certificate Manager (ACM) and Application Load Balancer (ALB).

## Configuration

### Certificate
- **Type:** AWS ACM Wildcard Certificate
- **Domain:** *.hulftincpredev.net
- **ARN:** arn:aws:acm:us-east-1:486560520867:certificate/c95122d8-b92c-470f-8aa3-fdd136041d9e
- **Status:** ISSUED
- **Validation:** DNS validation

### ALB Listeners

#### HTTPS Listener (Port 443)
- **Protocol:** HTTPS
- **SSL Policy:** ELBSecurityPolicy-TLS13-1-2-2021-06
- **Certificate:** *.hulftincpredev.net
- **Action:** Forward to target group (port 3333)

#### HTTP Listener (Port 80)
- **Protocol:** HTTP
- **Action:** Redirect to HTTPS (301 permanent redirect)
- **Target:** https://${host}:443/${path}

### Security Group Rules

ALB Security Group allows:
- **Inbound:** Port 80 (HTTP) from 0.0.0.0/0
- **Inbound:** Port 443 (HTTPS) from 0.0.0.0/0
- **Outbound:** All traffic

## Endpoints

### Primary Endpoint (HTTPS)
```
https://mcp.hulftincpredev.net/mcp
```

### Legacy Endpoint (HTTP - redirects to HTTPS)
```
http://mcp.hulftincpredev.net/mcp
```

## Testing

### Test HTTPS Endpoint
```bash
curl -X POST https://mcp.hulftincpredev.net/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

### Test HTTP Redirect
```bash
curl -I http://mcp.hulftincpredev.net/mcp
# Should return 301 redirect to https://
```

### Verify Certificate
```bash
openssl s_client -connect mcp.hulftincpredev.net:443 -servername mcp.hulftincpredev.net < /dev/null 2>/dev/null | openssl x509 -noout -text
```

### Test Script
```bash
./test-endpoint.sh
# Or specify custom endpoint
./test-endpoint.sh https://mcp.hulftincpredev.net/mcp
```

## SSL/TLS Details

### Supported TLS Versions
- TLS 1.3 (preferred)
- TLS 1.2

### Cipher Suites
Using AWS ELBSecurityPolicy-TLS13-1-2-2021-06:
- TLS 1.3: TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384, TLS_CHACHA20_POLY1305_SHA256
- TLS 1.2: ECDHE-RSA-AES128-GCM-SHA256, ECDHE-RSA-AES256-GCM-SHA384, and more

### Certificate Chain
```
*.hulftincpredev.net (leaf)
  └─ Amazon RSA 2048 M02 (intermediate)
      └─ Amazon Root CA 1 (root)
```

## Terraform Configuration

### Variables
```hcl
variable "certificate_arn" {
  description = "ARN of ACM certificate for HTTPS"
  type        = string
  default     = "arn:aws:acm:us-east-1:486560520867:certificate/c95122d8-b92c-470f-8aa3-fdd136041d9e"
}
```

### Resources
- `aws_lb_listener.https` - HTTPS listener on port 443
- `aws_lb_listener.http` - HTTP listener on port 80 (redirects to HTTPS)
- `aws_security_group.alb` - ALB security group with ports 80 and 443 open

## Security Best Practices

✅ **Implemented:**
- TLS 1.3 and 1.2 only (no older versions)
- Strong cipher suites
- HTTP to HTTPS redirect (301 permanent)
- Wildcard certificate for subdomain flexibility
- Security group restricts traffic to ports 80/443

🔄 **Future Enhancements:**
- HSTS (HTTP Strict Transport Security) headers
- Certificate rotation automation
- Certificate expiration monitoring
- WAF (Web Application Firewall) integration

## Troubleshooting

### Certificate Issues
```bash
# Check certificate status
aws acm describe-certificate \
  --certificate-arn arn:aws:acm:us-east-1:486560520867:certificate/c95122d8-b92c-470f-8aa3-fdd136041d9e \
  --region us-east-1 \
  --profile predev
```

### Listener Issues
```bash
# List ALB listeners
aws elbv2 describe-listeners \
  --load-balancer-arn $(aws elbv2 describe-load-balancers \
    --region us-east-1 --profile predev \
    --query 'LoadBalancers[?contains(LoadBalancerName, `hulft`)].LoadBalancerArn' \
    --output text) \
  --region us-east-1 \
  --profile predev
```

### SSL Handshake Issues
```bash
# Test SSL connection
openssl s_client -connect mcp.hulftincpredev.net:443 -tls1_3
openssl s_client -connect mcp.hulftincpredev.net:443 -tls1_2
```

## Cost Impact

- **ACM Certificate:** $0/month (free for AWS services)
- **ALB HTTPS Processing:** Included in ALB pricing
- **Data Transfer:** Standard AWS data transfer rates apply

No additional cost for HTTPS beyond existing ALB costs.
