# Metric Analyst Agent

基于 Spring AI Alibaba 多智能体架构的指标分析系统

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    AnalystSupervisor                        │
│                     (主管智能体)                              │
│                  - 意图路由                                   │
│                  - 结果汇总                                   │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │IntentRecognition│  │ MetricQueryAgent  │  │MultiMetricAnalysis│
│  │   (意图识别)    │  │  (指标查询)        │  │  (多指标分析)     │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │ ChitchatAgent │  │ClarificationAgent│                    │
│  │   (闲聊)      │  │   (澄清)          │                    │
│  └──────────────┘  └──────────────┘                        │
├─────────────────────────────────────────────────────────────┤
│                      Tools (工具层)                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │metric_retrieval│  │dimension_parse  │  │  sql_execute  │
│  │  (指标检索)    │  │  (维度解析)      │  │  (SQL执行)    │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
├─────────────────────────────────────────────────────────────┤
│                      Skills (技能层)                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ metric-query │  │dimension-parse│  │data-retrieval│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│  ┌──────────────┐                                           │
│  │  generation  │                                           │
│  └──────────────┘                                           │
└─────────────────────────────────────────────────────────────┘
```

## 智能体说明

| 智能体 | 职责 |
|--------|------|
| AnalystSupervisor | 主管智能体，负责意图路由和结果汇总 |
| IntentRecognitionAgent | 意图识别，分析查询类型和提取关键词 |
| MetricQueryAgent | 单指标查询，解析维度并执行查询 |
| MultiMetricAnalysisAgent | 多指标对比分析，支持并行查询 |
| ChitchatAgent | 闲聊问答 |
| ClarificationAgent | 意图不明确时引导用户澄清 |

## Skills 技能

| 技能 | 说明 |
|------|------|
| metric-query | 指标查询技能，支持多维度筛选 |
| dimension-parse | 维度解析技能，支持地区、时间、学历等 |
| data-retrieval | 数据检索技能，支持 SQL 和 ES |
| generation | 生成技能，格式化输出结果 |

## Tools 工具

| 工具 | 功能 |
|------|------|
| metric_retrieval | 根据描述检索指标 |
| region_parse | 解析地区维度（返回6位国标编码）|
| time_parse | 解析时间维度 |
| dimension_parse | 解析枚举维度 |
| sql_execute | 执行 SQL 查询 |

## API 接口

### 同步查询
```bash
POST /api/analyst/query
Content-Type: application/json

{
  "query": "北京上个月招聘数量",
  "sessionId": "optional-session-id"
}
```

### 流式查询
```bash
POST /api/analyst/query/stream
Content-Type: application/json

{
  "query": "北京和上海招聘对比"
}
```

## 技术栈

- Spring Boot 3.2.5
- Spring AI Alibaba 1.1.2.0
- DashScope (通义千问)
- Java 21

## 启动

```bash
# 设置 API Key
export AI_DASHSCOPE_API_KEY=your-api-key

# 运行
./mvnw spring-boot:run
```

## 维度编码

### 地区（6位国标）
- 100000 = 全国
- 110000 = 北京市
- 310000 = 上海市
- LEVEL_2 = 不同省份（省级）
- LEVEL_3 = 不同市（地级）

### 时间
- YYYYMM 格式：202401
- recent_N：recent_3（最近3期）
- 关键词：last_month, this_year

## 许可证

Apache License 2.0
