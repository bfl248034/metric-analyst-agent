---
name: data-retrieval
description: 从MySQL数据源检索指标数据，支持动态SQL构建和参数化查询。基于 db_data_source、db_data_table、db_data_dimension 配置架构。
---

# Data Retrieval Skill - 数据检索技能

## 功能说明

从MySQL数据源检索指标数据，支持动态SQL构建和参数化查询。

## 架构设计

```
┌─────────────────────────────────────────┐
│         db_data_source                  │
│  数据源配置（host/port/user/password）   │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│         db_data_table                   │
│  表元数据（table_name/time_column/      │
│           region_column/value_column）  │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│         db_data_dimension               │
│  维度字段映射（dimension_id →           │
│               dimension_code）          │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│    DynamicDataSourceManager             │
│    管理多数据源连接池                   │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│    DynamicQueryBuilder                  │
│    根据维度配置动态构建SQL              │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│    DataQueryService                     │
│    执行参数化查询                       │
└─────────────────────────────────────────┘
```

## 查询构建

### 标准查询

```sql
SELECT time_id, region_id, education_id, fact_value
FROM {table_name}
WHERE time_id = (SELECT MAX(time_id) FROM {table_name})  -- latest
  AND region_id = ?
  AND education_id = ?
ORDER BY time_id
```

### 趋势查询（近N期）

```sql
SELECT time_id, region_id, fact_value
FROM {table_name}
WHERE time_id IN (
    SELECT DISTINCT time_id 
    FROM {table_name} 
    ORDER BY time_id DESC 
    LIMIT ?
)
  AND region_id = ?
ORDER BY time_id
```

### 排名查询（省级分组）

```sql
SELECT region_id, SUM(fact_value) as total_value
FROM {table_name}
WHERE time_id = (SELECT MAX(time_id) FROM {table_name})
  AND (region_id LIKE '__0000' OR region_id = '100000')
  AND region_id != 'TOTAL'
GROUP BY region_id
ORDER BY total_value DESC
LIMIT 20
```

## 参数化查询

所有查询使用 PreparedStatement 参数绑定，防止SQL注入：

```java
// 参数按顺序设置
stmt.setObject(1, regionCode);
stmt.setObject(2, educationCode);
// ...
```

## 性能优化

- HikariCP 连接池管理
- SQL 参数化缓存
- 连接池大小：max=10, minIdle=2
- 连接超时：30秒

## 错误处理

- 数据源不可用：返回友好错误提示
- 表不存在：检查 db_data_table 配置
- 查询超时：降级为最新单期数据
