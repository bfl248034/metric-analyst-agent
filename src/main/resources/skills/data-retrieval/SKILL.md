---
name: data-retrieval
description: This skill should be used when retrieving data from databases or Elasticsearch. It supports SQL execution and semantic search.
---

# Data Retrieval Skill - 数据检索技能

## 功能说明

从各类数据源检索指标数据，支持 SQL 查询和语义检索。

## 数据源

### 1. MySQL 数据源
- 指标数据表
- 维度数据表
- 元数据表

### 2. Elasticsearch
- 指标语义索引
- 维度值索引

## 检索方式

### SQL 查询
```sql
-- 单指标查询
SELECT time_id, region_code, value 
FROM indicator_data 
WHERE indicator_id = ? 
  AND region_code = ? 
  AND time_id = ?

-- 多维度查询
SELECT * FROM indicator_data 
WHERE indicator_id = ? 
  AND region_code IN (?, ?)
  AND education_code = ?
```

### 语义检索
使用 Elasticsearch 进行：
- 指标名称匹配
- 指标描述匹配
- 同义词扩展

## 性能优化

- 查询缓存
- 索引优化
- 分页查询
- 异步执行

## 工具使用

- **sql_execute** - SQL执行
- **es_search** - ES语义检索
- **cache_get** - 缓存查询
