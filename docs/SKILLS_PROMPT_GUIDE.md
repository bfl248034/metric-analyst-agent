# Skills Markdown + System Prompt 集成方案

## 方案概述

将 Skills 能力写成 Markdown 文档，加载后注入 System Prompt，让大模型更好地理解系统能力。

## 文件结构

```
docs/skills/
├── metric-query-skill.md           # 指标查询 Skill 描述
├── indicator-recognition-skill.md  # 指标识别 Skill 描述
├── dimension-normalization-skill.md # 维度标准化 Skill 描述
└── chitchat-skill.md               # 闲聊处理 Skill 描述
```

## 核心组件

### 1. SkillPromptLoader

**职责**: 加载 Markdown 文件，构建 System Prompt

**功能**:
- 从 `docs/skills/*.md` 加载所有 Skill 描述
- 解析 Markdown，提取关键信息
- 构建完整的 System Prompt
- 支持按需构建精简 Prompt

**使用方式**:
```java
@Autowired
private SkillPromptLoader promptLoader;

// 获取完整 System Prompt
String systemPrompt = promptLoader.getSystemPrompt();

// 获取特定 Skill 描述
String metricSkill = promptLoader.getSkillDescription("metric-query-skill");

// 构建精简 Prompt（只包含指定 Skills）
String targetedPrompt = promptLoader.buildTargetedPrompt(
    List.of("metric-query-skill", "chitchat-skill")
);
```

### 2. EnhancedMetricAnalystAgent

**职责**: 使用增强的 System Prompt 处理请求

**特点**:
- 加载 Skills 描述注入 Prompt
- 大模型更清楚系统能力边界
- 生成更准确的引导回复和洞察

## System Prompt 结构

```
# Metric Analyst Agent

你是 Metric Analyst（数据分析智能体）...

## 核心能力
...

## 工作方式
1. 闲聊识别
2. 指标识别
3. 维度标准化
4. 数据查询
5. 结果生成

## 可用 Skills

### MetricQuerySkill
- **Skill ID**: metric_query
- **功能**: 查询各类经济指标数据
- **可用工具**: queryMetricCurrentValue, queryMetricTrend, queryMetricRanking...

### IndicatorRecognitionSkill
- **Skill ID**: indicator_recognition
- **功能**: 从用户输入中识别指标意图
...

## 回复风格
...

## 能力边界
✅ 你能做的：...
❌ 你不能做的：...
```

## 效果对比

### 不使用 Skills Prompt（传统方式）

```
System Prompt: "你是一个数据分析助手，可以查询招聘、企业、专利数据"

User: "北京招聘薪资"
LLM: 直接调用工具（可能忽略维度标准化步骤）
```

### 使用 Skills Prompt（本方案）

```
System Prompt: 包含完整的 Skills 描述、工具参数、使用示例

User: "北京招聘薪资"
LLM: 
1. 理解需要调用 IndicatorRecognitionSkill 识别指标
2. 然后调用 DimensionNormalizationSkill 标准化地区
3. 最后调用 MetricQuerySkill.queryMetricCurrentValue
4. 生成回复时参考回复风格指引
```

## 优势

| 优势 | 说明 |
|------|------|
| **能力边界清晰** | 大模型知道能做什么、不能做什么 |
| **工具使用规范** | 明确每个工具的参数和使用场景 |
| **回复风格一致** | 统一的回复风格和格式要求 |
| **易于维护** | 改 Markdown 即可，无需改代码 |
| **多 Agent 扩展** | 不同 Agent 加载不同 Skills 组合 |

## 使用示例

### 示例1: 基础使用

```java
@Service
public class QueryService {
    
    @Autowired
    private SkillPromptLoader promptLoader;
    
    @Autowired
    private ChatModel chatModel;
    
    public String process(String userInput) {
        // 1. 获取 System Prompt
        String systemPrompt = promptLoader.getSystemPrompt();
        
        // 2. 构建完整 Prompt
        String fullPrompt = systemPrompt + "\n\n用户输入: " + userInput;
        
        // 3. 调用大模型
        return chatModel.call(fullPrompt);
    }
}
```

### 示例2: 针对性 Skills

```java
// 闲聊场景 - 只加载 ChitchatSkill
String chitchatPrompt = promptLoader.buildTargetedPrompt(
    List.of("chitchat-skill")
);

// 数据查询场景 - 加载数据相关 Skills
String queryPrompt = promptLoader.buildTargetedPrompt(
    List.of("indicator-recognition-skill", 
            "dimension-normalization-skill", 
            "metric-query-skill")
);
```

### 示例3: 动态加载

```java
// 根据用户权限加载不同 Skills
List<String> allowedSkills = getUserAllowedSkills(userId);
String userPrompt = promptLoader.buildTargetedPrompt(allowedSkills);
```

## 扩展建议

### 1. 版本管理

```
docs/skills/
├── v1/
│   ├── metric-query-skill.md
│   └── ...
└── v2/  (新版本)
    ├── metric-query-skill.md
    └── ...
```

### 2. 多语言支持

```
docs/skills/
├── zh/
│   └── metric-query-skill.md
└── en/
    └── metric-query-skill.md
```

### 3. Skill 组合模板

```yaml
# skill-profiles.yaml
profiles:
  basic:
    skills: [chitchat, indicator-recognition]
  
  full:
    skills: [chitchat, indicator-recognition, dimension-normalization, metric-query]
  
  admin:
    skills: [all]
```

## 注意事项

1. **Token 消耗**: Skills 描述会增加 System Prompt 长度，注意控制总 Token
2. **热更新**: Markdown 修改后需要重启服务或实现热加载
3. **分级加载**: 复杂场景可以只加载必要的 Skills，减少干扰
4. **版本对齐**: 确保代码 Skills 和 Markdown 描述保持一致
