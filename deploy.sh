#!/bin/bash
set -e

echo "🚀 Deploying HULFT MCP to AWS ECS..."

# Variables
AWS_REGION="us-east-1"
AWS_PROFILE="predev"
APP_NAME="hulft-mcp"

# Build JAR
echo "📦 Building JAR..."
./gradlew clean build -x test

# Build Docker image
echo "🐳 Building Docker image..."
docker build -t ${APP_NAME}:latest .

# Get ECR repository URL
echo "🔍 Getting ECR repository URL..."
cd terraform
terraform init
ECR_URL=$(terraform output -raw ecr_repository_url 2>/dev/null || echo "")

if [ -z "$ECR_URL" ]; then
  echo "⚠️  ECR repository not found. Running terraform apply first..."
  terraform apply -auto-approve
  ECR_URL=$(terraform output -raw ecr_repository_url)
fi

# Login to ECR
echo "🔐 Logging in to ECR..."
aws ecr get-login-password --region ${AWS_REGION} --profile ${AWS_PROFILE} | \
  docker login --username AWS --password-stdin ${ECR_URL}

# Tag and push image
echo "📤 Pushing image to ECR..."
docker tag ${APP_NAME}:latest ${ECR_URL}:latest
docker push ${ECR_URL}:latest

# Update ECS service
echo "🔄 Updating ECS service..."
aws ecs update-service \
  --cluster ${APP_NAME}-cluster \
  --service ${APP_NAME} \
  --force-new-deployment \
  --region ${AWS_REGION} \
  --profile ${AWS_PROFILE}

# Get service URL
SERVICE_URL=$(terraform output -raw service_url)

echo "✅ Deployment complete!"
echo "🌐 Service URL: ${SERVICE_URL}"
echo "📊 Monitor: https://console.aws.amazon.com/ecs/home?region=${AWS_REGION}#/clusters/${APP_NAME}-cluster/services/${APP_NAME}/tasks"
