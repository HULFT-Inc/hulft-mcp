---
name: "hulft-mcp"
displayName: "HULFT MCP Server"
description: "Work-focused MCP server providing Jira, Fireflies, Outlook, and time tracking integrations for HULFT operations"
keywords: ["hulft", "jira", "fireflies", "outlook", "time-tracking", "work", "productivity"]
author: "Drew Stoneburger"
---

# HULFT MCP Server

## Overview

The HULFT MCP Server provides comprehensive work-focused integrations for daily productivity tasks. Built from the proven JFunctions toolkit, it exposes key operations as MCP tools that can be used directly within Kiro conversations.

**Key Integrations:**
- **Jira** - Issue management, search, transitions, and comments for HULFT shared instance
- **Time Tracking** - Everit/Stagil time logging, reports, and worklog management
- **Fireflies** - Meeting summaries, search, and transcript access
- **Outlook** - Email operations, calendar management, and contact search

This power enables seamless workflow automation where you can create Jira issues, log time, send emails, and access meeting summaries all through natural conversation with Kiro.

## Onboarding

### Prerequisites

**System Requirements:**
- Java 17+
- AWS CLI configured with appropriate permissions
- Access to HULFT Jira instance
- Fireflies API access
- Outlook/Microsoft Graph API access
- Everit Time Tracker permissions

**AWS Secrets Manager Setup:**
The MCP server reads all configuration from AWS Secrets Manager. You'll need to create secrets for each service integration.

### Installation

#### Step 1: Clone and Build MCP Server

```bash
# Clone the HULFT MCP server repository
git clone <repository-url> hulft-mcp-server
cd hulft-mcp-server

# Build the server
./gradlew build

# Verify build
java -jar build/libs/hulft-mcp-server.jar --version
```

#### Step 2: Configure AWS Secrets

Create the following secrets in AWS Secrets Manager:

**Jira Configuration (`hulft/jira/config`):**
```json
{
  "baseUrl": "https://your-company.atlassian.net",
  "username": "your-email@company.com",
  "apiToken": "your-jira-api-token",
  "defaultProject": "PROJECT-KEY"
}
```

**Fireflies Configuration (`hulft/fireflies/config`):**
```json
{
  "apiKey": "your-fireflies-api-key",
  "baseUrl": "https://api.fireflies.ai/graphql"
}
```

**Outlook Configuration (`hulft/outlook/config`):**
```json
{
  "clientId": "your-azure-app-client-id",
  "clientSecret": "your-azure-app-client-secret",
  "tenantId": "your-azure-tenant-id",
  "redirectUri": "http://localhost:8080/auth/callback"
}
```

**Time Tracking Configuration (`hulft/timetracking/config`):**
```json
{
  "everitBaseUrl": "https://your-everit-instance.com/api",
  "everitApiKey": "your-everit-api-key",
  "stagilBaseUrl": "https://your-stagil-instance.com/api",
  "stagilApiKey": "your-stagil-api-key"
}
```

#### Step 3: Test Configuration

```bash
# Test AWS Secrets access
aws secretsmanager get-secret-value --secret-id hulft/jira/config

# Test MCP server startup
java -jar build/libs/hulft-mcp-server.jar --test-config
```

### Configuration in Kiro

Add the HULFT MCP server to your Kiro configuration:

**File: `.kiro/settings/mcp.json`**
```json
{
  "mcpServers": {
    "hulft": {
      "command": "java",
      "args": ["-jar", "/path/to/hulft-mcp-server/build/libs/hulft-mcp-server.jar"],
      "env": {
        "AWS_PROFILE": "default",
        "AWS_REGION": "us-east-1"
      }
    }
  }
}
```

## Available MCP Tools

### Jira Operations

#### `jira_create_issue`
Create a new Jira issue in the HULFT shared instance.

**Parameters:**
- `project` (string, required): Project key (e.g., "PROJ")
- `issueType` (string, required): Issue type ("Story", "Bug", "Task", etc.)
- `summary` (string, required): Issue title
- `description` (string, optional): Issue description
- `assignee` (string, optional): Assignee username
- `priority` (string, optional): Priority level
- `labels` (array, optional): Issue labels

**Example:**
```json
{
  "project": "HULFT",
  "issueType": "Story",
  "summary": "Implement new feature",
  "description": "Detailed description of the feature",
  "assignee": "john.doe",
  "priority": "High",
  "labels": ["backend", "api"]
}
```

#### `jira_get_issue`
Retrieve detailed information about a Jira issue.

**Parameters:**
- `issueKey` (string, required): Issue key (e.g., "HULFT-123")

#### `jira_search_issues`
Search for issues using JQL (Jira Query Language).

**Parameters:**
- `jql` (string, required): JQL query
- `maxResults` (number, optional): Maximum results to return (default: 50)
- `fields` (array, optional): Specific fields to return

**Example:**
```json
{
  "jql": "assignee = currentUser() AND status != Done",
  "maxResults": 20,
  "fields": ["summary", "status", "assignee", "priority"]
}
```

#### `jira_get_my_tasks`
Get issues assigned to the current user.

**Parameters:**
- `status` (string, optional): Filter by status ("In Progress", "To Do", etc.)
- `maxResults` (number, optional): Maximum results (default: 25)

#### `jira_transition_issue`
Change the status of a Jira issue.

**Parameters:**
- `issueKey` (string, required): Issue key
- `transitionName` (string, required): Transition name ("In Progress", "Done", etc.)
- `comment` (string, optional): Comment to add with transition

#### `jira_add_comment`
Add a comment to a Jira issue.

**Parameters:**
- `issueKey` (string, required): Issue key
- `comment` (string, required): Comment text

#### `jira_update_issue`
Update fields on a Jira issue.

**Parameters:**
- `issueKey` (string, required): Issue key
- `fields` (object, required): Fields to update

**Example:**
```json
{
  "issueKey": "HULFT-123",
  "fields": {
    "summary": "Updated summary",
    "priority": "Critical",
    "labels": ["urgent", "hotfix"]
  }
}
```

### Time Tracking Operations

#### `time_log_work`
Log time to a Jira issue using Everit Time Tracker.

**Parameters:**
- `issueKey` (string, required): Jira issue key
- `timeSpent` (string, required): Time in format "1h 30m" or seconds
- `startDate` (string, optional): Start date (ISO format)
- `comment` (string, optional): Work description

**Example:**
```json
{
  "issueKey": "HULFT-123",
  "timeSpent": "2h 15m",
  "startDate": "2024-12-18T09:00:00Z",
  "comment": "Implemented authentication module"
}
```

#### `time_get_worklog`
Get details of a specific worklog entry.

**Parameters:**
- `worklogId` (string, required): Everit worklog ID

#### `time_summary_report`
Generate time summary report for a date range.

**Parameters:**
- `startDate` (string, required): Start date (YYYY-MM-DD)
- `endDate` (string, required): End date (YYYY-MM-DD)
- `user` (string, optional): Specific user (default: current user)

#### `time_details_report`
Generate detailed time report with individual entries.

**Parameters:**
- `startDate` (string, required): Start date (YYYY-MM-DD)
- `endDate` (string, required): End date (YYYY-MM-DD)
- `user` (string, optional): Specific user (default: current user)

#### `time_update_worklog`
Update an existing worklog entry.

**Parameters:**
- `worklogId` (string, required): Everit worklog ID
- `timeSpent` (string, optional): New time spent
- `comment` (string, optional): New comment
- `startDate` (string, optional): New start date

#### `time_delete_worklog`
Delete a worklog entry.

**Parameters:**
- `worklogId` (string, required): Everit worklog ID

### Fireflies Operations

#### `fireflies_list_meetings`
List recent meetings from Fireflies.

**Parameters:**
- `limit` (number, optional): Number of meetings to return (default: 10)
- `startDate` (string, optional): Filter meetings after this date
- `endDate` (string, optional): Filter meetings before this date

#### `fireflies_get_meeting`
Get detailed information about a specific meeting.

**Parameters:**
- `meetingId` (string, required): Fireflies meeting ID

#### `fireflies_search_meetings`
Search meetings by title, participants, or content.

**Parameters:**
- `query` (string, required): Search query
- `limit` (number, optional): Maximum results (default: 10)

#### `fireflies_get_summary`
Get AI-generated summary of a meeting.

**Parameters:**
- `meetingId` (string, required): Fireflies meeting ID
- `summaryType` (string, optional): Type of summary ("overview", "action_items", "questions")

### Outlook Operations

#### `outlook_send_email`
Send an email through Outlook.

**Parameters:**
- `to` (array, required): Recipient email addresses
- `subject` (string, required): Email subject
- `body` (string, required): Email body (HTML or plain text)
- `cc` (array, optional): CC recipients
- `bcc` (array, optional): BCC recipients
- `attachments` (array, optional): File attachments

**Example:**
```json
{
  "to": ["colleague@company.com"],
  "subject": "Project Update",
  "body": "Here's the latest update on the project...",
  "cc": ["manager@company.com"]
}
```

#### `outlook_read_email`
Read a specific email by ID.

**Parameters:**
- `messageId` (string, required): Outlook message ID

#### `outlook_search_email`
Search emails by criteria.

**Parameters:**
- `query` (string, required): Search query
- `folder` (string, optional): Folder to search in (default: "inbox")
- `limit` (number, optional): Maximum results (default: 10)

#### `outlook_list_calendar`
List calendar events.

**Parameters:**
- `startDate` (string, optional): Start date filter
- `endDate` (string, optional): End date filter
- `limit` (number, optional): Maximum events (default: 10)

#### `outlook_create_calendar`
Create a new calendar event.

**Parameters:**
- `subject` (string, required): Event title
- `startTime` (string, required): Start time (ISO format)
- `endTime` (string, required): End time (ISO format)
- `attendees` (array, optional): Attendee email addresses
- `location` (string, optional): Event location
- `body` (string, optional): Event description

## Common Workflows

### Workflow 1: Daily Task Management

**Goal:** Check assigned tasks, update status, and log time

**Steps:**
1. **Get your current tasks:**
   ```
   Use jira_get_my_tasks with status filter
   ```

2. **Update task status:**
   ```
   Use jira_transition_issue to move tasks to "In Progress"
   ```

3. **Log work time:**
   ```
   Use time_log_work to record time spent
   ```

4. **Add progress comments:**
   ```
   Use jira_add_comment to document progress
   ```

### Workflow 2: Meeting Follow-up

**Goal:** Process meeting recordings and create follow-up tasks

**Steps:**
1. **Find recent meetings:**
   ```
   Use fireflies_list_meetings for today's meetings
   ```

2. **Get meeting summary:**
   ```
   Use fireflies_get_summary to extract action items
   ```

3. **Create Jira issues for action items:**
   ```
   Use jira_create_issue for each action item
   ```

4. **Send follow-up email:**
   ```
   Use outlook_send_email to share action items with team
   ```

### Workflow 3: Weekly Time Reporting

**Goal:** Generate and share weekly time reports

**Steps:**
1. **Generate time summary:**
   ```
   Use time_summary_report for the week
   ```

2. **Get detailed breakdown:**
   ```
   Use time_details_report for specific projects
   ```

3. **Email report to manager:**
   ```
   Use outlook_send_email with time report attached
   ```

## Troubleshooting

### MCP Server Connection Issues

**Problem:** MCP server won't start or connect
**Symptoms:**
- Error: "Connection refused"
- Server not responding in Kiro

**Solutions:**
1. **Verify Java installation:**
   ```bash
   java -version  # Should show Java 17+
   ```

2. **Check server startup:**
   ```bash
   java -jar build/libs/hulft-mcp-server.jar --test
   ```

3. **Verify AWS credentials:**
   ```bash
   aws sts get-caller-identity
   ```

4. **Check Kiro MCP configuration:**
   - Ensure correct path to JAR file
   - Verify environment variables are set

### AWS Secrets Manager Issues

**Error:** "Unable to retrieve secret"
**Cause:** Missing AWS permissions or incorrect secret names
**Solution:**
1. **Verify AWS permissions:**
   ```bash
   aws secretsmanager list-secrets
   ```

2. **Check secret exists:**
   ```bash
   aws secretsmanager get-secret-value --secret-id hulft/jira/config
   ```

3. **Verify IAM permissions:**
   - `secretsmanager:GetSecretValue`
   - `secretsmanager:DescribeSecret`

### Jira Authentication Errors

**Error:** "401 Unauthorized"
**Cause:** Invalid API token or expired credentials
**Solution:**
1. **Generate new API token:**
   - Go to Atlassian Account Settings
   - Create new API token
   - Update secret in AWS Secrets Manager

2. **Verify Jira URL:**
   - Ensure baseUrl in secret is correct
   - Test with browser access

### Fireflies API Issues

**Error:** "GraphQL query failed"
**Cause:** Invalid API key or query syntax
**Solution:**
1. **Verify API key:**
   - Check Fireflies dashboard for valid API key
   - Update secret in AWS Secrets Manager

2. **Test API access:**
   ```bash
   curl -H "Authorization: Bearer YOUR_API_KEY" \
        https://api.fireflies.ai/graphql
   ```

### Outlook Authentication Problems

**Error:** "OAuth token expired"
**Cause:** Microsoft Graph token needs refresh
**Solution:**
1. **Re-authenticate:**
   - MCP server will prompt for re-authentication
   - Follow OAuth flow in browser

2. **Check Azure app registration:**
   - Verify client ID and secret
   - Ensure correct redirect URI

### Time Tracking Errors

**Error:** "Worklog not found"
**Cause:** Invalid worklog ID or permissions
**Solution:**
1. **Use correct worklog ID:**
   - Everit IDs are different from Jira worklog IDs
   - Use `time_summary_report` to find correct IDs

2. **Verify Everit permissions:**
   - Ensure user has time tracking permissions
   - Check Everit API access

## Best Practices

### Security
- **Never hardcode credentials** - Always use AWS Secrets Manager
- **Rotate API tokens regularly** - Set up automated rotation where possible
- **Use least privilege** - Grant minimal required permissions
- **Monitor API usage** - Watch for unusual activity patterns

### Performance
- **Cache frequently used data** - Reduce API calls where possible
- **Use appropriate timeouts** - Don't let requests hang indefinitely
- **Batch operations** - Combine multiple updates when supported
- **Respect rate limits** - Implement proper backoff strategies

### Workflow Optimization
- **Standardize issue templates** - Use consistent formats for better automation
- **Tag issues appropriately** - Enable better searching and filtering
- **Document time consistently** - Use clear, searchable work descriptions
- **Link related items** - Connect Jira issues, meetings, and emails

### Error Handling
- **Log all errors** - Maintain detailed logs for troubleshooting
- **Provide clear error messages** - Help users understand what went wrong
- **Implement retry logic** - Handle temporary network issues gracefully
- **Validate inputs** - Check parameters before making API calls

## MCP Config Placeholders

**IMPORTANT:** Before using this power, replace the following placeholders in `mcp.json` with your actual values:

- **`HULFT_MCP_SERVER_PATH`**: Path to the HULFT MCP server installation directory.
  - **How to set it:**
    1. Build the MCP server: `cd hulft-mcp-server && ./gradlew build`
    2. Note the project directory path (e.g., `/Users/yourname/hulft-mcp-server`)
    3. Replace placeholder with the full path to the project directory

- **`AWS_PROFILE_NAME`**: Your AWS profile name for accessing Secrets Manager.
  - **How to get it:**
    1. List available profiles: `aws configure list-profiles`
    2. Use your desired profile name (e.g., "default", "work", "hulft")
    3. Ensure the profile has Secrets Manager permissions

- **`AWS_REGION_NAME`**: AWS region where your secrets are stored.
  - **How to set it:** Use the region where you created the secrets (e.g., "us-east-1", "us-west-2")
  - Check current region: `aws configure get region`

**After replacing placeholders, your mcp.json should look like:**
```json
{
  "mcpServers": {
    "hulft": {
      "command": "java",
      "args": ["-jar", "/Users/yourname/hulft-mcp-server/build/libs/hulft-mcp-server.jar"],
      "env": {
        "AWS_PROFILE": "default",
        "AWS_REGION": "us-east-1",
        "LOG_LEVEL": "INFO"
      }
    }
  }
}
```

## Configuration

### Environment Variables

The MCP server uses these environment variables:

- `AWS_PROFILE` - AWS profile to use (default: "default")
- `AWS_REGION` - AWS region for Secrets Manager (default: "us-east-1")
- `LOG_LEVEL` - Logging level ("DEBUG", "INFO", "WARN", "ERROR")
- `MCP_SERVER_PORT` - Port for MCP server (default: auto-assigned)

### AWS IAM Policy

Required IAM policy for the MCP server:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": [
        "arn:aws:secretsmanager:*:*:secret:hulft/*"
      ]
    }
  ]
}
```

### Secret Rotation

Set up automatic rotation for sensitive secrets:

1. **Jira API tokens** - Rotate every 90 days
2. **Fireflies API keys** - Rotate every 180 days
3. **Azure client secrets** - Rotate every 365 days

---

**MCP Server:** hulft-mcp-server
**Repository:** TBD - Will be created during implementation
