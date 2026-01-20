#!/bin/bash
# Monitor HULFT MCP deployment status

AWS_REGION="us-east-1"
AWS_PROFILE="predev"
CLUSTER="hulft-mcp-cluster"
SERVICE="hulft-mcp"
ALB_URL="http://hulft-mcp-alb-32462339.us-east-1.elb.amazonaws.com/mcp"

echo "🔍 Monitoring HULFT MCP Deployment..."
echo ""

# Check service status
echo "📊 Service Status:"
aws ecs describe-services \
  --cluster $CLUSTER \
  --services $SERVICE \
  --region $AWS_REGION \
  --profile $AWS_PROFILE \
  --query 'services[0].{Running:runningCount,Desired:desiredCount,Status:status}' \
  --output table

echo ""

# Check task status
echo "📦 Task Status:"
TASK_ARN=$(aws ecs list-tasks \
  --cluster $CLUSTER \
  --service-name $SERVICE \
  --region $AWS_REGION \
  --profile $AWS_PROFILE \
  --query 'taskArns[0]' \
  --output text)

if [ ! -z "$TASK_ARN" ]; then
  aws ecs describe-tasks \
    --cluster $CLUSTER \
    --tasks $TASK_ARN \
    --region $AWS_REGION \
    --profile $AWS_PROFILE \
    --query 'tasks[0].{Status:lastStatus,Health:healthStatus,CPU:cpu,Memory:memory}' \
    --output table
else
  echo "No tasks found"
fi

echo ""

# Check ALB health
echo "🏥 Load Balancer Health:"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" $ALB_URL 2>/dev/null)
if [ "$HTTP_CODE" == "405" ]; then
  echo "✅ Service is healthy (HTTP $HTTP_CODE)"
elif [ "$HTTP_CODE" == "000" ]; then
  echo "⏳ Service not responding yet (still starting)"
else
  echo "⚠️  Unexpected response (HTTP $HTTP_CODE)"
fi

echo ""

# Check recent logs
echo "📝 Recent Logs:"
aws logs tail /ecs/$SERVICE \
  --since 5m \
  --region $AWS_REGION \
  --profile $AWS_PROFILE \
  2>/dev/null | tail -10 || echo "No logs yet"

echo ""
echo "🔗 Console: https://console.aws.amazon.com/ecs/home?region=$AWS_REGION#/clusters/$CLUSTER/services/$SERVICE"
