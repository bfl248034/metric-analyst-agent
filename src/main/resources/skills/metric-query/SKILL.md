---
name: metric-query
description: 查询各类指标的具体数值，支持多维度筛选。适用于单指标查询、多地区对比、趋势分析、排名查询等场景。
---

# Metric Query Skill - 指标查询技能

## 功能说明

用于查询各类指标的具体数值，支持多维度筛选和复杂分析场景。

## 适用场景

| 场景类型 | 示例 |
|---------|------|
| 单指标查询 | "北京招聘薪资是多少" |
| 多地区对比 | "对比北京上海广东的招聘数量" |
| 趋势分析 | "北京招聘数量最近6个月趋势" |
| 排名查询 | "各省份招聘薪资排名" |

## 可用指标

| 指标名称 | 指标ID | 说明 | 单位 |
|---------|--------|------|------|
| 招聘岗位数量 | I_RPA_ICN_RAE_POSITION_NUM | 企业发布的招聘岗位数量 | 个 |
| 招聘市场主体数量 | I_RPA_ICN_RAE_COMPANY_NUM | 参与招聘的企业主体数量 | 个 |
| 招聘岗位平均薪酬 | I_RPA_ICN_RAE_SALARY_AMOUNT | 招聘岗位的平均薪资水平 | 元 |
| 新增企业数量 | I_RPA_ICN_MKE_COMPANY_ADD_NUM | 新注册登记的企业数量 | 个 |
| 注销企业数量 | I_RPA_ICN_MKE_COMPANY_CANCEL_NUM | 注销登记的企业数量 | 个 |
| 在营企业数量 | I_RPA_ICN_MKE_COMPANY_ON_NUM | 处于在营状态的企业总量 | 个 |
| 专利申请数量 | I_RPA_ICN_PAT_APPLICATION_NUM | 提交的专利申请数量 | 个 |
| 政府采购金额 | I_RPA_ICN_GVP_AMOUNT | 政府采购项目的成交金额 | 万元 |
| 政府采购数量 | I_RPA_ICN_GVP_NUM | 政府采购项目的成交数量 | 个 |
| 政府采购平均价格 | I_RPA_ICN_GVP_AMOUNT_AVG | 政府采购项目的平均成交价格 | 万元/个 |

## 支持维度

| 维度ID | 维度名称 | 示例值 |
|--------|----------|--------|
| region | 地区 | 110000(北京), 310000(上海), 100000(全国), 省级, 市级 |
| time | 时间 | latest(最新), last:N(近N期), 2024-03(具体月份) |
| education | 学历 | TOTAL(汇总), RAE_EDU_6(本科), RAE_EDU_7(硕士) |
| economic_type | 经济类型 | TOTAL, MKE_ECO_3(私营), MKE_ECO_1(国有) |
| spe_tag | 特殊资质 | TOTAL, MKE_SPE_4(高新技术企业) |
| patent_type | 专利类型 | TOTAL, PAT_PTT_2(发明专利) |
| company_type | 申请人类型 | TOTAL, RPA_CTP_1(企业) |
| data_attr | 数据属性 | DAT_1(当期), DAT_2(累计) |
| icn_chain_area | 产业链领域 | ICN_CHAIN_6 |
| icn_chain_link | 产业链环节 | ICN_CHAIN_6 |

## 查询流程

```
用户输入
    ↓
指标识别（BM25+向量+同义词+大模型精排）
    ↓
维度标准化（地区/时间/其他维度）
    ↓
动态SQL构建（根据 db_data_dimension 配置）
    ↓
数据查询（参数化查询）
    ↓
结果加工 + 洞察生成
```

## 注意事项

- 地区编码使用6位国标编码（如110000=北京）
- 时间支持：latest(最新)/last:N(近N期)/YYYY-MM格式
- 未指定的维度使用 db_data_dimension 中的默认值
- 数值结果保留2位小数
- 排名查询默认返回TOP20
