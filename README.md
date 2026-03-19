# Metric Analyst Agent

基于 Spring AI Alibaba Agent Framework 的指标分析多智能体系统。

## 技术栈

- **框架**: Spring Boot 3.2.5 + Spring AI Alibaba 1.1.2.1
- **Agent 框架**: Spring AI Alibaba Agent Framework (ReactAgent, SupervisorAgent)
- **数据库**: MySQL 8.0 (开发环境可使用 H2)
- **JDK**: Java 21

## 架构设计

### 多智能体架构

```
┌─────────────────────────────────────────────────────────────┐
│              MetricAnalystOrchestrator                      │
│                 (Supervisor Agent)                          │
│                     指标分析主管                              │
└────────────────────┬────────────────────────────────────────┘
                     │ 协调调度
        ┌────────────┼────────────┐
        ▼            ▼            ▼
┌──────────────┐ ┌──────────┐ ┌──────────────┐
│ metric_query │ │ insight  │ │   report     │
│   _expert    │ │ _analyst │ │  _generator  │
│  指标查询专家  │ │ 洞察分析  │ │  报告生成    │
└──────────────┘ └──────────┘ └──────────────┘
        │            │            │
        └────────────┼────────────┘
                     ▼
        ┌────────────────────────┐
        │   MetricQueryTools     │
        │    (@Tool 注解工具)     │
        │  - queryMetricCurrentValue
        │  - queryMetricComparison
        │  - queryMetricTrend
        │  - queryMetricRanking
        └────────────────────────┘
```

### 核心特性

1. **ReactAgent 模式**: 基于 ReAct (Reasoning + Acting) 的智能体架构
2. **工具调用**: 使用 `@Tool` 注解定义工具，支持自动 JSON Schema 生成
3. **Supervisor 模式**: 主管智能体动态路由请求到合适的专家智能体
4. **记忆管理**: 使用 `MemorySaver` 维护对话上下文
5. **多模型支持**: 支持 DashScope (通义千问) 和 OpenAI 兼容接口

## 快速开始

### 1. 数据库准备

#### MySQL (生产环境)

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE metric_analyst CHARACTER SET utf8mb4;"

# 执行初始化脚本
mysql -u root -p metric_analyst < src/main/resources/sql/init-mysql.sql
```

#### H2 (本地开发)

默认使用 H2 内存数据库，无需额外配置。

### 2. 配置环境变量

```bash
# DashScope API Key (通义千问)
export AI_DASHSCOPE_API_KEY=your-dashscope-api-key

# MySQL 配置 (生产环境)
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_USER=root
export MYSQL_PASSWORD=your-password
```

### 3. 运行应用

```bash
# 本地开发模式 (使用 H2 数据库)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 生产模式 (使用 MySQL)
./mvnw spring-boot:run
```

## API 接口

### 智能对话接口

```bash
# 主管 Agent 自动路由处理
GET /api/chat?input=北京招聘数量是多少

# 带会话ID的多轮对话
GET /api/chat?input=那上海呢&threadId=user_123
```

### 直接调用专家 Agent

```bash
# 指标查询专家
GET /api/query?input=北京招聘数量

# 洞察分析专家
GET /api/analyze?input=分析一下趋势

# 报告生成专家
GET /api/report?input=生成招聘市场分析报告
```

### 工具调用接口

```bash
# 查询单个指标
GET /api/tools/single?metric=招聘数量&region=北京

# 多地区对比
GET /api/tools/compare?metric=招聘数量&regions=北京,上海,杭州

# 查询趋势
GET /api/tools/trend?metric=招聘数量&region=北京&months=6

# 查询排名
GET /api/tools/ranking?metric=招聘数量&topN=5

# 提取维度信息
GET /api/tools/extract?input=北京招聘数量多少
```

### 系统接口

```bash
# 健康检查
GET /api/health

# 获取可用 Agent 列表
GET /api/agents
```

## 项目结构

```
metric-analyst-agent/
├── src/main/java/com/metric/analyst/agent/
│   ├── agents/
│   │   ├── AgentConfig.java           # Agent 配置
│   │   ├── MetricAnalystOrchestrator.java  # 主管编排器
│   │   └── MetricQueryTools.java      # @Tool 注解工具类
│   ├── controller/
│   │   └── AnalystController.java     # REST API
│   ├── dto/
│   │   ├── MetricComparisonDTO.java
│   │   ├── MetricRankingDTO.java
│   │   └── MetricTrendDTO.java
│   ├── entity/
│   │   └── IndicatorFact.java         # 指标事实实体
│   ├── repository/
│   │   └── IndicatorFactRepository.java
│   └── MetricAnalystAgentApplication.java
├── src/main/resources/
│   ├── sql/
│   │   └── init-mysql.sql             # MySQL 初始化脚本
│   ├── application.yml                # 主配置
│   └── application-local.yml          # 本地开发配置
└── pom.xml
```

## 扩展开发

### 添加新的工具

```java
@Component
public class NewTools {
    
    @Tool(description = "新工具描述")
    public String newTool(
            @ToolParam(description = "参数描述") String param) {
        // 实现逻辑
        return "结果";
    }
}
```

### 添加新的 Agent

```java
@Bean
public ReactAgent newAgent(ChatModel chatModel, MemorySaver memorySaver) {
    return ReactAgent.builder()
            .name("new_agent")
            .description("新 Agent 描述")
            .model(chatModel)
            .systemPrompt("系统提示词")
            .saver(memorySaver)
            .build();
}
```

## 参考文档

- [Spring AI Alibaba Agent Framework](https://java2ai.com/docs/frameworks/agent-framework)
- [ReactAgent 教程](https://java2ai.com/docs/frameworks/agent-framework/tutorials/agents)
- [Tools 教程](https://java2ai.com/docs/frameworks/agent-framework/tutorials/tools)
- [Multi-Agent 教程](https://java2ai.com/docs/frameworks/agent-framework/advanced/multi-agent)

## License

MIT License
