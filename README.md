# 🐧 Penguin PR Bot

> An AI-powered GitHub PR review and automation bot built with **Spring Boot 3** and **Google Gemini AI**.

---

## ✨ Features

- 🔍 **AI Code Review (`/review`)**: Automatically reviews Pull Request diffs, identifies bugs, security vulnerabilities, and code quality improvements with context-aware suggestions.
- 📝 **PR Summarization (`/summarize`)**: Generates high-level summaries of PR changes, architectural impacts, and modified files.
- 🏷️ **Automated Labelling (`/label <name>`)**: Adds labels to pull requests dynamically via chat commands.
- ✅ **PR Approval (`/approve [message]`)**: Approves pull requests directly from comments.
- 🧹 **Cleanup (`/delete-pr-bot-comments`)**: Cleans up previous bot comments on the PR to keep conversation clean.
- 🔄 **Outdated Review Marking**: Marks previous review comments as outdated when new commits are pushed (`synchronize` event).
- ⚙️ **Repository-level Configuration**: Supports `.penguin.yml` in repositories for custom review instructions and automation rules.

---

## 🛠️ Tech Stack

- **Java 17**
- **Spring Boot 3.3.4** (Web, MVC)
- **Google GenAI SDK** (Gemini 2.5 Flash)
- **Hub4j GitHub API** & **GitHub REST API**
- **JJWT (Java JWT 0.12.6)** & **BouncyCastle** for GitHub App authentication
- **Maven**

---

## 🚀 Getting Started

### 1. Prerequisites
- Java 17+
- Maven 3.8+
- GitHub App created with required permissions (Pull requests, Issues, Webhooks)
- Google Gemini API Key

### 2. Configuration
Set the following environment variables or configure them in `application.yml`:

```env
PENGUIN_GITHUB_APP_ID=your_github_app_id
PENGUIN_GITHUB_PRIVATE_KEY_PATH=/path/to/private-key.pem
PENGUIN_GITHUB_WEBHOOK_SECRET=your_webhook_secret
PENGUIN_GEMINI_API_KEY=your_gemini_api_key
PENGUIN_GEMINI_MODEL=gemini-2.5-flash
PENGUIN_BOT_NAME=Penguin PR Bot
```

### 3. Build and Run
```bash
# Build
mvn clean package

# Run
mvn spring-boot:run
```

---

## 🤖 Bot Commands

| Command | Description |
|---|---|
| `/review` | Triggers an AI-powered code review of the PR diff |
| `/summarize` | Generates a concise summary of the PR changes |
| `/label <name>` | Adds a label to the PR |
| `/approve [message]` | Approves the PR with an optional comment |
| `/delete-pr-bot-comments` | Deletes previous bot comments on the PR |
| `/help` | Shows available bot commands and documentation |
