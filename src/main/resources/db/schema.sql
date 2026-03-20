-- ============================================
-- 初始化数据
-- ============================================

-- 初始化数据源配置（示例：本地MySQL）
INSERT INTO db_data_source (source_id, source_name, source_type, jdbc_url, username, password, driver_class, pool_size, enabled) VALUES
('local_mysql', '本地MySQL数据库', 'MYSQL', 'jdbc:mysql://localhost:3306/metric_analyst?useSSL=false&serverTimezone=Asia/Shanghai', 'root', '', 'com.mysql.cj.jdbc.Driver', 10, TRUE)
ON DUPLICATE KEY UPDATE jdbc_url = VALUES(jdbc_url);

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
('I_RPA_ICN_RAE_SALARY_AMOUNT', '招聘岗位平均薪酬', '元', 'M', '当期,当期同比', 'tbl_recruit_salary', 
 '招聘岗位的平均薪资水平，反映劳动力市场价格', '招聘就业', '薪资水平', '薪资,工资,薪酬,收入,待遇', TRUE),
('I_RPA_ICN_RAE_POSITION_NUM', '招聘岗位数量', '个', 'M', '当期,当期同比', 'tbl_recruit_position',
 '招聘市场的岗位数量，反映招聘需求', '招聘就业', '招聘需求', '岗位,招聘,职位,工作机会', TRUE),
('I_RPA_ICN_ECO_SPE_COMPANY_ADD_NUM', '新增企业数量', '个', 'M', '当期,当期同比', 'tbl_company_add',
 '新增注册企业数量，反映市场活力', '市场主体', '企业增量', '新增企业,新注册,创业', TRUE)
ON DUPLICATE KEY UPDATE indicator_name = VALUES(indicator_name), tags = VALUES(tags);

-- 初始化数据表配置
INSERT INTO db_data_table (table_id, indicator_id, source_id, table_name, value_column, mom_column, yoy_column, time_column, frequency, enabled) VALUES
('tbl_recruit_salary', 'I_RPA_ICN_RAE_SALARY_AMOUNT', 'local_mysql', 'ads_rpa_w_icn_recruit_salary_amount_m', 'fact_value', 'value_mom', 'value_yoy', 'time_id', 'M', TRUE),
('tbl_recruit_position', 'I_RPA_ICN_RAE_POSITION_NUM', 'local_mysql', 'ads_rpa_w_icn_edu_recruit_position_num_m', 'fact_value', 'value_mom', 'value_yoy', 'time_id', 'M', TRUE),
('tbl_company_add', 'I_RPA_ICN_ECO_SPE_COMPANY_ADD_NUM', 'local_mysql', 'ads_rpa_w_icn_eco_spe_company_add_num_m', 'fact_value', 'value_mom', 'value_yoy', 'time_id', 'M', TRUE)
ON DUPLICATE KEY UPDATE table_name = VALUES(table_name);

-- 初始化维度定义
INSERT INTO db_data_dimension (table_id, dimension_id, dimension_name, is_common, is_required, default_value, dimension_type, sort_order) VALUES
-- 薪资表维度
('tbl_recruit_salary', 'region', '地区', TRUE, TRUE, '100000', 'categorical', 1),
('tbl_recruit_salary', 'time', '时间', TRUE, TRUE, 'latest', 'temporal', 2),
('tbl_recruit_salary', 'education', '学历', TRUE, FALSE, 'TOTAL', 'categorical', 3),
('tbl_recruit_salary', 'data_attr', '数据属性', TRUE, FALSE, 'DAT_1', 'categorical', 4),
-- 招聘数量表维度
('tbl_recruit_position', 'region', '地区', TRUE, TRUE, '100000', 'categorical', 1),
('tbl_recruit_position', 'time', '时间', TRUE, TRUE, 'latest', 'temporal', 2),
('tbl_recruit_position', 'education', '学历', TRUE, FALSE, 'TOTAL', 'categorical', 3),
-- 新增企业表维度
('tbl_company_add', 'region', '地区', TRUE, TRUE, '100000', 'categorical', 1),
('tbl_company_add', 'time', '时间', TRUE, TRUE, 'latest', 'temporal', 2)
ON DUPLICATE KEY UPDATE default_value = VALUES(default_value);

-- ============================================
-- 示例数据表创建（可选，实际由ETL或外部系统创建）
-- ============================================

/*
-- 薪资表示例
CREATE TABLE IF NOT EXISTS ads_rpa_w_icn_recruit_salary_amount_m (
    time_id DATE NOT NULL COMMENT '时间',
    region_id VARCHAR(64) NOT NULL COMMENT '地区',
    education_id VARCHAR(64) COMMENT '学历',
    data_attr_id VARCHAR(64) COMMENT '数据属性',
    fact_value DECIMAL(18,2) COMMENT '指标值',
    value_mom DECIMAL(8,2) COMMENT '环比',
    value_yoy DECIMAL(8,2) COMMENT '同比',
    PRIMARY KEY (time_id, region_id, education_id, data_attr_id)
);

-- 插入示例数据
INSERT INTO ads_rpa_w_icn_recruit_salary_amount_m VALUES
('2024-02-29', '100000', 'TOTAL', 'DAT_1', 15000.00, 4.5, 6.8),
('2024-02-29', '110000', 'TOTAL', 'DAT_1', 18000.00, 5.0, 8.2),
('2024-02-29', '310000', 'TOTAL', 'DAT_1', 20000.00, 6.0, 9.5)
ON DUPLICATE KEY UPDATE fact_value = VALUES(fact_value);
*/