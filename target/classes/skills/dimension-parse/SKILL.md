---
name: dimension-parse
description: This skill should be used when parsing dimension values from user query, including region codes, time periods, education levels, and other categorical dimensions.
---

# Dimension Parse Skill - 维度解析技能

## 功能说明

从用户查询中解析各类维度的具体值，支持自然语言识别和编码转换。

## 支持维度

### 1. 地区维度 (region)

**编码规则**：6位国标编码
- 100000 = 全国
- 110000 = 北京市
- 310000 = 上海市
- 440000 = 广东省

**级别标记**：
- LEVEL_2 = 不同省份/地区（省级）
- LEVEL_3 = 不同市（地级）
- LEVEL_4 = 不同区县（县级）

**识别示例**：
| 用户表述 | 解析结果 |
|----------|----------|
| 北京 | 110000 |
| 上海 | 310000 |
| 全国 | 100000 |
| 不同省份 | LEVEL_2 |
| 广东和浙江 | [440000, 330000] |

### 2. 时间维度 (time)

**格式支持**：
- YYYYMM：如 202401
- recent_N：如 recent_3 (最近3期)
- 特殊标记：last_year, this_year, last_month, current

**识别示例**：
| 用户表述 | 解析结果 |
|----------|----------|
| 2024年1月 | 202401 |
| 最近3个月 | recent_3 |
| 上个月 | last_month |
| 今年 | this_year |

### 3. 学历维度 (education)

| 编码 | 名称 |
|------|------|
| 1 | 博士 |
| 2 | 硕士 |
| 3 | 本科 |
| 4 | 大专 |
| 5 | 高中 |
| 0 | 不限 |

### 4. 产业链维度 (industry_chain)

| 编码 | 名称 |
|------|------|
| material | 材料 |
| component | 零部件 |
| assembly | 整机装配 |
| sales | 销售服务 |

## 分维度分析

当用户说"不同学历"、"分地区"时：
1. 排除汇总值（all/全部/不限）
2. 返回该维度所有有效值

## 工具使用

- **dimension_value_lookup** - 查询维度值编码
- **region_code_resolve** - 地区编码解析
- **time_parser** - 时间解析
