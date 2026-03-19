-- ============================================
-- 指标分析智能体 - 数据库初始化脚本
-- ============================================

-- 1. 指标元数据表
CREATE TABLE IF NOT EXISTS db_indicator (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    indicator_id VARCHAR(64) NOT NULL UNIQUE COMMENT '指标编码，如 I_RPA_ICN_RAE_SALARY_AMOUNT',
    indicator_name VARCHAR(128) NOT NULL COMMENT '指标名称',
    unit VARCHAR(32) COMMENT '单位',
    frequency VARCHAR(10) NOT NULL COMMENT '频率: M(月)/Q(季)/Y(年)',
    valid_measures VARCHAR(256) COMMENT '有效度量：当期，当期同比，累计，累计同比',
    table_id VARCHAR(128) COMMENT '对应事实表名',
    remark TEXT COMMENT '业务描述',
    domain VARCHAR(64) COMMENT '领域：招聘就业/市场主体/知识产权/政府采购',
    subdomain VARCHAR(64) COMMENT '子领域',
    tags VARCHAR(256) COMMENT '同义词标签，逗号分隔',
    indexed BOOLEAN DEFAULT FALSE COMMENT '是否已建立索引',
    index_version BIGINT COMMENT '索引版本',
    last_indexed_at DATETIME COMMENT '最后索引时间',
    embedding_json TEXT COMMENT '预计算向量 JSON',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_domain (domain),
    INDEX idx_table_id (table_id),
    FULLTEXT INDEX ft_name_tags (indicator_name, tags, remark) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标元数据表';

-- 2. 维度值表
CREATE TABLE IF NOT EXISTS dimension_values (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dimension_id VARCHAR(64) NOT NULL COMMENT '维度编码：region/education/time/...',
    dimension_name VARCHAR(64) COMMENT '维度名称',
    value_code VARCHAR(64) NOT NULL COMMENT '维度值编码',
    value_name VARCHAR(128) NOT NULL COMMENT '维度值名称',
    synonyms VARCHAR(500) COMMENT '同义词，逗号分隔',
    parent_code VARCHAR(64) COMMENT '父级编码',
    sort_order INT COMMENT '排序',
    indexed BOOLEAN DEFAULT FALSE COMMENT '是否已索引',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_dim_value (dimension_id, value_code),
    INDEX idx_parent (parent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维度值表';

-- 3. 数据维度关联表
CREATE TABLE IF NOT EXISTS db_data_dimension (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id VARCHAR(128) NOT NULL COMMENT '事实表名',
    dimension_id VARCHAR(64) NOT NULL COMMENT '维度编码',
    dimension_name VARCHAR(64) COMMENT '维度名称',
    dimension_code VARCHAR(64) COMMENT '维度编码（备用）',
    is_common BOOLEAN DEFAULT TRUE COMMENT '是否公共维度',
    is_required BOOLEAN DEFAULT FALSE COMMENT '是否必填',
    default_value VARCHAR(64) COMMENT '默认值',
    dimension_type VARCHAR(20) COMMENT '类型：temporal/categorical',
    sort_order INT COMMENT '排序',
    UNIQUE INDEX uk_table_dim (table_id, dimension_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据维度关联表';

-- 4. 指标事实表（示例表，实际每张指标一张表）
CREATE TABLE IF NOT EXISTS indicator_fact (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    time_id DATE NOT NULL COMMENT '时间ID，取频率最后一天',
    region_id VARCHAR(64) NOT NULL COMMENT '地区编码',
    region_level VARCHAR(20) COMMENT '地区级别：全国/省级/市级/区县级',
    education_id VARCHAR(64) COMMENT '学历编码',
    economic_type_id VARCHAR(64) COMMENT '经济类型编码',
    spe_tag_id VARCHAR(64) COMMENT '资质标签编码',
    patent_type_id VARCHAR(64) COMMENT '专利类型编码',
    company_type_id VARCHAR(64) COMMENT '公司类型编码',
    price_range_id VARCHAR(64) COMMENT '价格区间编码',
    scene_type_id VARCHAR(64) COMMENT '场景类型编码',
    data_attr_id VARCHAR(64) COMMENT '数据属性编码',
    icn_chain_area_id VARCHAR(64) COMMENT '产业链领域编码',
    icn_chain_link_id VARCHAR(64) COMMENT '产业链环节编码',
    fact_value DECIMAL(18,2) COMMENT '指标值',
    value_mom DECIMAL(8,2) COMMENT '环比',
    value_yoy DECIMAL(8,2) COMMENT '同比',
    table_id VARCHAR(128) COMMENT '表标识',
    INDEX idx_time_region (time_id, region_id),
    INDEX idx_region_level (region_level),
    INDEX idx_time (time_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标事实表模板';

-- ============================================
-- 初始化数据
-- ============================================

-- 初始化地区维度
INSERT INTO dimension_values (dimension_id, dimension_name, value_code, value_name, synonyms, parent_code, sort_order, indexed) VALUES
('region', '地区', '100000', '全国', '中国,全国,全中国,国家', NULL, 1, TRUE),
('region', '地区', '110000', '北京市', '北京,帝都,京城', '100000', 2, TRUE),
('region', '地区', '310000', '上海市', '上海,魔都,沪', '100000', 3, TRUE),
('region', '地区', '440000', '广东省', '广东,粤,岭南', '100000', 4, TRUE),
('region', '地区', '440100', '广州市', '广州,羊城,穗', '440000', 5, TRUE),
('region', '地区', '440300', '深圳市', '深圳,鹏城,深', '440000', 6, TRUE),
('region', '地区', '330000', '浙江省', '浙江,浙', '100000', 7, TRUE),
('region', '地区', '330100', '杭州市', '杭州,杭', '330000', 8, TRUE),
('region', '地区', '320000', '江苏省', '江苏,苏', '100000', 9, TRUE),
('region', '地区', '320500', '苏州市', '苏州,苏', '320000', 10, TRUE)
ON DUPLICATE KEY UPDATE synonyms = VALUES(synonyms);

-- 初始化学历维度
INSERT INTO dimension_values (dimension_id, dimension_name, value_code, value_name, synonyms, parent_code, sort_order, indexed) VALUES
('education', '学历', 'TOTAL', '全部', '所有,不限', NULL, 1, TRUE),
('education', '学历', 'RAE_EDU_6', '本科', '本科,大学,学士学位', NULL, 2, TRUE),
('education', '学历', 'RAE_EDU_7', '硕士', '硕士,研究生', NULL, 3, TRUE),
('education', '学历', 'RAE_EDU_8', '博士', '博士,博士研究生', NULL, 4, TRUE)
ON DUPLICATE KEY UPDATE synonyms = VALUES(synonyms);

-- 初始化数据属性维度
INSERT INTO dimension_values (dimension_id, dimension_name, value_code, value_name, synonyms, parent_code, sort_order, indexed) VALUES
('data_attr', '数据属性', 'DAT_1', '当期', '当期值,最新', NULL, 1, TRUE),
('data_attr', '数据属性', 'DAT_2', '累计', '累计值', NULL, 2, TRUE)
ON DUPLICATE KEY UPDATE synonyms = VALUES(synonyms);

-- 初始化示例指标
INSERT INTO db_indicator (indicator_id, indicator_name, unit, frequency, valid_measures, table_id, remark, domain, subdomain, tags, indexed) VALUES
('I_RPA_ICN_RAE_SALARY_AMOUNT', '招聘岗位平均薪酬', '元', 'M', '当期,当期同比', 'ads_rpa_w_icn_recruit_salary_amount_m', 
 '招聘岗位的平均薪资水平，反映劳动力市场价格', '招聘就业', '薪资水平', '薪资,工资,薪酬,收入,待遇', TRUE),
('I_RPA_ICN_RAE_POSITION_NUM', '招聘岗位数量', '个', 'M', '当期,当期同比', 'ads_rpa_w_icn_edu_recruit_position_num_m',
 '招聘市场的岗位数量，反映招聘需求', '招聘就业', '招聘需求', '岗位,招聘,职位,工作机会', TRUE),
('I_RPA_ICN_ECO_SPE_COMPANY_ADD_NUM', '新增企业数量', '个', 'M', '当期,当期同比', 'ads_rpa_w_icn_eco_spe_company_add_num_m',
 '新增注册企业数量，反映市场活力', '市场主体', '企业增量', '新增企业,新注册,创业', TRUE)
ON DUPLICATE KEY UPDATE indicator_name = VALUES(indicator_name), tags = VALUES(tags);

-- 初始化维度关联
INSERT INTO db_data_dimension (table_id, dimension_id, dimension_name, is_common, is_required, default_value, dimension_type, sort_order) VALUES
('ads_rpa_w_icn_recruit_salary_amount_m', 'region', '地区', TRUE, TRUE, '100000', 'categorical', 1),
('ads_rpa_w_icn_recruit_salary_amount_m', 'time', '时间', TRUE, TRUE, 'latest', 'temporal', 2),
('ads_rpa_w_icn_recruit_salary_amount_m', 'education', '学历', TRUE, FALSE, 'TOTAL', 'categorical', 3),
('ads_rpa_w_icn_recruit_salary_amount_m', 'data_attr', '数据属性', TRUE, FALSE, 'DAT_1', 'categorical', 4)
ON DUPLICATE KEY UPDATE default_value = VALUES(default_value);

-- 插入示例事实数据
INSERT INTO indicator_fact (time_id, region_id, region_level, education_id, data_attr_id, fact_value, value_mom, value_yoy, table_id) VALUES
('2024-02-29', '100000', '全国', 'TOTAL', 'DAT_1', 15000.00, 4.5, 6.8, 'ads_rpa_w_icn_recruit_salary_amount_m'),
('2024-02-29', '110000', '省级', 'TOTAL', 'DAT_1', 18000.00, 5.0, 8.2, 'ads_rpa_w_icn_recruit_salary_amount_m'),
('2024-02-29', '310000', '省级', 'TOTAL', 'DAT_1', 20000.00, 6.0, 9.5, 'ads_rpa_w_icn_recruit_salary_amount_m'),
('2024-02-29', '440100', '市级', 'TOTAL', 'DAT_1', 16000.00, 4.8, 7.5, 'ads_rpa_w_icn_recruit_salary_amount_m'),
('2024-02-29', '330100', '市级', 'TOTAL', 'DAT_1', 17000.00, 5.2, 8.0, 'ads_rpa_w_icn_recruit_salary_amount_m'),
('2024-02-29', '110000', '省级', 'RAE_EDU_6', 'DAT_1', 22000.00, 5.5, 9.0, 'ads_rpa_w_icn_recruit_salary_amount_m')
ON DUPLICATE KEY UPDATE fact_value = VALUES(fact_value), value_mom = VALUES(value_mom), value_yoy = VALUES(value_yoy);
