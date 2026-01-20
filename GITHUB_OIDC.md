# GitHub Actions OIDC Configuration

This project uses OpenID Connect (OIDC) to authenticate GitHub Actions with AWS, eliminating the need for long-lived AWS credentials.

## Setup Complete ✅

The IAM role `GitHubActionsDeployRole` has been configured to allow GitHub Actions from the `HULFT-Inc/hulft-mcp` repository to assume it.

## GitHub Secret Configuration

Add the following secret to the GitHub repository:

1. Navigate to: https://github.com/HULFT-Inc/hulft-mcp/settings/secrets/actions
2. Click **"New repository secret"**
3. Configure:
   - **Name**: `AWS_ROLE_ARN`
   - **Value**: `arn:aws:iam::486560520867:role/GitHubActionsDeployRole`
4. Click **"Add secret"**

## How It Works

### OIDC Flow

1. GitHub Actions requests a JWT token from GitHub's OIDC provider
2. The workflow presents this token to AWS STS
3. AWS validates the token against the IAM role's trust policy
4. If valid, AWS issues temporary credentials (valid for 1 hour)
5. The workflow uses these credentials to deploy

### Trust Policy

The IAM role trusts GitHub's OIDC provider with the following conditions:

```json
{
  "StringEquals": {
    "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
  },
  "StringLike": {
    "token.actions.githubusercontent.com:sub": [
      "repo:HULFT-Inc/homs-demo-visual:*",
      "repo:HULFT-Inc/hulft-mcp:*"
    ]
  }
}
```

This allows:
- Any branch in the `HULFT-Inc/hulft-mcp` repository
- Both `main` and `develop` branches to deploy

### Workflow Configuration

The workflows are already configured to use OIDC:

```yaml
permissions:
  id-token: write  # Required for OIDC
  contents: read

jobs:
  deploy:
    steps:
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: us-east-1
```

## IAM Permissions

The `GitHubActionsDeployRole` has permissions to:
- Push Docker images to ECR
- Update ECS services
- Read ECS cluster/service information
- Wait for service stability

## Security Benefits

✅ **No long-lived credentials** - Tokens expire after 1 hour
✅ **Scoped access** - Only specific repositories can assume the role
✅ **Audit trail** - All actions logged in CloudTrail
✅ **Automatic rotation** - New tokens issued for each workflow run
✅ **No secret management** - GitHub manages the OIDC tokens

## Testing

After adding the secret, test the deployment:

```bash
# Make a small change
git checkout -b test/oidc-deployment
echo "# test" >> README.md
git add README.md
git commit -m "test: verify OIDC deployment"
git push origin test/oidc-deployment

# Create PR and merge to develop or main
# Watch the GitHub Actions workflow run
```

## Troubleshooting

### Error: "Not authorized to perform sts:AssumeRoleWithWebIdentity"

**Cause**: The repository is not in the trust policy or the secret is incorrect.

**Solution**:
1. Verify the secret value matches: `arn:aws:iam::486560520867:role/GitHubActionsDeployRole`
2. Check the trust policy includes `repo:HULFT-Inc/hulft-mcp:*`

### Error: "Access Denied" during deployment

**Cause**: The IAM role lacks necessary permissions.

**Solution**: Contact AWS admin to add required permissions to `GitHubActionsDeployRole`.

### Workflow doesn't trigger

**Cause**: Workflow file syntax error or branch protection rules.

**Solution**:
1. Check `.github/workflows/deploy-*.yml` syntax
2. Verify branch protection allows Actions to run

## Manual Deployment (Fallback)

If OIDC fails, you can still deploy manually:

```bash
# Build and push
./gradlew build
docker buildx build --platform linux/amd64 -t hulft-mcp:latest .
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 486560520867.dkr.ecr.us-east-1.amazonaws.com
docker tag hulft-mcp:latest 486560520867.dkr.ecr.us-east-1.amazonaws.com/hulft-mcp:latest
docker push 486560520867.dkr.ecr.us-east-1.amazonaws.com/hulft-mcp:latest

# Deploy
aws ecs update-service --cluster hulft-mcp-cluster --service hulft-mcp --force-new-deployment --region us-east-1
```

## References

- [GitHub OIDC Documentation](https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services)
- [AWS IAM OIDC Provider](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_create_oidc.html)
- [configure-aws-credentials Action](https://github.com/aws-actions/configure-aws-credentials)
