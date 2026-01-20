variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "aws_profile" {
  description = "AWS profile"
  type        = string
  default     = "predev"
}

variable "vpc_id" {
  description = "VPC ID for deployment"
  type        = string
  default     = "vpc-0cac8cd860ede88c5"
}

variable "subnet_cidr" {
  description = "Subnet CIDR block"
  type        = string
  default     = "10.50.0.0/24"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "predev"
}

variable "app_name" {
  description = "Application name"
  type        = string
  default     = "hulft-mcp"
}

variable "domain_name" {
  description = "Domain name for the service"
  type        = string
  default     = "mcp.hulftincpredev.net"
}

variable "container_port" {
  description = "Container port"
  type        = number
  default     = 3333
}

variable "desired_count" {
  description = "Desired number of tasks"
  type        = number
  default     = 2
}

variable "cpu" {
  description = "Fargate CPU units"
  type        = string
  default     = "512"
}

variable "memory" {
  description = "Fargate memory in MB"
  type        = string
  default     = "1024"
}

variable "certificate_arn" {
  description = "ARN of ACM certificate for HTTPS"
  type        = string
  default     = "arn:aws:acm:us-east-1:486560520867:certificate/c95122d8-b92c-470f-8aa3-fdd136041d9e"
}

variable "create_dns_record" {
  description = "Create Route53 DNS record"
  type        = bool
  default     = true
}
