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
  default     = "mcp.hulft.predev.aws"
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
