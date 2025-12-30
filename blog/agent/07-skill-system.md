# Agent 技能系统实现

系列：[Agent 上下文工程实践指南](/pages/agent/00-index) | 第 7 篇

上一篇：[06. 实战：X-to-Book 多 Agent 系统设计](/pages/agent/x-to-book-case-study)

## 引言

前面6篇文章介绍了上下文工程的理论、技术和实战案例。但如何让这些知识可复用、可移植？Agent Skills 提供了一个轻量级的插件系统，让 Agent 能够按需加载专业知识。本文探讨如何设计和实现这样的技能系统。

本文整合了源项目中 `project-development` 技能和技能系统实现的内容。

## 什么是 Agent Skills

### 定义

Agent Skills 是一种**轻量级知识插件系统**，通过标准化的格式封装专业知识、最佳实践和工具定义，使 Agent 能够动态加载所需的上下文。

### 核心特点

1. **标准化格式**：统一的 SKILL.md 文件格式
2. **渐进式披露**：三层加载策略（元数据 → 完整内容 → 参考资料）
3. **平台无关**：工作于 Claude Code、Cursor、任何支持 Skills 的 Agent
4. **可组合**：技能可以引用其他技能

### 与提示词的区别

| 特性 | 提示词 | Agent Skills |
|------|-------|-------------|
| 结构 | 自由文本 | 标准化 YAML + Markdown |
| 加载方式 | 全量加载 | 渐进式披露 |
| 复用性 | 复制粘贴 | 插件式安装 |
| 可发现性 | 依赖搜索 | 语义索引 |
| 组合性 | 困难 | 原生支持 |

## SKILL.md 文件格式

### 基本结构

```markdown
---
name: context-fundamentals
version: 1.0.0
description: 上下文工程基础知识
tags:
  - context
  - fundamentals
  - attention
dependencies: []
---

# Context Fundamentals

## 概述

简要说明这个技能的内容和用途。

## 何时使用

- 场景1
- 场景2

## 核心概念

### 概念1

详细说明...

### 概念2

详细说明...

## 最佳实践

1. 实践1
2. 实践2

## 示例

\`\`\`typescript
// 代码示例
\`\`\`

## 参考资料

- [链接1](url)
- [链接2](url)
```

### YAML 头部规范

```yaml
---
name: string           # 技能唯一标识（kebab-case）
version: string        # 语义化版本号（semver）
description: string    # 简短描述（<100字符）
tags: string[]         # 标签列表（用于搜索）
dependencies: string[] # 依赖的其他技能
author: string         # 可选：作者
license: string        # 可选：许可证
---
```

### 内容组织规范

**必需部分**：

1. **概述**：1-2段文字说明技能内容
2. **何时使用**：明确的使用场景
3. **核心概念**：关键知识点

**推荐部分**：

4. **最佳实践**：可操作的建议
5. **示例**：代码或配置示例
6. **常见问题**：FAQ
7. **参考资料**：延伸阅读

### 长度限制

- **主文件**：< 500 行（约 15K tokens）
- **原因**：超过 500 行的内容加载效率降低，且难以维护
- **解决方案**：使用 `references/` 目录存储详细内容

## 渐进式披露实现

### 三层加载策略

```
Layer 1: 元数据（Metadata）
- YAML 头部
- 技能名称、描述、标签
- 用于：技能发现和选择

Layer 2: 完整内容（Full Content）
- SKILL.md 主文件
- 用于：当前任务需要的知识

Layer 3: 参考资料（References）
- references/ 目录下的详细文档
- 用于：深入研究特定主题
```

### 实现示例

```typescript
type SkillMetadata = {
  name: string
  version: string
  description: string
  tags: string[]
  dependencies: string[]
}

type Skill = {
  metadata: SkillMetadata
  content?: string
  references?: Map<string, string>
}

class SkillLoader {
  private skills: Map<string, Skill> = new Map()
  private skillsDir: string

  constructor(skillsDir: string) {
    this.skillsDir = skillsDir
  }

  // Layer 1: 加载所有技能的元数据
  async loadAllMetadata(): Promise<SkillMetadata[]> {
    const skillDirs = await readdir(this.skillsDir)
    const metadata: SkillMetadata[] = []

    for (const dir of skillDirs) {
      const skillPath = `${this.skillsDir}/${dir}/SKILL.md`

      if (await exists(skillPath)) {
        const meta = await this.extractMetadata(skillPath)
        metadata.push(meta)

        // 缓存元数据
        this.skills.set(meta.name, { metadata: meta })
      }
    }

    return metadata
  }

  // Layer 2: 按需加载完整内容
  async loadSkillContent(skillName: string): Promise<string> {
    const skill = this.skills.get(skillName)

    if (!skill) {
      throw new Error(`Skill ${skillName} not found`)
    }

    if (skill.content) {
      return skill.content  // 已缓存
    }

    const skillPath = `${this.skillsDir}/${skillName}/SKILL.md`
    const content = await readFile(skillPath, "utf-8")

    // 移除 YAML 头部
    const contentWithoutYAML = content.replace(/^---\n[\s\S]+?\n---\n/, '')

    // 缓存内容
    skill.content = contentWithoutYAML
    this.skills.set(skillName, skill)

    return contentWithoutYAML
  }

  // Layer 3: 按需加载参考资料
  async loadReference(
    skillName: string,
    referenceName: string
  ): Promise<string> {
    const refPath = `${this.skillsDir}/${skillName}/references/${referenceName}.md`

    if (!await exists(refPath)) {
      throw new Error(`Reference ${referenceName} not found for skill ${skillName}`)
    }

    return await readFile(refPath, "utf-8")
  }

  // 提取 YAML 头部
  private async extractMetadata(skillPath: string): Promise<SkillMetadata> {
    const content = await readFile(skillPath, "utf-8")
    const yamlMatch = content.match(/^---\n([\s\S]+?)\n---/)

    if (!yamlMatch) {
      throw new Error(`Invalid SKILL.md format: ${skillPath}`)
    }

    const yaml = yamlMatch[1]
    return parseYAML(yaml)  // 使用 YAML 解析库
  }
}
```

### 使用示例

```typescript
const loader = new SkillLoader("/path/to/skills")

// 启动时：加载所有元数据（快速，<1K tokens）
const allSkills = await loader.loadAllMetadata()
console.log(`Found ${allSkills.length} skills`)

// 显示给用户或系统提示
const skillList = allSkills.map(s => `- ${s.name}: ${s.description}`).join('\n')

// 用户选择技能或 Agent 决定需要某技能
const userChoice = "context-fundamentals"

// 按需加载完整内容
const skillContent = await loader.loadSkillContent(userChoice)

// 如果需要深入参考
const detailedGuide = await loader.loadReference(
  userChoice,
  "attention-mechanisms"
)
```

### 性能对比

| 加载方式 | Token 数 | 加载时间 |
|---------|---------|---------|
| 全量加载 10 个技能 | 150K | 慢 |
| 仅元数据 | 2K | 快 |
| 元数据 + 1 个技能内容 | 17K | 快 |
| 元数据 + 1 个技能 + 1 个参考 | 35K | 中等 |

## 目录结构

### 推荐结构

```
skills/
├── context-fundamentals/
│   ├── SKILL.md                 # 主文件（<500行）
│   └── references/              # 详细参考资料
│       ├── attention-mechanisms.md
│       ├── ruler-benchmark.md
│       └── lost-in-middle.md
├── context-optimization/
│   ├── SKILL.md
│   └── references/
│       ├── compression-methods.md
│       └── kv-cache.md
├── multi-agent-patterns/
│   ├── SKILL.md
│   └── references/
│       ├── supervisor-pattern.md
│       ├── swarm-pattern.md
│       └── hierarchical-pattern.md
└── README.md                    # 技能集概览
```

### references/ 目录

用于存储详细内容，避免 SKILL.md 过长：

**何时使用 references/**：

- 研究论文摘要和详细分析
- 复杂的代码示例和完整实现
- 历史背景和深度讨论
- 替代方案的详细对比

**命名规范**：

- 使用 kebab-case
- 名称反映内容（如 `ruler-benchmark.md` 而非 `ref1.md`）

## 语义知识注册表

### 为什么需要语义检索

基于文件名和标签的检索不够：

```
用户问题："如何避免中间部分信息丢失？"

基于关键词：难以匹配到 "lost-in-middle" 技能
基于语义：可以匹配到相关技能
```

### 架构

```
PostgreSQL + pgvector
  ↓
技能内容 → 文本嵌入 → 向量存储
  ↓
用户查询 → 查询嵌入 → 向量搜索 → 相关技能
```

### Schema

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE skills (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) UNIQUE NOT NULL,
  version VARCHAR(50) NOT NULL,
  description TEXT NOT NULL,
  tags TEXT[] NOT NULL,
  content TEXT NOT NULL,
  embedding vector(1536),  -- OpenAI text-embedding-3-small 的维度
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX ON skills USING ivfflat (embedding vector_cosine_ops);

CREATE TABLE skill_references (
  id SERIAL PRIMARY KEY,
  skill_id INTEGER REFERENCES skills(id),
  reference_name VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  embedding vector(1536),
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX ON skill_references USING ivfflat (embedding vector_cosine_ops);
```

### 索引脚本

```python
import os
import openai
from pathlib import Path
import psycopg2
import yaml

def get_embedding(text: str) -> list[float]:
    response = openai.embeddings.create(
        model="text-embedding-3-small",
        input=text
    )
    return response.data[0].embedding

def index_skill(conn, skill_dir: Path):
    skill_file = skill_dir / "SKILL.md"

    with open(skill_file, 'r') as f:
        content = f.read()

    # 提取 YAML 头部
    yaml_match = re.match(r'^---\n([\s\S]+?)\n---\n([\s\S]+)$', content)
    if not yaml_match:
        print(f"Invalid format: {skill_file}")
        return

    metadata = yaml.safe_load(yaml_match.group(1))
    skill_content = yaml_match.group(2)

    # 生成嵌入
    embedding = get_embedding(skill_content)

    # 插入数据库
    with conn.cursor() as cur:
        cur.execute("""
            INSERT INTO skills (name, version, description, tags, content, embedding)
            VALUES (%s, %s, %s, %s, %s, %s)
            ON CONFLICT (name) DO UPDATE
            SET version = EXCLUDED.version,
                description = EXCLUDED.description,
                tags = EXCLUDED.tags,
                content = EXCLUDED.content,
                embedding = EXCLUDED.embedding,
                updated_at = NOW()
            RETURNING id
        """, (
            metadata['name'],
            metadata['version'],
            metadata['description'],
            metadata.get('tags', []),
            skill_content,
            embedding
        ))

        skill_id = cur.fetchone()[0]

        # 索引 references
        refs_dir = skill_dir / "references"
        if refs_dir.exists():
            for ref_file in refs_dir.glob("*.md"):
                index_reference(conn, skill_id, ref_file)

    conn.commit()

def index_reference(conn, skill_id: int, ref_file: Path):
    with open(ref_file, 'r') as f:
        content = f.read()

    embedding = get_embedding(content)

    with conn.cursor() as cur:
        cur.execute("""
            INSERT INTO skill_references (skill_id, reference_name, content, embedding)
            VALUES (%s, %s, %s, %s)
        """, (skill_id, ref_file.stem, content, embedding))

def index_all_skills(skills_dir: str, db_url: str):
    conn = psycopg2.connect(db_url)

    for skill_dir in Path(skills_dir).iterdir():
        if skill_dir.is_dir() and (skill_dir / "SKILL.md").exists():
            print(f"Indexing {skill_dir.name}...")
            index_skill(conn, skill_dir)

    conn.close()

if __name__ == "__main__":
    index_all_skills(
        "/path/to/skills",
        "postgresql://localhost/skills"
    )
```

### 检索 API

```python
def search_skills(query: str, limit: int = 5) -> list[dict]:
    embedding = get_embedding(query)

    conn = psycopg2.connect(DB_URL)
    with conn.cursor() as cur:
        cur.execute("""
            SELECT name, description, content,
                   1 - (embedding <=> %s::vector) AS similarity
            FROM skills
            ORDER BY embedding <=> %s::vector
            LIMIT %s
        """, (embedding, embedding, limit))

        results = []
        for row in cur.fetchall():
            results.append({
                "name": row[0],
                "description": row[1],
                "content": row[2],
                "similarity": row[3]
            })

    conn.close()
    return results

# 使用示例
results = search_skills("如何避免中间部分信息丢失", limit=3)

for r in results:
    print(f"{r['name']}: {r['description']} (相似度: {r['similarity']:.2f})")
    print(r['content'][:200])
    print()
```

## 项目集成

### Claude Code 集成

Claude Code 原生支持 Agent Skills：

```bash
# 安装技能集
/plugin marketplace add muratcankoylan/Agent-Skills-for-Context-Engineering

# 启用技能
/plugin enable context-fundamentals

# 查看已安装技能
/plugin list

# 禁用技能
/plugin disable context-fundamentals
```

### 自定义集成

在自己的项目中集成 Skills：

```typescript
// skillsConfig.json
{
  "skillsDir": "./skills",
  "autoLoad": ["context-fundamentals", "tool-design"],
  "semanticSearch": {
    "enabled": true,
    "endpoint": "postgresql://localhost/skills"
  }
}
```

```typescript
import { SkillLoader } from "./skill-loader"

class AgentWithSkills {
  private skillLoader: SkillLoader
  private activeSkills: Set<string> = new Set()

  async initialize() {
    this.skillLoader = new SkillLoader(config.skillsDir)

    // 加载所有元数据
    await this.skillLoader.loadAllMetadata()

    // 自动加载配置的技能
    for (const skillName of config.autoLoad) {
      await this.activateSkill(skillName)
    }
  }

  async activateSkill(skillName: string): Promise<void> {
    const content = await this.skillLoader.loadSkillContent(skillName)
    this.activeSkills.add(skillName)

    // 将技能内容加入系统提示
    this.updateSystemPrompt()
  }

  async handleUserQuery(query: string): Promise<string> {
    // 可选：语义搜索相关技能
    if (config.semanticSearch.enabled) {
      const relevantSkills = await searchSkills(query, 2)

      for (const skill of relevantSkills) {
        if (!this.activeSkills.has(skill.name)) {
          await this.activateSkill(skill.name)
        }
      }
    }

    // 使用激活的技能处理查询
    return await this.processQuery(query)
  }

  private updateSystemPrompt(): void {
    const skillContents = Array.from(this.activeSkills)
      .map(name => this.skillLoader.skills.get(name)?.content)
      .filter(c => c)
      .join('\n\n---\n\n')

    this.systemPrompt = `
You are an AI assistant with access to specialized skills.

<active_skills>
${skillContents}
</active_skills>

Use the knowledge from these skills to answer user questions.
`
  }
}
```

## 创建自己的技能

### 检查清单

**设计阶段**：

- [ ] 技能有明确的范围和目标？
- [ ] 名称是描述性的且唯一？
- [ ] 依赖关系清晰？

**内容阶段**：

- [ ] YAML 头部完整且有效？
- [ ] 概述清晰（1-2段）？
- [ ] "何时使用"明确？
- [ ] 核心概念结构化？
- [ ] 示例可执行？
- [ ] 主文件 < 500 行？

**质量阶段**：

- [ ] 内容准确且经过验证？
- [ ] 代码示例已测试？
- [ ] 引用了权威参考资料？
- [ ] 标签和描述有助于发现？

**发布阶段**：

- [ ] 版本号遵循 semver？
- [ ] 变更日志记录？
- [ ] 示例项目可用？

### 示例：创建 "API Design" 技能

```markdown
---
name: api-design
version: 1.0.0
description: RESTful API 设计最佳实践
tags:
  - api
  - rest
  - design
  - web
dependencies:
  - tool-design
author: Your Name
license: MIT
---

# API Design

## 概述

本技能总结了 RESTful API 设计的最佳实践，包括资源建模、URL 设计、HTTP 方法使用、错误处理和版本管理。

## 何时使用

- 设计新的 RESTful API
- 重构现有 API
- 审查 API 设计

## 核心概念

### 资源建模

资源是 API 的核心抽象。资源应该：

1. **使用名词而非动词**
   - 好：`GET /users`
   - 差：`GET /getUsers`

2. **使用复数形式**
   - 好：`GET /users/123`
   - 差：`GET /user/123`

3. **嵌套表示关系**
   - `GET /users/123/posts`（用户的帖子）
   - `GET /posts?user_id=123`（另一种方式）

### HTTP 方法

| 方法 | 用途 | 幂等性 | 安全性 |
|------|------|--------|--------|
| GET | 获取资源 | 是 | 是 |
| POST | 创建资源 | 否 | 否 |
| PUT | 完整更新 | 是 | 否 |
| PATCH | 部分更新 | 否 | 否 |
| DELETE | 删除资源 | 是 | 否 |

### 错误处理

使用标准 HTTP 状态码，并提供详细的错误信息：

\`\`\`json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid email format",
    "details": {
      "field": "email",
      "value": "not-an-email",
      "constraint": "must be a valid email address"
    }
  }
}
\`\`\`

## 最佳实践

1. **版本控制**：使用 URL 版本（`/v1/users`）或 Header 版本
2. **分页**：对列表资源提供分页
3. **过滤和排序**：支持查询参数（`?sort=-created_at&status=active`）
4. **HATEOAS**：可选，提供超链接导航

## 示例

### 完整的用户管理 API

\`\`\`typescript
// 获取用户列表
GET /v1/users?page=1&limit=20&sort=-created_at

// 获取单个用户
GET /v1/users/123

// 创建用户
POST /v1/users
{
  "name": "Alice",
  "email": "alice@example.com"
}

// 更新用户
PATCH /v1/users/123
{
  "name": "Alice Smith"
}

// 删除用户
DELETE /v1/users/123

// 获取用户的帖子
GET /v1/users/123/posts
\`\`\`

## 常见问题

**Q: PUT 和 PATCH 的区别？**
A: PUT 要求提供完整的资源表示，PATCH 只需要提供要更新的字段。

**Q: 如何处理批量操作？**
A: 可以使用 POST 到特殊端点，如 `POST /users/batch-delete`。

## 参考资料

- [REST API Tutorial](https://restfulapi.net/)
- [Microsoft REST API Guidelines](https://github.com/microsoft/api-guidelines)
```

## 实践检查清单

### 技能系统设计检查
- [ ] 是否使用标准化的 SKILL.md 格式？
- [ ] 是否实现了渐进式披露？
- [ ] 是否有语义检索能力？

### 技能内容检查
- [ ] YAML 头部是否完整？
- [ ] 主文件是否 < 500 行？
- [ ] 是否有清晰的"何时使用"说明？
- [ ] 代码示例是否可执行？

### 集成检查
- [ ] 是否易于在项目中集成？
- [ ] 是否支持按需加载？
- [ ] 是否有配置文件管理技能？

## 下一步

通过构建技能系统，我们让上下文工程的知识变得可复用和可移植。最后一篇文章将总结整个系列，提炼8个关键洞察。

下一篇：[08. 总结：8个关键洞察](/pages/agent/key-insights)

## 参考资料

- [Agent Skills for Context Engineering](https://github.com/muratcankoylan/Agent-Skills-for-Context-Engineering)
- [Agent Skills 开放标准](https://github.com/skills)
- [pgvector Documentation](https://github.com/pgvector/pgvector)
