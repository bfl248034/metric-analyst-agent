# Metric Analyst Agent - 指标分析智能体

基于 Spring AI Alibaba 构建的企业级数据分析智能体平台。

## 项目架构

```
┌─────────────────────────────────────────────────────────┐
│                    用户交互层                            │
│   AnalystController (REST API)                          │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│              数据分析智能体（五阶段流程）                 │
│                                                          │
│   Stage 0: 意图分类（闲聊识别）                          │
│   Stage 1: 指标识别（BM25+向量+同义词+大模型精排）        │
│   Stage 2: 维度标准化（时间/地区/其他维度）               │
│   Stage 3: 动态数据查询（多数据源架构）                   │
│   Stage 4: 结果加工 + 洞察生成                           │
└────────┬────────────────────────────────────────────────┘
         │
┌────────▼────────────────────────────────────────────────┐
│              数据基础设施层                               │
│   - db_data_source: 数据源配置（MySQL/Kylin/API）         │
│   - db_data_table: 数据表元数据                          │
│   - db_data_dimension: 维度字段映射                      │
│   - db_indicator: 指标元数据                             │
│   - dimension_values: 维度值定义                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              共享基础设施层                               │
│   - DashScope (通义千问)                                  │
│   - Java内存索引 (向量+同义词)                            │
└─────────────────────────────────────────────────────────┘
```

## 核心功能

### 1. 五阶段处理流程

| 阶段 | 组件 | 功能 |
|------|------|------|
| 0 | IntentClassificationService | 闲聊识别，关键词匹配 |
| 1 | IndicatorRecognitionService | 两阶段指标识别（召回+精排） |
| 2 | DimensionNormalizationService | 维度标准化（地区/时间/默认值） |
| 3 | DataQueryService | 动态数据查询 |
| 4 | MetricAnalystAgent | 洞察生成 |

### 2. 指标识别两阶段

- **召回阶段**: BM25(MySQL) + 向量相似度(Java内存) + 同义词倒排(HashMap)
- **精排阶段**: 大模型语义理解，提取维度信息

### 3. 维度处理三规则

| 维度 | 处理方式 |
|------|---------|
| 地区 | 模型识别名称 → 编码；关键词"各省份"→region_level |
| 时间 | latest(最新)/last:N(近N期)/具体日期 |
| 其他 | 默认值填充（db_data_dimension.default_value） |

### 4. 动态数据源架构

```
┌─────────────────────────────────────────┐
│           DynamicDataSourceManager      │
│  管理多数据源连接池（MySQL/Kylin）       │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│           DynamicQueryBuilder           │
│  根据 db_data_dimension 动态构建 SQL    │
│  使用 dimension_code 映射实际字段        │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│           DataQueryService              │
│  执行参数化查询，返回结构化结果          │
└─────────────────────────────────────────┘
```

## 快速开始

### 1. 数据库初始化

```bash
mysql -u root -p metric_db < src/main/resources/sql/init_mysql.sql
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

### 核心表（按依赖顺序）

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| **db_data_source** | 数据源配置 | source_id, source_type, host, port, database_name, username, password |
| **db_data_table** | 数据表元数据 | table_id, table_name, source_id, time_column, region_column, value_column |
| **db_data_dimension** | 维度字段映射 | table_id, dimension_id, dimension_code |
| **db_indicator** | 指标元数据 | indicator_id, indicator_name, unit, table_id, domain, tags |
| **dimension_values** | 维度值定义 | dimension_id, value_code, value_name, synonyms |

### 表关系

```
db_indicator.table_id ──→ db_data_table.table_id
                              │
                              ├──→ db_data_source.source_id
                              │
                              └──→ db_data_dimension.table_id
```

## 可用指标

| 指标ID | 指标名称 | 领域 |
|--------|----------|------|
| I_RPA_ICN_RAE_POSITION_NUM | 招聘岗位数量 | 招聘就业 |
| I_RPA_ICN_RAE_COMPANY_NUM | 招聘市场主体数量 | 招聘就业 |
| I_RPA_ICN_RAE_SALARY_AMOUNT | 招聘岗位平均薪酬 | 招聘就业 |
| I_RPA_ICN_MKE_COMPANY_ADD_NUM | 新增企业数量 | 市场主体 |
| I_RPA_ICN_MKE_COMPANY_CANCEL_NUM | 注销企业数量 | 市场主体 |
| I_RPA_ICN_MKE_COMPANY_ON_NUM | 在营企业数量 | 市场主体 |
| I_RPA_ICN_PAT_APPLICATION_NUM | 专利申请数量 | 知识产权 |
| I_RPA_ICN_GVP_AMOUNT | 政府采购金额 | 政府采购 |
| I_RPA_ICN_GVP_NUM | 政府采购数量 | 政府采购 |
| I_RPA_ICN_GVP_AMOUNT_AVG | 政府采购平均价格 | 政府采购 |

## 支持维度

| 维度ID | 说明 | 示例值 |
|--------|------|--------|
| region | 地区 | 110000(北京), 310000(上海), 100000(全国), 省级/市级/全国 |
| time | 时间 | latest, last:6, 2024-03 |
| education | 学历 | TOTAL, RAE_EDU_6(本科), RAE_EDU_7(硕士) |
| economic_type | 经济类型 | TOTAL, MKE_ECO_3(私营) |
| spe_tag | 特殊资质 | TOTAL, MKE_SPE_4(高新企业) |
| patent_type | 专利类型 | TOTAL, PAT_PTT_2(发明专利) |
| company_type | 申请人类型 | TOTAL, RPA_CTP_1(企业) |
| price_range | 价格区间 | TOTAL, RPA_PRI_2(20-100万) |
| scene_type | 场景类型 | TOTAL, RPA_SCE_1(环境保护) |
| data_attr | 数据属性 | DAT_1(当期), DAT_2(累计) |
| icn_chain_area | 产业链领域 | ICN_CHAIN_6 |
| icn_chain_link | 产业链环节 | ICN_CHAIN_6 |

## 技术栈

- Spring Boot 3.2.5
- Spring AI Alibaba 1.1.2.1
- Spring Data JPA
- MySQL 8.0
- HikariCP 连接池
- DashScope (通义千问)
- text-embedding-v2 (向量模型)

## 项目结构

```
src/main/java/com/metric/analyst/agent/
├── agents/                    # 智能体定义
│   ├── MetricAnalystAgent.java         # 主分析智能体（五阶段流程）
│   └── MetricQueryTools.java           # 指标查询工具集
├── controller/                # API控制器
│   └── AnalystController.java
├── dto/                       # 数据传输对象（已清理）
├── entity/                    # 实体类
│   ├── DataSource.java                 # 数据源配置
│   ├── DataTable.java                  # 数据表元数据
│   ├── DataDimension.java              # 维度字段映射
│   ├── Indicator.java                  # 指标元数据
│   └── DimensionValue.java             # 维度值定义
├── repository/                # 数据访问层
│   ├── DataSourceRepository.java
│   ├── DataTableRepository.java
│   ├── DataDimensionRepository.java
│   ├── IndicatorRepository.java
│   └── DimensionValueRepository.java
└── service/                   # 业务逻辑层
    ├── IntentClassificationService.java    # 意图分类
    ├── IndicatorRecognitionService.java    # 指标识别（两阶段）
    ├── DimensionNormalizationService.java  # 维度标准化
    ├── DataQueryService.java               # 数据查询
    ├── IndicatorVectorStore.java           # 向量存储
    ├── SynonymIndexService.java            # 同义词索引
    ├── IndicatorLocator.java               # 指标定位器接口
    ├── SimpleIndicatorLocator.java         # 简化实现（避免循环依赖）
    └── datasource/
        ├── DynamicDataSourceManager.java   # 动态数据源管理
        └── DynamicQueryBuilder.java        # 动态SQL构建
```

## 查询示例

```
北京招聘薪资
→ 查询 I_RPA_ICN_RAE_SALARY_AMOUNT 在 region=北京, time=latest

北京近6个月本科招聘薪资
→ 查询 I_RPA_ICN_RAE_SALARY_AMOUNT 在 region=北京, time=last:6, education=本科

各省份招聘薪资排名
→ 查询 I_RPA_ICN_RAE_SALARY_AMOUNT 按省级分组排名

对比北京上海杭州企业新增数量
→ 分别查询三个地区的 I_RPA_ICN_MKE_COMPANY_ADD_NUM
```

## License

MIT
