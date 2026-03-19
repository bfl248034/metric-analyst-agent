# Metric Analyst Agent - 数据分析智能体

基于 Spring AI Alibaba 构建的企业级多智能体分析平台。

## 项目架构

```
┌─────────────────────────────────────────────────────────┐
│                    用户交互层                            │
│   AnalystController (REST API)                          │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│           数据分析智能体（MetricAnalystAgent）           │
│   基于 Spring AI Alibaba Skills + Hooks 构建            │
│                                                          │
│   Stage 0: 意图分类（闲聊识别）                          │
│   Stage 1: 指标识别（BM25+向量+大模型精排）               │
│   Stage 2: 维度标准化（时间/地区/默认值）                 │
│   Stage 3: 数据查询（预聚合数据）                        │
│   Stage 4: 结果加工 + 洞察生成                           │
└────────┬────────────────────────────────────────────────┘
         │
┌────────▼────────────────────────────────────────────────┐
│              共享基础设施层                               │
│   - DashScope (通义千问)                                  │
│   - MySQL (数据+元数据)                                   │
│   - Java内存 (向量+同义词索引)                            │
└─────────────────────────────────────────────────────────┘
```

## 核心功能

### 1. 五阶段处理流程

| 阶段 | 组件 | 功能 |
|------|------|------|
| 0 | IntentClassificationService | 闲聊识别，关键词匹配 |
| 1 | IndicatorRecognitionService | 两阶段指标识别 |
| 2 | DimensionNormalizationService | 维度标准化 |
| 3 | DataQueryService | 数据查询 |
| 4 | MetricAnalystAgent | 洞察生成 |

### 2. 指标识别两阶段

- **召回阶段**: BM25(MySQL) + 向量相似度(Java内存) + 同义词倒排(HashMap)
- **精排阶段**: 大模型语义理解

### 3. 维度处理三规则

| 维度 | 处理方式 |
|------|---------|
| 地区 | 模型识别名称 → 后端编码；关键词"各省份"→region_level |
| 时间 | 动态计算：latest/last:N/具体日期；超范围自动降级 |
| 其他 | 默认值填充（db_data_dimension.default_value） |

## 快速开始

### 1. 数据库初始化

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 2. 配置环境变量

```bash
export DASHSCOPE_API_KEY=your-api-key
export MYSQL_PASSWORD=your-password
```

### 3. 运行项目

```bash
mvn spring-boot:run
```

### 4. 测试接口

```bash
# 健康检查
curl http://localhost:8080/api/health

# 对话接口
curl "http://localhost:8080/api/chat?input=北京招聘薪资"

# 获取领域列表
curl http://localhost:8080/api/domains
```

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/chat | GET | 智能体对话 |
| /api/domains | GET | 获取可查询领域 |
| /api/health | GET | 健康检查 |

## 数据模型

### 核心表

- `db_indicator`: 指标元数据
- `dimension_values`: 维度值定义
- `db_data_dimension`: 维度关联配置
- `indicator_fact`: 指标事实数据（预聚合）

## 技术栈

- Spring Boot 3.2.5
- Spring AI Alibaba 1.1.2.1
- MySQL 8.0
- DashScope (通义千问)

## 项目结构

```
src/main/java/com/metric/analyst/agent/
├── agents/           # 智能体定义
├── controller/       # API控制器
├── dto/              # 数据传输对象
├── entity/           # 实体类
├── repository/       # 数据访问层
└── service/          # 业务逻辑层
    ├── IntentClassificationService.java    # 意图分类
    ├── IndicatorRecognitionService.java    # 指标识别
    ├── DimensionNormalizationService.java  # 维度标准化
    ├── DataQueryService.java               # 数据查询
    ├── IndicatorVectorStore.java           # 向量存储
    └── SynonymIndexService.java            # 同义词索引
```

## License

MIT
