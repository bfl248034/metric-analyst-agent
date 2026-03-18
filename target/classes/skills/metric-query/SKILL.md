---
name: metric-query
description: This skill should be used when the user wants to query a specific metric value with dimensions like region, time, education level, etc. It supports single metric queries and multi-dimensional filtering.
---

# Metric Query Skill - 指标查询技能

## 功能说明

用于查询各类指标的具体数值，支持多维度筛选。

## 适用场景

- 查询某地区某时间的指标数值
- 按维度筛选（地区、时间、学历、产业链环节等）
- 单指标精确查询

## 可用维度

| 维度ID | 维度名称 | 示例值 |
|--------|----------|--------|
| region | 地区 | 110000(北京), 310000(上海), 100000(全国) |
| time | 时间 | 202401, recent_3 |
| education | 学历 | 1(博士), 2(硕士), 3(本科) |
| industry_chain | 产业链环节 | material(材料), component(零部件) |
| company_type | 企业类型 | enterprise(企业), research(研究机构) |

## 查询示例

### 示例1：地区+时间查询
用户：北京上个月招聘了多少人？
- 指标：招聘数量 (recruitment_count)
- 地区：110000 (北京)
- 时间：recent_1 (最近1期)

### 示例2：多维度查询
用户：北京本科招聘数量
- 指标：招聘数量
- 地区：110000 (北京)
- 学历：3 (本科)
- 时间：latest (最新)

### 示例3：全国查询
用户：全国上个月专利数量
- 指标：专利数量 (patent_count)
- 地区：100000 (全国)
- 时间：recent_1

## 工具使用

1. **metric_retrieval** - 根据描述匹配指标
2. **dimension_parse** - 解析维度值
3. **sql_execute** - 执行数据查询

## 注意事项

- 地区编码使用6位国标编码
- 时间支持：YYYYMM 格式 或 recent_N 格式
- 如果维度值不明确，使用默认值
- 数值结果保留2位小数
