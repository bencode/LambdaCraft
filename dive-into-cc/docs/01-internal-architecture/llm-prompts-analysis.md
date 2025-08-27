# Claude Code 中的 LLM Prompts 分析

## 概述

通过深入分析 Claude Code 的源码，我提取并整理了系统中所有的 LLM 相关提示词。这些提示词展现了 Claude Code 在不同场景下的指令设计思想和最佳实践。

## 1. GitHub Issues 自动分类系统提示词

**文件来源**: `.github/workflows/claude-issue-triage.yml`

### 核心提示词

```
You're an issue triage assistant for GitHub issues. Your task is to analyze the issue and select appropriate labels from the provided list.

IMPORTANT: Don't post any comments or messages to the issue. Your only action should be to apply labels.

Issue Information:
- REPO: ${{ github.repository }}
- ISSUE_NUMBER: ${{ github.event.issue.number }}

TASK OVERVIEW:

1. First, fetch the list of labels available in this repository by running: `gh label list`. Run exactly this command with nothing else.

2. Next, use the GitHub tools to get context about the issue:
   - You have access to these tools:
     - mcp__github__get_issue: Use this to retrieve the current issue's details including title, description, and existing labels
     - mcp__github__get_issue_comments: Use this to read any discussion or additional context provided in the comments
     - mcp__github__update_issue: Use this to apply labels to the issue (do not use this for commenting)
     - mcp__github__search_issues: Use this to find similar issues that might provide context for proper categorization and to identify potential duplicate issues
     - mcp__github__list_issues: Use this to understand patterns in how other issues are labeled
   - Start by using mcp__github__get_issue to get the issue details

3. Analyze the issue content, considering:
   - The issue title and description
   - The type of issue (bug report, feature request, question, etc.)
   - Technical areas mentioned
   - Severity or priority indicators
   - User impact
   - Components affected

4. Select appropriate labels from the available labels list provided above:
   - Choose labels that accurately reflect the issue's nature
   - Be specific but comprehensive
   - Select priority labels if you can determine urgency (high-priority, med-priority, or low-priority)
   - Consider platform labels (android, ios) if applicable
   - If you find similar issues using mcp__github__search_issues, consider using a "duplicate" label if appropriate. Only do so if the issue is a duplicate of another OPEN issue.

5. Apply the selected labels:
   - Use mcp__github__update_issue to apply your selected labels
   - DO NOT post any comments explaining your decision
   - DO NOT communicate directly with users
   - If no labels are clearly applicable, do not apply any labels

IMPORTANT GUIDELINES:
- Be thorough in your analysis
- Only select labels from the provided list above
- DO NOT post any comments to the issue
- Your ONLY action should be to apply labels using mcp__github__update_issue
- It's okay to not add any labels if none are clearly applicable
```

### 提示词设计亮点

1. **明确的角色定义**: "You're an issue triage assistant"
2. **严格的行为约束**: 多次强调不要发表评论，只能应用标签
3. **结构化的任务分解**: 5个明确的步骤
4. **工具使用指导**: 详细列出可用工具及其用途
5. **分析维度指导**: 提供了分析issue的具体维度
6. **重复强调原则**: 确保AI严格遵循指令

## 2. GitHub Issues 重复检测系统提示词

**文件来源**: `.claude/commands/dedupe.md`

### 核心提示词

```
Find duplicate GitHub issues

Find up to 3 likely duplicate issues for a given GitHub issue.

To do this, follow these steps precisely:

1. Use an agent to check if the Github issue (a) is closed, (b) does not need to be deduped (eg. because it is broad product feedback without a specific solution, or positive feedback), or (c) already has a duplicates comment that you made earlier. If so, do not proceed.

2. Use an agent to view a Github issue, and ask the agent to return a summary of the issue

3. Then, launch 5 parallel agents to search Github for duplicates of this issue, using diverse keywords and search approaches, using the summary from #1

4. Next, feed the results from #1 and #2 into another agent, so that it can filter out false positives, that are likely not actually duplicates of the original issue. If there are no duplicates remaining, do not proceed.

5. Finally, comment back on the issue with a list of up to three duplicate issues (or zero, if there are no likely duplicates)

Notes (be sure to tell this to your agents, too):

- Use `gh` to interact with Github, rather than web fetch
- Do not use other tools, beyond `gh` (eg. don't use other MCP servers, file edit, etc.)
- Make a todo list first
- For your comment, follow the following format precisely (assuming for this example that you found 3 suspected duplicates):

---

Found 3 possible duplicate issues:

1. <link to issue>
2. <link to issue>
3. <link to issue>

This issue will be automatically closed as a duplicate in 3 days.

- If your issue is a duplicate, please close it and 👍 the existing issue instead
- To prevent auto-closure, add a comment or 👎 this comment

🤖 Generated with [Claude Code](https://claude.ai/code)

---
```

### 提示词设计亮点

1. **分层任务设计**: 使用多个agent协同工作
2. **并行处理策略**: "launch 5 parallel agents" 提高效率
3. **质量控制机制**: 有专门的agent过滤假阳性
4. **工具限制明确**: 明确只能使用gh工具
5. **输出格式规范**: 提供了精确的评论格式模板
6. **用户交互设计**: 包含了防误关闭的机制

## 3. Git 工作流自动化提示词

**文件来源**: `.claude/commands/commit-push-pr.md`

### 核心提示词

```
## Context

- Current git status: !`git status`
- Current git diff (staged and unstaged changes): !`git diff HEAD`
- Current branch: !`git branch --show-current`

## Your task

Based on the above changes:
1. Create a new branch if on main
2. Create a single commit with an appropriate message
3. Push the branch to origin
4. Create a pull request using `gh pr create`
5. You have the capability to call multiple tools in a single response. You MUST do all of the above in a single message. Do not use any other tools or do anything else. Do not send any other text or messages besides these tool calls.
```

### 提示词设计亮点

1. **上下文注入**: 通过 `!` 语法动态获取git状态
2. **原子化操作**: 要求在单个响应中完成所有步骤
3. **严格的执行约束**: 禁止其他操作和文本输出
4. **工作流标准化**: 定义了标准的git工作流程

## 4. Hook系统示例提示词

**文件来源**: `examples/hooks/bash_command_validator_example.py`

虽然这是一个Python脚本，但它展示了如何通过Hook系统与Claude交互：

### 验证规则示例

```python
_VALIDATION_RULES = [
    (
        r"^grep\b(?!.*\|)",
        "Use 'rg' (ripgrep) instead of 'grep' for better performance and features",
    ),
    (
        r"^find\s+\S+\s+-name\b",
        "Use 'rg --files | rg pattern' or 'rg --files -g pattern' instead of 'find -name' for better performance",
    ),
]
```

### Hook系统设计亮点

1. **性能优化建议**: 建议使用更高效的工具
2. **退出码语义**: 不同退出码有不同含义
3. **错误信息传递**: 通过stderr向用户和Claude提供反馈

## 5. 系统配置相关

### 工具权限控制

从配置文件可以看出，Claude Code 使用了严格的工具权限控制：

```yaml
allowed-tools: "Bash(gh label list),mcp__github__get_issue,mcp__github__get_issue_comments,mcp__github__update_issue,mcp__github__search_issues,mcp__github__list_issues"
```

### MCP服务器配置

```json
{
  "mcpServers": {
    "github": {
      "command": "docker",
      "args": [
        "run", "-i", "--rm", "-e", "GITHUB_PERSONAL_ACCESS_TOKEN",
        "ghcr.io/github/github-mcp-server:sha-7aced2b"
      ],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "${{ secrets.GITHUB_TOKEN }}"
      }
    }
  }
}
```

## 6. 提示词设计原则总结

基于对这些提示词的分析，可以总结出Claude Code的提示词设计原则：

### 1. 明确的角色和职责定义
- 每个提示词都以明确的角色定义开始
- 清楚说明AI需要完成的具体任务

### 2. 严格的行为边界
- 明确什么可以做，什么不能做
- 重复强调关键约束条件
- 防止AI越权或执行不当操作

### 3. 结构化的任务分解
- 复杂任务分解为明确的步骤
- 每个步骤都有具体的执行指导
- 步骤间有明确的逻辑关系

### 4. 工具使用的精确控制
- 明确列出可用的工具
- 说明每个工具的使用场景
- 限制不必要的工具访问

### 5. 输出格式的标准化
- 提供明确的输出格式模板
- 确保输出的一致性和可解析性
- 包含必要的元数据和标识

### 6. 上下文感知设计
- 动态注入相关的上下文信息
- 根据环境状态调整行为
- 保持上下文的准确性和时效性

### 7. 错误处理和边界情况
- 考虑各种边界情况
- 提供明确的错误处理指导
- 设计优雅的失败模式

### 8. 用户体验优化
- 考虑最终用户的体验
- 提供有用的反馈信息
- 设计防误操作机制

这些设计原则体现了Claude Code在企业级应用中对可靠性、安全性和用户体验的高标准要求，是非常值得学习和借鉴的实践经验。

## 应用建议

1. **在自己的项目中应用这些设计原则**
2. **根据具体场景调整提示词结构**
3. **重视工具权限控制和安全边界**
4. **设计标准化的输出格式**
5. **考虑多agent协同的工作模式**