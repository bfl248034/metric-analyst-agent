-- MySQL 初始化脚本 - metric-analyst 数据库
-- 创建数据库
CREATE DATABASE IF NOT EXISTS metric_analyst DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE metric_analyst;

-- 指标维度表
CREATE TABLE IF NOT EXISTS indicator_dim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    indicator_code VARCHAR(100) NOT NULL COMMENT '指标编码',
    indicator_name VARCHAR(200) NOT NULL COMMENT '指标名称',
    indicator_category VARCHAR(100) COMMENT '指标分类',
    unit VARCHAR(50) COMMENT '单位',
    description TEXT COMMENT '指标描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_indicator_code (indicator_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标维度表';

-- 地区维度表
CREATE TABLE IF NOT EXISTS region_dim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    region_code VARCHAR(50) NOT NULL COMMENT '地区编码',
    region_name VARCHAR(100) NOT NULL COMMENT '地区名称',
    region_level VARCHAR(20) COMMENT '地区级别: province/city/district',
    parent_code VARCHAR(50) COMMENT '上级地区编码',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_region_code (region_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地区维度表';

-- 时间维度表
CREATE TABLE IF NOT EXISTS time_dim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    year INT NOT NULL COMMENT '年份',
    month INT COMMENT '月份',
    quarter INT COMMENT '季度',
    date_str VARCHAR(20) COMMENT '日期字符串',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_time (year, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='时间维度表';

-- 指标事实表
CREATE TABLE IF NOT EXISTS indicator_fact (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    indicator_code VARCHAR(100) NOT NULL COMMENT '指标编码',
    region_code VARCHAR(50) NOT NULL COMMENT '地区编码',
    year INT NOT NULL COMMENT '年份',
    month INT COMMENT '月份',
    education_level_code VARCHAR(50) COMMENT '教育水平编码',
    company_type_code VARCHAR(50) COMMENT '公司类型编码',
    metric_value DECIMAL(18,4) COMMENT '指标值',
    value_yoy DECIMAL(10,4) COMMENT '同比增长率%',
    value_mom DECIMAL(10,4) COMMENT '环比增长率%',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fact (indicator_code, region_code, year, month, education_level_code, company_type_code),
    INDEX idx_indicator_region (indicator_code, region_code),
    INDEX idx_region_time (region_code, year, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标事实表';

-- 教育水平维度表
CREATE TABLE IF NOT EXISTS education_level_dim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    level_code VARCHAR(50) NOT NULL COMMENT '教育水平编码',
    level_name VARCHAR(100) NOT NULL COMMENT '教育水平名称',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_level_code (level_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教育水平维度表';

-- 公司类型维度表
CREATE TABLE IF NOT EXISTS company_type_dim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(50) NOT NULL COMMENT '公司类型编码',
    type_name VARCHAR(100) NOT NULL COMMENT '公司类型名称',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公司类型维度表';

-- ===========================================
-- 初始化数据
-- ===========================================

-- 初始化指标
INSERT INTO indicator_dim (indicator_code, indicator_name, indicator_category, unit, description) VALUES
('recruitment_count', '招聘数量', '人力资源', '个', '地区招聘数量统计'),
('avg_salary', '平均薪资', '薪酬福利', '元', '地区平均薪资水平'),
('job_seekers_count', '求职者数量', '人力资源', '个', '求职者数量统计'),
('talent_demand_index', '人才需求指数', '人才市场', '指数', '人才需求综合指数'),
('salary_growth_rate', '薪资增长率', '薪酬福利', '%', '薪资同比增长率');

-- 初始化地区
INSERT INTO region_dim (region_code, region_name, region_level, parent_code) VALUES
('CN', '中国', 'country', NULL),
('110000', '北京', 'province', 'CN'),
('310000', '上海', 'province', 'CN'),
('440100', '广州', 'city', 'CN'),
('440300', '深圳', 'city', 'CN'),
('330100', '杭州', 'city', 'CN'),
('320500', '苏州', 'city', 'CN'),
('420100', '武汉', 'city', 'CN'),
('510100', '成都', 'city', 'CN'),
('500000', '重庆', 'province', 'CN'),
('120000', '天津', 'province', 'CN'),
('610100', '西安', 'city', 'CN');

-- 初始化教育水平
INSERT INTO education_level_dim (level_code, level_name, sort_order) VALUES
('high_school', '高中及以下', 1),
('associate', '大专', 2),
('bachelor', '本科', 3),
('master', '硕士', 4),
('phd', '博士', 5);

-- 初始化公司类型
INSERT INTO company_type_dim (type_code, type_name) VALUES
('private', '民营企业'),
('state_owned', '国有企业'),
('foreign', '外资企业'),
('joint_venture', '合资企业'),
('startup', '创业公司');

-- 初始化时间维度 (2023-2024)
INSERT INTO time_dim (year, month, quarter, date_str) VALUES
(2023, 1, 1, '2023-01'),
(2023, 2, 1, '2023-02'),
(2023, 3, 1, '2023-03'),
(2023, 4, 2, '2023-04'),
(2023, 5, 2, '2023-05'),
(2023, 6, 2, '2023-06'),
(2023, 7, 3, '2023-07'),
(2023, 8, 3, '2023-08'),
(2023, 9, 3, '2023-09'),
(2023, 10, 4, '2023-10'),
(2023, 11, 4, '2023-11'),
(2023, 12, 4, '2023-12'),
(2024, 1, 1, '2024-01'),
(2024, 2, 1, '2024-02'),
(2024, 3, 1, '2024-03'),
(2024, 4, 2, '2024-04'),
(2024, 5, 2, '2024-05'),
(2024, 6, 2, '2024-06');

-- 初始化示例数据：北京2024年各月招聘数量
INSERT INTO indicator_fact (indicator_code, region_code, year, month, metric_value, value_yoy, value_mom) VALUES
('recruitment_count', '110000', 2024, 1, 125000, 8.5, -5.2),
('recruitment_count', '110000', 2024, 2, 98000, 5.2, -21.6),
('recruitment_count', '110000', 2024, 3, 142000, 12.3, 44.9),
('recruitment_count', '110000', 2024, 4, 135000, 7.8, -4.9),
('recruitment_count', '110000', 2024, 5, 148000, 9.1, 9.6),
('recruitment_count', '110000', 2024, 6, 152000, 10.2, 2.7);

-- 初始化示例数据：上海2024年各月招聘数量
INSERT INTO indicator_fact (indicator_code, region_code, year, month, metric_value, value_yoy, value_mom) VALUES
('recruitment_count', '310000', 2024, 1, 118000, 6.2, -3.1),
('recruitment_count', '310000', 2024, 2, 95000, 3.8, -19.5),
('recruitment_count', '310000', 2024, 3, 138000, 11.5, 45.3),
('recruitment_count', '310000', 2024, 4, 132000, 8.2, -4.3),
('recruitment_count', '310000', 2024, 5, 145000, 9.8, 9.8),
('recruitment_count', '310000', 2024, 6, 149000, 10.5, 2.8);

-- 初始化示例数据：杭州2024年各月招聘数量
INSERT INTO indicator_fact (indicator_code, region_code, year, month, metric_value, value_yoy, value_mom) VALUES
('recruitment_count', '330100', 2024, 1, 85000, 15.2, -2.8),
('recruitment_count', '330100', 2024, 2, 68000, 12.5, -20.0),
('recruitment_count', '330100', 2024, 3, 98000, 18.3, 44.1),
('recruitment_count', '330100', 2024, 4, 92000, 14.8, -6.1),
('recruitment_count', '330100', 2024, 5, 105000, 16.2, 14.1),
('recruitment_count', '330100', 2024, 6, 108000, 17.5, 2.9);

-- 初始化示例数据：深圳2024年各月招聘数量
INSERT INTO indicator_fact (indicator_code, region_code, year, month, metric_value, value_yoy, value_mom) VALUES
('recruitment_count', '440300', 2024, 1, 105000, 9.8, -4.2),
('recruitment_count', '440300', 2024, 2, 82000, 6.5, -21.9),
('recruitment_count', '440300', 2024, 3, 125000, 14.2, 52.4),
('recruitment_count', '440300', 2024, 4, 118000, 10.5, -5.6),
('recruitment_count', '440300', 2024, 5, 132000, 12.8, 11.9),
('recruitment_count', '440300', 2024, 6, 138000, 13.5, 4.5);

-- 初始化示例数据：广州2024年各月招聘数量
INSERT INTO indicator_fact (indicator_code, region_code, year, month, metric_value, value_yoy, value_mom) VALUES
('recruitment_count', '440100', 2024, 1, 98000, 7.5, -3.5),
('recruitment_count', '440100', 2024, 2, 78000, 4.2, -20.4),
('recruitment_count', '440100', 2024, 3, 115000, 11.8, 47.4),
('recruitment_count', '440100', 2024, 4, 108000, 8.5, -6.1),
('recruitment_count', '440100', 2024, 5, 122000, 10.2, 13.0),
('recruitment_count', '440100', 2024, 6, 128000, 11.5, 4.9);

-- 初始化示例数据：平均薪资 (2024年6月)
INSERT INTO indicator_fact (indicator_code, region_code, year, month, metric_value, value_yoy) VALUES
('avg_salary', '110000', 2024, 6, 18500, 6.5),
('avg_salary', '310000', 2024, 6, 19200, 7.2),
('avg_salary', '330100', 2024, 6, 15800, 9.5),
('avg_salary', '440300', 2024, 6, 16800, 8.2),
('avg_salary', '440100', 2024, 6, 14200, 5.8);
