---
name: metric-query
description: |
  指标查询专家。专门处理指标数据查询，支持：
  - 单指标当前值查询
  - 多期趋势查询
  - 地区排名查询
  - 多地区对比查询
---

# Metric Query Skill

## 可用工具

### normalizeDimensions
**用途**: 将用户输入的维度值标准化为系统编码

**参数**:
- indicatorId: 指标编码
- dimensions: 维度映射（region/time/education等）

**示例**:
```
输入: {"region": "北京", "time": "latest"}
输出: {"region": "110000", "time": "2024-02-29"}
```

### queryCurrentValue
**用途**: 查询单个指标在指定地区的当前值

**参数**:
- indicatorId: 指标编码
- dimensions: 标准化后的维度

**返回**:
```json
{
  "value": 20000,
  "unit": "元",
  "timeId": "2024-02-29",
  "yoy": 9.0,
  "mom": 4.5
}
```

### queryTrend
**用途**: 查询指标的历史趋势

**参数**:
- indicatorId: 指标编码
- regionCode: 地区编码
- months: 查询月数（1-24）

**返回**: 时间序列数据列表

### queryRanking
**用途**: 查询指标的地区排名

**参数**:
- indicatorId: 指标编码
- regionLevel: 地区级别（省级/市级/全国）
- topN: 返回数量（默认10，最大20）

**返回**: 排名列表 [{rank, regionId, value}, ...]

### queryComparison
**用途**: 对比多个地区的同一指标

**参数**:
- indicatorId: 指标编码
- regionCodes: 地区编码列表
- time: 时间

## 查询流程

```
1. 提取用户输入中的指标和维度
2. 调用 normalizeDimensions 标准化维度
3. 根据查询类型选择查询工具
4. 返回结构化数据
```

## 可查询的指标

| 指标名称 | 编码 | 单位 | 支持维度 |
|---------|------|------|---------|
| 招聘岗位数量 | I_RPA_ICN_RAE_POSITION_NUM | 个 | region, time, education |
| 招聘薪资 | I_RPA_ICN_RAE_SALARY_AMOUNT | 元 | region, time, education |
| 新增企业数量 | I_RPA_ICN_MKE_COMPANY_ADD_NUM | 个 | region, time, economic_type |
| 在营企业数量 | I_RPA_ICN_MKE_COMPANY_ON_NUM | 个 | region, time, economic_type |
| 专利申请数量 | I_RPA_ICN_PAT_APPLICATION_NUM | 个 | region, time, patent_type |

## 维度说明

### region 地区
- 支持名称：北京、上海、广州、深圳、杭州等
- 支持别称：帝都=北京，魔都=上海，鹏城=深圳
- 支持级别查询：各省份、各市、全国

### time 时间
- latest: 最新数据
- last:N: 近N期数据
- YYYY-MM-DD: 具体日期

### education 学历
- TOTAL: 全部
- 本科、硕士、博士等

### economic_type 经济类型
- TOTAL: 全部
- 国有、私营、外资等
