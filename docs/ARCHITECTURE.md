# Metric Analyst Agent 技术方案

> 企业级数据分析智能体平台 - 详细设计文档

---

## 一、项目定位与架构愿景

### 1.1 项目定位

**Metric Analyst Agent** 是一个企业级多智能体分析平台，当前阶段实现数据分析智能体（Metric Analyst）。

**核心目标**：让政府/企业决策者通过自然语言对话（如"各省份近6个月本科招聘薪资排名"），系统自动完成意图识别→指标匹配→维度标准化→数据查询→洞察生成，无需懂SQL、无需记忆指标编码。

**数据范围**：
- 招聘就业、市场主体、知识产权、政府采购四大领域
- 10个核心指标，预留扩展接口

### 1.2 分层架构

```
┌─────────────────────────────────────────────────────────┐
│                    用户交互层                            │
│            Web对话页 / REST API / 未来IM集成            │
└──────────────────┬──────────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────────┐
│                 Agent Router（预留）                     │
│            当前：直接路由到数据分析智能体                 │
│            未来：支持文献问答、通用助手等智能体           │
└────────┬────────────────────────────────────────────────┘
         │
┌────────▼────────────────────────────────────────────────┐
│           数据分析智能体（Metric Analyst）               │
│          基于 Spring AI Alibaba Skills + Hooks 构建     │
│                                                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │ AgentHook: Pre-process（阶段0：闲聊识别）       │    │
│  │ - 关键词匹配 / 大模型分类 / 模板回复            │    │
│  └─────────────────────────────────────────────────┘    │
│                           ↓                              │
│  ┌─────────────────────────────────────────────────┐    │
│  │ 阶段1：指标识别（两阶段检索+精排）              │    │
│  │ - BM25 + 向量相似度 + 同义词倒排（召回）        │    │
│  │ - 大模型精排（确认+维度提取）                   │    │
│  │ - 指标无匹配处理（三层降级）                    │    │
│  └─────────────────────────────────────────────────┘    │
│                           ↓                              │
│  ┌─────────────────────────────────────────────────┐    │
│  │ ToolHook: Pre-tool-call（阶段2：维度标准化）    │    │
│  │ - 地区：模型识别名称 → 后端编码 + region_level  │    │
│  │ - 时间：动态计算（最新/近N期/超范围降级）       │    │
│  │ - 其他：默认值填充                              │    │
│  └─────────────────────────────────────────────────┘    │
│                           ↓                              │
│  ┌─────────────────────────────────────────────────┐    │
│  │ Skill: MetricQuery（阶段3：数据查询）           │    │
│  │ - queryCurrentValue / queryTrend / queryRanking │    │
│  │ - 预聚合数据直接查询，无GROUP BY                │    │
│  └─────────────────────────────────────────────────┘    │
│                           ↓                              │
│  ┌─────────────────────────────────────────────────┐    │
│  │ ToolHook: Post-tool-call（阶段4：结果加工）     │    │
│  │ - 数据整理 / 排名计算 / 趋势分析                │    │
│  └─────────────────────────────────────────────────┘    │
│                           ↓                              │
│  ┌─────────────────────────────────────────────────┐    │
│  │ AgentHook: Post-process（阶段4：洞察生成）      │    │
│  │ - 大模型解读 / 回复生成 / 上下文更新            │    │
│  └─────────────────────────────────────────────────┘    │
└────────┬────────────────────────────────────────────────┘
         │
┌────────▼────────────────────────────────────────────────┐
│                 共享基础设施层                           │
│   - 大模型调用（DashScope通义千问）                      │
│   - MySQL（事实数据+元数据+全文索引）                    │
│   - Java内存（向量+同义词索引，启动加载）                │
└─────────────────────────────────────────────────────────┘
```

---

## 二、数据模型详解

### 2.1 事实层（ADS预聚合宽表）

**核心设计**：所有维度组合已预计算，直接精确匹配查询，无GROUP BY。

**时间存储规则**：取频率周期最后一天
- 月度：`2024-02-29`、`2024-03-31`（每月最后一天）
- 季度：`2024-03-31`、`2024-06-30`（每季度末）
- 年度：`2024-12-31`（年末）

**地区级别字段（关键设计）**：
```sql
region_level VARCHAR(20) -- 全国/省级/市级/区县级
```

**10张事实表**：

| 表名 | 说明 |
|------|------|
| `ads_rpa_w_icn_edu_recruit_position_num_m` | 招聘岗位数量 |
| `ads_rpa_w_icn_recruit_salary_amount_m` | 招聘岗位平均薪酬 |
| `ads_rpa_w_icn_eco_spe_company_add_num_m` | 新增企业数量 |
| `ads_rpa_w_icn_eco_spe_company_cancel_num_m` | 注销企业数量 |
| `ads_rpa_w_icn_eco_spe_company_on_num_m` | 在营企业数量 |
| `ads_rpa_w_icn_recruit_company_num_m` | 招聘市场主体数量 |
| `ads_rpa_w_icn_typ_com_patent_application_num_m` | 专利申请数量 |
| `ads_rpa_w_icn_sce_pri_government_procurement_amount_m` | 政府采购金额 |
| `ads_rpa_w_icn_sce_pri_government_procurement_num_m` | 政府采购数量 |
| `ads_rpa_w_icn_sce_pri_government_procurement_avg_m` | 政府采购平均价格 |

**数据示例**：

```
time_id    | region_id | region_level | education_id | economic_type_id | fact_value | value_mom | value_yoy
-----------|-----------|--------------|--------------|------------------|------------|-----------|----------
2024-02-29 | 100000    | 全国         | TOTAL        | TOTAL            | 15000      | 4.5       | 6.8
2024-02-29 | 110000    | 省级         | RAE_EDU_6    | TOTAL            | 20000      | 5.5       | 9.0
2024-02-29 | 440100    | 市级         | TOTAL        | TOTAL            | 18000      | 4.8       | 7.2
```

### 2.2 元数据层

#### db_indicator（指标元数据）

| 字段 | 说明 | 示例 |
|------|------|------|
| `indicator_id` | 指标编码 | `I_RPA_ICN_RAE_SALARY_AMOUNT` |
| `indicator_name` | 指标名称 | 招聘岗位平均薪酬 |
| `unit` | 单位 | 元 |
| `frequency` | 频率 | M（月）/ Q（季）/ Y（年） |
| `valid_measures` | 有效度量 | 当期，当期同比，累计，累计同比 |
| `table_id` | 对应事实表 | `ads_rpa_w_icn_recruit_salary_amount_m` |
| `domain` | 大领域 | 招聘就业/市场主体/知识产权/政府采购 |
| `subdomain` | 子领域 | 薪资水平/招聘需求/企业增量 |
| `tags` | 同义词 | 薪资,工资,薪酬,收入,待遇 |
| `remark` | 业务描述 | 招聘岗位的平均薪资水平... |
| `embedding_json` | 预计算向量 | `[0.23,-0.56,0.89,...]` |
| `indexed` | 是否已建立向量索引 | true/false |

#### db_data_dimension（维度定义）

| 字段 | 说明 | 示例 |
|------|------|------|
| `table_id` | 关联事实表 | `ads_rpa_w_icn_recruit_salary_amount_m` |
| `dimension_id` | 维度标识 | `region`/`education`/`time`/`economic_type` |
| `dimension_name` | 维度名称 | 地区/学历/时间/经济类型 |
| `is_common` | 是否公共维度 | 1是，0否 |
| `is_required` | 是否必填 | 1是，0否 |
| `default_value` | 默认值 | `TOTAL`/`100000`/`DAT_1`/`ICN_CHAIN_6` |
| `dimension_type` | 类型 | temporal（时间）/ categorical（分类） |
| `sort_order` | 排序 | 1,2,3... |

**维度默认值配置**：

| 维度 | 默认值 | 含义 | 适用指标 |
|------|--------|------|----------|
| region | 100000 | 全国 | 所有指标 |
| education | TOTAL | 全部学历 | 招聘类指标 |
| economic_type | TOTAL | 全部经济类型 | 企业类指标 |
| spe_tag | TOTAL | 全部资质 | 企业类指标 |
| patent_type | TOTAL | 全部专利类型 | 专利类指标 |
| company_type | TOTAL | 全部申请人类型 | 专利类指标 |
| price_range | TOTAL | 全部价格 | 采购类指标 |
| scene_type | TOTAL | 全部场景 | 采购类指标 |
| data_attr | DAT_1 | 当期值 | 所有指标 |
| icn_chain_area | ICN_CHAIN_6 | 产业链领域 | 所有指标 |
| icn_chain_link | ICN_CHAIN_6 | 产业链环节 | 所有指标 |

#### dimension_values（维度值）

| 字段 | 说明 | 示例 |
|------|------|------|
| `dimension_id` | 维度标识 | `region` |
| `value_code` | 值编码 | `110000` |
| `value_name` | 值名称 | 北京市 |
| `synonyms` | 同义词 | 北京,帝都,京城 |
| `parent_code` | 父级编码 | `100000`（全国） |
| `sort_order` | 排序 | 1,2,3... |
| `indexed` | 是否已索引 | true/false |

**地区维度示例**：

| dimension_id | value_code | value_name | synonyms | parent_code |
|--------------|------------|------------|----------|-------------|
| region | 100000 | 全国 | 中国,全国,全中国 | NULL |
| region | 110000 | 北京市 | 北京,帝都,京城 | 100000 |
| region | 310000 | 上海市 | 上海,魔都,沪 | 100000 |
| region | 440000 | 广东省 | 广东,粤,岭南 | 100000 |
| region | 440100 | 广州市 | 广州,羊城,穗 | 440000 |
| region | 440300 | 深圳市 | 深圳,鹏城,深 | 440000 |

---

## 三、查询处理完整流程（五阶段+Hooks）

### 阶段0：闲聊识别（AgentHook: Pre-process）

**目的**：快速识别闲聊意图，友好回复并引导回数据主题。

**两层识别**：

#### ① 关键词快速匹配（无需大模型，O(1)）

```java
// 打招呼
Set<String> GREETINGS = Set.of(
    "你好", "您好", "嗨", "哈喽", "hello", "hi", 
    "在吗", "在吗？", "有人吗", "早上好", "下午好", "晚上好"
);

// 问身份
Set<String> IDENTITY_QUERIES = Set.of(
    "你是谁", "你是什么", "你叫什么名字", "你能做什么",
    "你是干嘛的", "介绍一下自己", "你有什么用"
);

// 无关问题
Set<String> OFF_TOPIC = Set.of(
    "讲个笑话", "天气怎么样", "今天几号", "帮我写代码",
    "1+1等于几", "你会唱歌吗", "推荐一部电影"
);
```

#### ② 大模型分类兜底（模糊输入）

```json
{
  "intent": "greeting|identity|off_topic|data_query",
  "confidence": 0.95,
  "reason": "用户询问身份能力"
}
```

#### 回复策略（模板化，不浪费大模型token）

| 类型 | 触发条件 | 回复内容 |
|------|----------|----------|
| 打招呼 | 关键词匹配 | "你好！我是数据分析助手，可以帮您查询招聘、企业、专利等领域的指标数据。比如您可以问：\n• 北京招聘薪资怎么样？\n• 各省份企业新增排名" |
| 问身份 | 关键词匹配 | "我是Metric Analyst（数据分析智能体），您的专业数据助手。\n\n我擅长：\n📊 查询各类经济指标（招聘、企业、专利、采购）\n📈 分析趋势变化（同比、环比、排名）\n🏆 多地区对比（各省份、各市排名）\n\n您可以这样问我：\n• '北京本科招聘薪资是多少？'\n• '各省份近半年企业新增排名'\n• '深圳专利申请趋势怎么样？'" |
| 无关问题 | 关键词匹配 | "哈哈，这个我还真不太擅长～我是专门做数据分析的，不如我们来聊聊招聘市场或者企业数据？试试问我：近6个月杭州招聘趋势？" |

#### Hook实现

```java
@Component
public class ChitchatHook implements AgentHook {
    @Autowired
    private IntentClassifier classifier;
    
    @Override
    public void beforeProcess(AgentContext context) {
        String userInput = context.getUserInput();
        IntentType intent = classifier.classify(userInput);
        
        if (intent != IntentType.DATA_QUERY) {
            context.setInterrupted(true);
            context.setReply(generateChitchatReply(intent));
        }
        context.setAttribute("intent", intent);
    }
}
```

---

### 阶段1：指标识别（两阶段检索+精排+无匹配处理）

#### 第一层：候选召回（三路并行）

##### ① BM25全文检索（MySQL FULLTEXT）

```sql
-- 利用现有MySQL，ngram中文分词
SELECT * FROM db_indicator 
WHERE MATCH(indicator_name, tags, remark) AGAINST('招聘薪资') 
LIMIT 10;
```

##### ② 向量相似度检索（Java内存计算）

```java
@Component
public class IndicatorVectorStore {
    private Map<String, float[]> embeddings = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void load() {
        // 启动时从MySQL加载：indicator_id -> float[1024]
        List<Indicator> list = indicatorRepository.findAll();
        list.forEach(ind -> embeddings.put(
            ind.getIndicatorId(),
            JSON.parseArray(ind.getEmbeddingJson(), float[].class)
        ));
    }
    
    public List<IndicatorScore> search(String userQuery) {
        // 调用DashScope API获取用户输入embedding
        float[] queryVector = embeddingClient.embed(userQuery);
        
        // 内存计算cosine similarity
        return embeddings.entrySet().stream()
            .map(e -> new IndicatorScore(e.getKey(), 
                cosineSimilarity(queryVector, e.getValue())))
            .sorted(Comparator.comparing(IndicatorScore::score).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }
}
// 内存占用：1000指标 × 1024维 × 4字节 ≈ 4MB
```

##### ③ 同义词倒排索引（Java HashMap）

```java
@Component
public class SynonymIndex {
    private Map<String, List<String>> index = new HashMap<>();
    
    @PostConstruct
    public void build() {
        // 从dimension_values和db_indicator.tags构建
        // Map<"帝都", List<"110000相关指标ID">>
        // Map<"工资", List<"薪资相关指标ID">>
    }
}
// O(1)查找
```

**融合排序**：加权融合三路结果（BM25:0.3 + 向量:0.5 + 同义词:0.2），取Top 3进入第二阶段。

#### 第二层：大模型精排

**Prompt结构**：

```
【系统角色】
你是指标识别专家，从候选指标中选择最匹配用户查询的一个。

【候选指标】
1. 招聘岗位平均薪酬 - 维度：时间、地区、学历、数据属性 - 单位：元 - 描述：招聘岗位的平均薪资水平
2. 招聘岗位数量 - 维度：时间、地区、学历、产业链、数据属性 - 单位：个

【特殊规则】
- 时间格式：YYYY-MM-DD，取频率最后一天
- 地区识别：模型直接识别地区名称（支持别称如"帝都"），输出标准化名称
- 未指定维度使用默认值

【用户输入】
"北京近6个月本科招聘薪资怎么样"

【输出JSON格式】
{
  "selected_indicator": "招聘岗位平均薪酬",
  "indicator_id": "I_RPA_ICN_RAE_SALARY_AMOUNT",
  "confidence": 0.98,
  "extracted_dimensions": {
    "region": {"value": "北京", "normalized": true},
    "time": {"type": "last", "value": 6, "format": "YYYY-MM-DD"},
    "education": {"value": "本科"}
  }
}
```

#### 指标无匹配处理（三层降级）

| 场景 | 触发条件 | 处理方式 | 回复示例 |
|------|----------|----------|----------|
| **领域推荐** | 召回有结果但精排置信度<0.5 | 分析用户意图领域，推荐相似指标 | "抱歉，我没有找到GDP指标。但我可以为您提供市场主体方面的数据：\n📊 新增企业数量（反映市场活力）\n📊 在营企业数量（反映经济体量）" |
| **澄清询问** | 有多个弱候选（0.3-0.5） | 列出Top 3让用户选择 | "您是想查询以下哪个指标？\n1. 招聘岗位平均薪酬 - 反映劳动力市场价格\n2. 招聘市场主体数量 - 反映招聘活跃度\n3. 在营企业数量 - 反映市场主体规模" |
| **全局引导** | 完全无匹配 | 展示所有可查询领域+示例 | "抱歉，我没有理解您想查询什么指标。\n\n目前我可以帮您查询：\n📈 招聘就业（岗位数量、平均薪酬、招聘企业）\n🏢 市场主体（新增、注销、在营企业）\n💡 知识产权（专利申请）\n🛒 政府采购（金额、数量、均价）\n\n💬 试试这样问：\n• '北京招聘薪资'\n• '各省份企业新增排名'\n• '近6个月专利趋势'" |

---

### 阶段2：维度标准化（ToolHook: Pre-tool-call）

#### Hook实现

```java
@Component
public class DimensionNormalizationHook implements ToolHook {
    @Override
    public void beforeToolCall(ToolContext context) {
        Map<String, Object> params = context.getParameters();
        
        // 1. 地区标准化
        if (params.containsKey("region")) {
            String regionInput = (String) params.get("region");
            
            // 识别分组查询（各省份/各市）
            if (isMultiRegionKeyword(regionInput)) {
                String level = parseRegionLevel(regionInput); // 省级/市级
                params.put("region_level", level);
                params.remove("region");
            } else {
                // 具体地区标准化
                String regionCode = regionService.normalize(regionInput);
                params.put("region", regionCode); // 北京→110000
            }
        }
        
        // 2. 时间动态计算
        if (params.containsKey("time")) {
            Object timeValue = params.get("time");
            
            if ("latest".equals(timeValue)) {
                // 取最新时间
                String latest = timeService.getLatestTime(
                    (String) params.get("indicator_id"));
                params.put("time", latest);
            } else if (timeValue.toString().startsWith("last:")) {
                // 近N期展开
                int n = Integer.parseInt(timeValue.toString().split(":")[1]);
                List<String> times = timeService.getLastNPeriods(
                    (String) params.get("indicator_id"), n);
                params.put("time_list", times);
                params.remove("time");
            }
        }
        
        // 3. 默认值填充
        fillDefaultValues(params);
    }
}
```

#### 维度处理三规则

| 维度 | 处理方式 | 细节 |
|------|----------|------|
| **地区** | 模型自识别名称 → 后端编码 | 关键词"各省份"→`region_level='省级'`；具体"北京"→`region='110000'` |
| **时间** | 动态计算 | 未指定→最新；近N期→倒推N个月取最后一天；超范围→降级最新 |
| **其他** | 默认值填充 | 未指定用`db_data_dimension.default_value`（TOTAL/DAT_1） |

#### 时间计算细节

```
最新时间查询：SELECT MAX(time_id) FROM {table_id} → 2024-02-29

近6个月展开：
- 2024-02-29（最新）
- 2024-01-31
- 2023-12-31
- 2023-11-30
- 2023-10-31
- 2023-09-30

超范围处理：
用户："2025年数据" 
→ 系统："2025年数据尚未发布，为您展示最新数据（2024年2月）"
```

---

### 阶段3：数据查询（Skill: MetricQuery）

#### Skill定义

```java
@Component
public class MetricQuerySkill implements Skill {
    
    @Override
    public String getName() { return "metric_query"; }
    
    @Override
    public String getDescription() { 
        return "查询各类经济指标数据，支持单指标查询、趋势分析、多地区对比、排名统计"; 
    }
    
    @Tool(description = "查询指标当前值")
    public MetricResult queryCurrentValue(
        @ToolParam(description = "指标编码") String indicatorId,
        @ToolParam(description = "地区编码") String region,
        @ToolParam(description = "地区级别（各省份/各市时用）") String regionLevel,
        @ToolParam(description = "学历编码") String education,
        @ToolParam(description = "时间或时间列表") Object time,
        @ToolParam(description = "数据属性") String dataAttr) {
        
        // 构建SQL
        String sql = buildSql(indicatorId, region, regionLevel, education, time, dataAttr);
        
        // 执行查询
        List<IndicatorFact> facts = jdbcTemplate.query(sql, new IndicatorFactMapper());
        return new MetricResult(facts);
    }
    
    @Tool(description = "查询指标趋势")
    public TrendResult queryTrend(...) { }
    
    @Tool(description = "查询指标排名")
    public RankingResult queryRanking(...) { }
}
```

#### SQL构建（分组查询差异化）

```sql
-- 场景1：具体地区+具体时间（单条记录）
SELECT * FROM ads_rpa_w_icn_recruit_salary_amount_m
WHERE time_id = '2024-02-29'
  AND region_id = '110000'
  AND education_id = 'RAE_EDU_6'
  AND economic_type_id = 'TOTAL'
  AND data_attr_id = 'DAT_1';

-- 场景2：各省份（地区级别查询）
SELECT region_id, fact_value, value_mom 
FROM ads_rpa_w_icn_recruit_salary_amount_m
WHERE region_level = '省级'  -- 关键：使用region_level
  AND education_id = 'TOTAL'
  AND time_id = '2024-02-29'
ORDER BY fact_value DESC;

-- 场景3：不同学历（排除TOTAL）
SELECT education_id, fact_value 
FROM ads_rpa_w_icn_recruit_salary_amount_m
WHERE region_id = '110000'
  AND education_id != 'TOTAL'  -- 关键：排除汇总值
  AND time_id = '2024-02-29'
ORDER BY fact_value DESC;

-- 场景4：近6个月趋势（时间列表）
SELECT time_id, fact_value, value_mom 
FROM ads_rpa_w_icn_recruit_salary_amount_m
WHERE time_id IN ('2023-09-30','2023-10-31',...,'2024-02-29')
  AND region_id = '110000'
ORDER BY time_id;
```

**关键设计**：预聚合数据，直接精确匹配，无GROUP BY。

---

### 阶段4：结果加工与洞察生成（Hooks）

#### ToolHook: Post-tool-call

```java
@Component
public class ResultProcessingHook implements ToolHook {
    @Override
    public void afterToolCall(ToolContext context, Object result) {
        List<IndicatorFact> facts = (List<IndicatorFact>) result;
        
        // 计算排名
        List<RankingItem> ranking = facts.stream()
            .sorted(Comparator.comparing(IndicatorFact::getFactValue).reversed())
            .map(f -> new RankingItem(f.getRegionId(), f.getFactValue(), 
                                      calculateRank(f, facts)))
            .collect(Collectors.toList());
        
        // 计算趋势
        TrendAnalysis trend = analyzeTrend(facts);
        
        // 存入上下文
        context.setAttribute("ranking", ranking);
        context.setAttribute("trend", trend);
        context.setAttribute("latestValue", getLatestValue(facts));
        context.setAttribute("avgValue", calculateAverage(facts));
    }
}
```

#### AgentHook: Post-process

```java
@Component
public class InsightGenerationHook implements AgentHook {
    @Override
    public void afterProcess(AgentContext context) {
        // 构建数据摘要
        String dataSummary = buildDataSummary(context);
        
        // 调用大模型生成洞察
        String prompt = """
            基于以下数据，生成简洁的分析回复：
            指标：{indicator_name}
            维度：{dimensions}
            数据摘要：{summary}
            
            要求：
            1. 先说最新值和同比
            2. 提及排名（如有）
            3. 描述趋势
            4. 控制在100字以内
            
            数据摘要：""" + dataSummary;
        
        String insight = chatClient.call(prompt);
        context.setFinalReply(insight);
    }
}
```

#### 回复示例

```
"北京本科招聘薪资近6个月呈稳步上升趋势，最新月份（2024年2月）
平均薪资为20,000元，同比增长9.0%，在全国主要城市中排名第2，
仅次于上海（21,000元）。从趋势看，近半年月均增长约300元。"
```

---

## 四、异常情况处理

### 4.1 闲聊处理（阶段0）
- 关键词快速识别，模板回复
- 每次回复都包含能力简介+具体示例
- 引导用户回到数据查询主题

### 4.2 指标无匹配（阶段1）
- **领域推荐**：分析意图领域，推荐相似指标
- **澄清询问**：列出Top候选让用户选择
- **全局引导**：展示所有可查询领域

### 4.3 时间超范围（阶段2）
- 自动降级为最新时间
- 明确告知用户数据时间范围

### 4.4 维度不匹配（阶段2）
- 提示用户该指标不支持的维度
- 建议可用的筛选条件

---

## 五、技术选型

| 组件 | 选型 | 原因 |
|------|------|------|
| **框架** | Spring Boot 3.2 + Spring AI Alibaba 1.1.2.1 | 官方最佳实践，支持ReactAgent、SupervisorAgent、Skills、Hooks |
| **数据库** | MySQL 8.0 | 事实数据+元数据+全文索引（ngram），无需新组件 |
| **大模型** | DashScope（通义千问） | 中文理解强，与Spring AI Alibaba集成好 |
| **向量检索** | Java内存计算 | 指标<1000，启动加载，计算<10ms，内存<10MB |
| **同义词索引** | Java HashMap | 启动构建，O(1)查找 |
| **缓存** | 无 | 数据量小，直接查库+内存计算足够 |

### 内存占用估算

- 指标向量：1000 × 1024 × 4字节 ≈ **4MB**
- 同义词索引：< **1MB**
- **总计：<10MB**

---

## 六、关键设计要点总结

### 6.1 五阶段+Hooks架构

| 阶段 | Hook/Skill | 职责 |
|------|------------|------|
| 0 | AgentHook: Pre-process | 闲聊识别，快速过滤 |
| 1 | 指标识别服务 | 两阶段检索+精排+无匹配处理 |
| 2 | ToolHook: Pre-tool-call | 维度标准化（地区/时间/默认值） |
| 3 | Skill: MetricQuery | 预聚合数据查询 |
| 4 | ToolHook: Post-tool-call | 结果加工（排名/趋势） |
| 4 | AgentHook: Post-process | 洞察生成 |

### 6.2 两阶段指标识别

- **召回阶段**：BM25（MySQL）+ 向量相似度（Java内存）+ 同义词倒排（HashMap）三路并行
- **精排阶段**：大模型语义理解，输出结构化维度

### 6.3 维度处理三规则

| 维度 | 处理方式 | 特殊设计 |
|------|----------|----------|
| 地区 | 模型自识别→后端编码 | `region_level`字段区分全国/省/市 |
| 时间 | 动态计算 | 取频率最后一天（YYYY-MM-DD），超范围自动降级 |
| 其他 | 默认值填充 | 未指定用`default_value`（TOTAL/DAT_1） |

### 6.4 分组查询差异化

- **地区分组**：`region_level='省级'`（包含该级别所有地区）
- **其他维度分组**：`xxx_id!='TOTAL'`（排除汇总，展开明细）

### 6.5 预聚合数据模型

- ADS宽表已计算所有维度组合
- 查询直接精确匹配，无GROUP BY
- 时间取频率最后一天

### 6.6 Spring AI Alibaba 特性应用

- **Skills**：指标查询技能封装，可复用、可动态加载
- **Hooks**：闲聊识别、维度标准化、结果加工声明式拦截

---

## 七、当前实施范围

### 已实现/确定实现

- [x] 数据模型（ADS宽表+元数据+维度值）
- [x] Spring AI Alibaba Skills + Hooks 架构
- [x] 闲聊识别（AgentHook: Pre-process）
- [x] 指标识别两阶段（BM25+向量+同义词召回，大模型精排）
- [x] 指标无匹配处理（三层降级）
- [x] 维度标准化（ToolHook: Pre-tool-call）
- [x] 分组查询（地区用level/其他排除TOTAL）
- [x] 预聚合查询（Skill: MetricQuery）
- [x] 结果加工与洞察生成（Hooks）

### 预留接口

- [ ] AgentRouter（当前直接路由）
- [ ] 其他智能体（文献问答Skill）
- [ ] 复杂跨指标关联分析

---

## 八、附录

### 8.1 项目仓库

- **GitHub**: https://github.com/bfl248034/metric-analyst-agent

### 8.2 核心依赖

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
    <version>1.1.2.1</version>
</dependency>
```

### 8.3 参考文档

- [Spring AI Alibaba 官方文档](https://java2ai.com/docs/frameworks/agent-framework)
- [ReactAgent 模式](https://java2ai.com/docs/frameworks/agent-framework/tutorials/agents)
- [多智能体架构](https://java2ai.com/docs/frameworks/agent-framework/advanced/multi-agent)

---

*文档版本：v1.0*  
*最后更新：2026-03-20*
