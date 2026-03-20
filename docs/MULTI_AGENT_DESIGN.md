# Metric Analyst 多智能体协同方案

基于 Spring AI Alibaba Agent Framework 官方最佳实践设计

---

## 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                      SupervisorAgent                            │
│                    (主协调者 - 意图识别与分发)                     │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  闲聊识别    │  │  任务理解    │  │  路由决策    │          │
│  │  Skill       │  │  Skill       │  │  Skill       │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└──────────┬──────────────────────────────────────────────────────┘
           │
           ▼ 路由决策
    ┌──────┴──────┬──────────────┬──────────────┐
    │             │              │              │
    ▼             ▼              ▼              ▼
┌───────┐    ┌───────┐    ┌───────┐    ┌───────┐
│Query  │    │Insight│    │Report │    │Fallback│
│Agent  │    │Agent  │    │Agent  │    │Handler │
└───┬───┘    └───┬───┘    └───┬───┘    └───────┘
    │            │            │
    │  ┌─────────┴─────────┐  │
    │  │   MetricQueryTools │  │
    │  │  (共享工具集)       │  │
    │  └───────────────────┘  │
    │                         │
    ▼                         ▼
┌─────────────┐       ┌─────────────┐
│  DataQuery  │       │  ChartGen   │
│  Service    │       │  Tool       │
└─────────────┘       └─────────────┘
```

---

## 核心组件

### 1. Skills 目录结构

```
src/main/resources/skills/
├── supervisor/
│   ├── SKILL.md                 # 监督者技能：意图识别、路由决策
│   ├── references/
│   │   └── routing-rules.md     # 路由规则参考
│   └── examples/
│       └── routing-examples.md  # 路由示例
│
├── metric-query/
│   ├── SKILL.md                 # 指标查询技能
│   ├── references/
│   │   ├── indicator-catalog.md # 指标目录
│   │   └── dimension-guide.md   # 维度指南
│   └── examples/
│       └── query-patterns.md    # 查询模式示例
│
├── insight-analysis/
│   ├── SKILL.md                 # 洞察分析技能
│   └── examples/
│       └── analysis-templates.md
│
└── report-generation/
│   ├── SKILL.md                 # 报告生成技能
│   └── examples/
│       └── report-templates.md
```

### 2. Agent 定义

#### SupervisorAgent - 主协调者

```yaml
# supervisor/SKILL.md
---
name: supervisor
description: |
  我是 Metric Analyst 的主协调者，负责：
  1. 识别用户意图（闲聊/数据查询/洞察分析/报告生成）
  2. 将任务路由到合适的子 Agent
  3. 协调多步骤任务执行
---

# 能力范围

## 意图分类

| 意图类型 | 特征 | 路由目标 |
|---------|------|---------|
| chitchat | 打招呼、问身份、无关话题 | 直接回复 |
| query | 询问具体数值、趋势、排名 | metric-query-agent |
| analysis | 深度分析、对比、异常检测 | insight-analysis-agent |
| report | 生成报告、导出数据 | report-generation-agent |

## 路由规则

1. **单指标查询** → metric-query-agent
2. **多指标对比** → insight-analysis-agent  
3. **生成报告** → report-generation-agent
4. **未知意图** → 澄清询问
```

#### MetricQueryAgent - 指标查询专家

```yaml
# metric-query/SKILL.md
---
name: metric-query
description: 专门处理指标数据查询，支持当前值、趋势、排名、对比
---

# 可用工具

## queryCurrentValue
查询单个指标在指定地区的当前值

## queryTrend
查询指标的历史趋势（近N个月）

## queryRanking
查询指标的地区排名

## queryComparison
对比多个地区的同一指标

## normalizeDimensions
标准化维度值（地区/时间/其他）
```

### 3. 代码实现

#### SupervisorAgent 配置

```java
@Configuration
public class MultiAgentConfig {

    @Bean
    public SkillRegistry skillRegistry() {
        return ClasspathSkillRegistry.builder()
            .classpathPath("skills")
            .build();
    }

    @Bean
    public SkillsAgentHook skillsAgentHook(SkillRegistry registry) {
        return SkillsAgentHook.builder()
            .skillRegistry(registry)
            .build();
    }

    // ===== 子 Agent =====

    @Bean
    public ReactAgent metricQueryAgent(ChatModel chatModel,
                                        MetricQueryTools queryTools) {
        return ReactAgent.builder()
            .name("metric-query-agent")
            .model(chatModel)
            .description("专门处理指标数据查询")
            .instruction("""
                你是指标查询专家。使用可用工具查询数据：
                1. 先使用 normalizeDimensions 标准化维度
                2. 根据查询类型选择合适的查询工具
                3. 返回结构化的数据结果
                """)
            .tools(queryTools)
            .saver(new MemorySaver())
            .build();
    }

    @Bean
    public ReactAgent insightAnalysisAgent(ChatModel chatModel) {
        return ReactAgent.builder()
            .name("insight-analysis-agent")
            .model(chatModel)
            .description("专门进行数据洞察分析")
            .instruction("基于查询结果进行深度分析，提供见解...")
            .saver(new MemorySaver())
            .build();
    }

    @Bean
    public ReactAgent reportGenerationAgent(ChatModel chatModel) {
        return ReactAgent.builder()
            .name("report-generation-agent")
            .model(chatModel)
            .description("生成数据分析报告")
            .instruction("整合数据和分析结果，生成专业报告...")
            .saver(new MemorySaver())
            .build();
    }

    // ===== 主 SupervisorAgent =====

    @Bean
    public SupervisorAgent supervisorAgent(ChatModel chatModel,
                                           SkillsAgentHook skillsHook,
                                           ReactAgent metricQueryAgent,
                                           ReactAgent insightAnalysisAgent,
                                           ReactAgent reportGenerationAgent) {
        
        // 构建子 Agent 映射
        Map<String, ReactAgent> subAgents = Map.of(
            "metric-query", metricQueryAgent,
            "insight-analysis", insightAnalysisAgent,
            "report-generation", reportGenerationAgent
        );

        return SupervisorAgent.builder()
            .name("metric-analyst-supervisor")
            .description("Metric Analyst 主协调者")
            .model(chatModel)
            .systemPrompt("""
                你是 Metric Analyst 的主协调者。你的职责：
                
                ## 意图识别
                分析用户输入，识别意图类型：
                - chitchat: 打招呼、询问身份、无关话题
                - query: 具体数据查询（薪资、数量、趋势等）
                - analysis: 深度分析需求（对比、异常、预测）
                - report: 报告生成需求
                
                ## 路由决策
                根据意图选择子 Agent：
                - query → metric-query-agent
                - analysis → insight-analysis-agent
                - report → report-generation-agent
                
                ## 响应格式
                只返回 Agent 名称或 FINISH，不要解释。
                """)
            .subAgents(subAgents.values().stream().toList())
            .hooks(List.of(skillsHook))
            .build();
    }
}
```

#### 共享 Tools

```java
@Component
public class MetricQueryTools {

    private final DataQueryService dataQueryService;
    private final DimensionNormalizationService dimensionService;

    @Tool(description = "标准化维度值，将用户输入的地区、时间等转换为系统编码")
    public NormalizedDimensions normalizeDimensions(
        @ToolParam(description = "指标编码") String indicatorId,
        @ToolParam(description = "维度映射") Map<String, Object> dimensions
    ) {
        return dimensionService.normalize(indicatorId, dimensions);
    }

    @Tool(description = "查询指标当前值")
    public QueryResult queryCurrentValue(
        @ToolParam(description = "指标编码") String indicatorId,
        @ToolParam(description = "标准化后的维度") NormalizedDimensions dimensions
    ) {
        return dataQueryService.queryCurrent(indicatorId, dimensions);
    }

    @Tool(description = "查询指标趋势")
    public TrendResult queryTrend(
        @ToolParam(description = "指标编码") String indicatorId,
        @ToolParam(description = "地区编码") String regionCode,
        @ToolParam(description = "查询月数") int months
    ) {
        return dataQueryService.queryTrend(indicatorId, regionCode, months);
    }

    @Tool(description = "查询指标排名")
    public RankingResult queryRanking(
        @ToolParam(description = "指标编码") String indicatorId,
        @ToolParam(description = "地区级别：省级/市级") String regionLevel,
        @ToolParam(description = "返回数量") int topN
    ) {
        return dataQueryService.queryRanking(indicatorId, regionLevel, topN);
    }
}
```

#### Hooks - 闲聊识别

```java
@Component
public class ChitchatHook extends MessagesModelHook {

    private static final Set<String> GREETINGS = Set.of(
        "你好", "您好", "嗨", "哈喽", "hello", "hi"
    );

    @Override
    public String getName() {
        return "chitchat_detector";
    }

    @Override
    public AgentCommand beforeModel(List<Message> messages, RunnableConfig config) {
        // 提取用户输入
        String userInput = extractLastUserInput(messages);
        
        // 闲聊检测
        if (isChitchat(userInput)) {
            String reply = generateChitchatReply(userInput);
            
            // 直接返回回复，跳过模型调用
            List<Message> response = List.of(
                new AssistantMessage(reply)
            );
            
            return new AgentCommand(response, UpdatePolicy.REPLACE);
        }
        
        return new AgentCommand(messages);
    }

    private boolean isChitchat(String input) {
        String normalized = input.trim().toLowerCase();
        return GREETINGS.contains(normalized) || 
               normalized.matches(".*(你是谁|你能做什么|讲个笑话).*");
    }

    private String generateChitchatReply(String input) {
        return """
            你好！我是 Metric Analyst（数据分析智能体）。
            
            我可以帮你：
            📊 查询各类经济指标（招聘、企业、专利、采购）
            📈 分析趋势变化（同比、环比、排名）
            🏆 多地区对比（各省份、各市排名）
            
            试试这样问：
            • "北京招聘薪资怎么样？"
            • "各省份企业新增排名"
            • "近6个月专利趋势"
            """;
    }
}
```

#### Interceptors - 性能监控

```java
@Component
public class PerformanceInterceptor extends ToolInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PerformanceInterceptor.class);

    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
        String toolName = request.getToolName();
        long startTime = System.currentTimeMillis();

        try {
            ToolCallResponse response = handler.call(request);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Tool {} executed in {}ms", toolName, duration);
            
            return response;
        } catch (Exception e) {
            log.error("Tool {} failed: {}", toolName, e.getMessage());
            return ToolCallResponse.error(request.getToolCall(), 
                "工具执行失败: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "performance_monitor";
    }
}
```

---

## 工作流示例

### 场景1：单指标查询

```
用户: "北京招聘薪资"
    ↓
SupervisorAgent
    ↓ 识别为 query 意图
MetricQueryAgent
    ↓
1. normalizeDimensions(region="北京") 
   → {region: "110000"}
2. queryCurrentValue(indicator="薪资", dimensions)
   → {value: 20000, unit: "元"}
    ↓
回复: "北京最新招聘薪资为 20,000 元"
```

### 场景2：多步骤分析任务

```
用户: "对比北京上海杭州的招聘薪资，并分析趋势"
    ↓
SupervisorAgent
    ↓ 识别为 analysis 意图（多步骤）
Step 1: MetricQueryAgent
    - queryComparison(regions=["北京","上海","杭州"])
    → 对比数据
    
Step 2: InsightAnalysisAgent  
    - analyzeTrend(data)
    - generateInsight(comparison, trend)
    → 深度分析
    
Step 3: SupervisorAgent
    - FINISH
    ↓
回复: 完整分析报告
```

### 场景3：报告生成

```
用户: "生成Q1招聘市场分析报告"
    ↓
SupervisorAgent
    ↓ 识别为 report 意图
Step 1: MetricQueryAgent (并行)
    - queryTrend(岗位数量, 全国, 3个月)
    - queryTrend(平均薪资, 全国, 3个月)
    - queryRanking(岗位数量, 省级, 10)
    
Step 2: ReportGenerationAgent
    - generateReport(allData)
    - exportToPDF()
    ↓
回复: 报告下载链接
```

---

## 关键设计决策

### 1. 为什么选择 SupervisorAgent 模式？

| 特性 | 优势 |
|------|------|
| **多步骤编排** | 复杂任务可分解为多个子任务 |
| **智能路由** | LLM 动态决策，比硬编码路由更灵活 |
| **上下文保持** | 子 Agent 执行后返回监督者，保持对话连续性 |
| **易于扩展** | 新增 Agent 只需注册到 Supervisor |

### 2. Skills 渐进式披露

```
System Prompt 初始只注入技能列表（name + description）
    ↓
模型判断需要某技能
    ↓
调用 read_skill(skill_name) 按需加载完整 SKILL.md
    ↓
使用该技能的工具和知识
```

**好处**：减少初始 Token 消耗，只加载需要的知识

### 3. 共享 Tools vs 专属 Tools

| Tool 类型 | 归属 | 示例 |
|----------|------|------|
| **共享 Tools** | MetricQueryTools（所有子 Agent 可用）| queryCurrentValue, queryTrend |
| **专属 Tools** | 特定 Agent 私有 | generateChart（仅 insight-agent）|

---

## 部署配置

```yaml
# application.yml
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
    
    agent:
      supervisor:
        model: qwen-max
        max-iterations: 10
      
      skills:
        directory: classpath:skills
        auto-reload: true
      
      hooks:
        enabled: 
          - chitchat_detector
          - message_trimmer
      
      interceptors:
        enabled:
          - performance_monitor
          - tool_retry
```

---

## 扩展方向

### Phase 2：RAG 增强

```java
// 添加文档检索工具
@Tool(description = "检索指标定义和业务文档")
public List<Document> retrieveDocuments(@ToolParam String query) {
    return vectorStore.similaritySearch(
        SearchRequest.builder()
            .query(query)
            .topK(3)
            .build()
    );
}
```

### Phase 3：Human-in-the-Loop

```java
@Bean
public HumanInTheLoopHook humanReviewHook() {
    return HumanInTheLoopHook.builder()
        .approvalOn("generateReport", ToolConfig.builder()
            .description("请确认生成报告")
            .build())
        .build();
}
```

### Phase 4：多模态支持

```java
@Tool(description = "生成数据可视化图表")
public ChartImage generateChart(QueryResult data) {
    // 调用图表生成服务
}
```
