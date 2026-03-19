# 代码逻辑排查清单

## 实施方案核心要点 vs 代码实现

---

## 1. 五阶段处理流程 ✅

| 阶段 | 方案要求 | 代码实现 | 状态 |
|------|---------|---------|------|
| Stage 0 | 闲聊识别（关键词匹配+大模型兜底） | IntentClassificationService | ✅ |
| Stage 1 | 指标识别（两阶段检索+精排） | IndicatorRecognitionService | ⚠️ 需检查 |
| Stage 2 | 维度标准化（地区/时间/默认值） | DimensionNormalizationService | ⚠️ 需检查 |
| Stage 3 | 数据查询（预聚合数据） | DataQueryService | ⚠️ 需检查 |
| Stage 4 | 结果加工+洞察生成 | MetricAnalystAgent | ⚠️ 需检查 |

---

## 2. 两阶段指标识别

### 2.1 召回阶段（三路并行）

| 召回方式 | 方案要求 | 代码实现 | 状态 |
|---------|---------|---------|------|
| BM25 | MySQL FULLTEXT | indicatorRepository.searchByFulltext | ⚠️ 检查SQL |
| 向量相似度 | Java内存计算cosine | IndicatorVectorStore.searchByVector | ❌ embed()未实现 |
| 同义词倒排 | Java HashMap O(1) | SynonymIndexService | ✅ |

**问题1**: `IndicatorVectorStore.embed()` 方法未完成，需要接入DashScope Embedding API

### 2.2 精排阶段

| 要求 | 实现 | 状态 |
|-----|------|------|
| 大模型确认指标 | llmRank()方法 | ✅ |
| 提取维度信息 | extracted_dimensions | ✅ |
| 置信度阈值0.6 | CONFIDENCE_THRESHOLD | ✅ |

---

## 3. 维度处理三规则

### 3.1 地区维度

| 场景 | 方案要求 | 代码实现 | 状态 |
|-----|---------|---------|------|
| 具体地区 | 北京→110000 | normalizeRegion() | ✅ |
| 级别查询 | 各省份→region_level='省级' | REGION_LEVEL_KEYWORDS | ✅ |
| 同义词匹配 | 帝都→北京 | synonyms匹配 | ✅ |

### 3.2 时间维度

| 场景 | 方案要求 | 代码实现 | 状态 |
|-----|---------|---------|------|
| 未指定 | 取latest | "latest"→查询MAX | ✅ |
| 近N期 | 展开N个时间点 | "last:N"→expandTimePeriods() | ✅ |
| 超范围 | 降级为最新+提示 | ⚠️ 未实现降级提示 | ❌ |

**问题2**: 时间超范围时，需要给用户提示"数据未发布，展示最新"

### 3.3 其他维度

| 要求 | 实现 | 状态 |
|-----|------|------|
| 默认值填充 | db_data_dimension.default_value | ✅ |
| 必填校验 | is_required字段 | ⚠️ 未使用 |

**问题3**: 未使用is_required进行必填维度校验

---

## 4. 分组查询差异化

| 维度类型 | 方案要求 | 代码实现 | 状态 |
|---------|---------|---------|------|
| 地区分组 | region_level='省级' | DataQueryService.buildSql | ✅ |
| 其他分组 | xxx_id != 'TOTAL' | isMultiValueQuery() | ✅ |

---

## 5. 预聚合数据模型

| 要求 | 实现 | 状态 |
|-----|------|------|
| 时间取最后一天 | YYYY-MM-DD格式 | ✅ |
| 无GROUP BY查询 | 直接精确匹配 | ✅ |
| region_level字段 | 用于地区级别查询 | ✅ |

---

## 6. 异常情况处理

| 异常场景 | 方案要求 | 代码实现 | 状态 |
|---------|---------|---------|------|
| 闲聊 | 关键词匹配+模板回复 | IntentClassificationService | ✅ |
| 指标无匹配-领域推荐 | 分析意图领域推荐相似指标 | ⚠️ 未实现 | ❌ |
| 指标无匹配-澄清询问 | 列出Top3让用户选择 | ⚠️ 未实现 | ❌ |
| 指标无匹配-全局引导 | 展示所有可查询领域 | MetricAnalystAgent.handleNoMatch | ✅ |
| 时间超范围 | 降级为最新+提示 | ⚠️ 未实现提示 | ❌ |
| 维度不匹配 | 提示不支持+建议可用 | ⚠️ 未实现 | ❌ |

**问题4**: 指标无匹配的三层降级处理不完整

---

## 7. 关键问题汇总

### 🔴 严重问题

1. **向量Embedding未实现**
   - 文件: IndicatorVectorStore.java
   - 问题: embed()方法返回空数组，未调用DashScope API
   - 修复: 接入DashScope Embedding API

### 🟡 中等问题

2. **时间超范围降级提示缺失**
   - 文件: DimensionNormalizationService.java
   - 问题: 未检测时间是否超范围并提示用户
   - 修复: 添加时间范围校验和降级提示

3. **必填维度校验缺失**
   - 文件: DimensionNormalizationService.java
   - 问题: 未使用is_required字段校验必填维度
   - 修复: 添加必填维度校验逻辑

4. **指标无匹配三层降级不完整**
   - 文件: MetricAnalystAgent.java
   - 问题: 只实现了全局引导，未实现领域推荐和澄清询问
   - 修复: 完善三层降级处理

### 🟢 轻微问题

5. **SQL注入风险**
   - 文件: DataQueryService.java
   - 问题: SQL拼接未使用参数化查询
   - 修复: 使用PreparedStatement或参数化查询

6. **线程安全问题**
   - 文件: IndicatorVectorStore.java
   - 问题: embeddings使用ConcurrentHashMap但加载时可能有问题
   - 修复: 确保线程安全

---

## 8. 需要补充的功能

1. **向量Embedding API接入**
2. **领域推荐算法**（根据用户输入分析意图领域）
3. **时间范围校验**
4. **必填维度校验**
5. **SQL参数化查询**
6. **单元测试覆盖**
