/*
 Navicat Premium Data Transfer
 Source Schema         : metric_analyst_agent

 Target Server Type    : MySQL
 Target Server Version : 50730
 File Encoding         : 65001

 Date: 19/03/2026 18:39:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for db_data_dimension
-- ----------------------------
DROP TABLE IF EXISTS `db_data_dimension`;
CREATE TABLE `db_data_dimension`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `table_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据表标识',
  `dimension_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '维度标识',
  `dimension_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '维度名称',
  `dimension_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据表维度字段',
  `is_common` tinyint(1) NULL DEFAULT 0 COMMENT '是否公共维度',
  `is_required` tinyint(1) NULL DEFAULT 0 COMMENT '是否必填',
  `default_value` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '默认值',
  `dimension_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型：temporal/categorical/numerical',
  `sort_order` int(11) NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_table`(`table_id`) USING BTREE,
  INDEX `idx_dimension`(`dimension_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 66 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据维度关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of db_data_dimension
-- ----------------------------
INSERT INTO `db_data_dimension` VALUES (1, 'ads_rpa_w_icn_edu_recruit_position_num_m', 'time', '时间', 'time_id', 1, 1, NULL, 'temporal', 1);
INSERT INTO `db_data_dimension` VALUES (2, 'ads_rpa_w_icn_edu_recruit_position_num_m', 'region', '地区', 'region_id', 1, 0, '100000', 'categorical', 2);
INSERT INTO `db_data_dimension` VALUES (3, 'ads_rpa_w_icn_edu_recruit_position_num_m', 'education', '学历', 'education_id', 1, 0, 'TOTAL', 'categorical', 3);
INSERT INTO `db_data_dimension` VALUES (4, 'ads_rpa_w_icn_edu_recruit_position_num_m', 'data_attr', '数据属性', 'data_attr_id', 1, 0, 'DAT_1', 'categorical', 4);
INSERT INTO `db_data_dimension` VALUES (5, 'ads_rpa_w_icn_recruit_company_num_m', 'time', '时间', 'time_id', 1, 1, NULL, 'temporal', 1);
INSERT INTO `db_data_dimension` VALUES (6, 'ads_rpa_w_icn_recruit_company_num_m', 'region', '地区', 'region_id', 1, 0, '100000', 'categorical', 2);
INSERT INTO `db_data_dimension` VALUES (7, 'ads_rpa_w_icn_recruit_company_num_m', 'data_attr', '数据属性', 'data_attr_id', 1, 0, 'DAT_1', 'categorical', 3);
INSERT INTO `db_data_dimension` VALUES (8, 'ads_rpa_w_icn_recruit_salary_amount_m', 'time', '时间', 'time_id', 1, 1, NULL, 'temporal', 1);
INSERT INTO `db_data_dimension` VALUES (9, 'ads_rpa_w_icn_recruit_salary_amount_m', 'region', '地区', 'region_id', 1, 0, '100000', 'categorical', 2);
INSERT INTO `db_data_dimension` VALUES (10, 'ads_rpa_w_icn_recruit_salary_amount_m', 'data_attr', '数据属性', 'data_attr_id', 1, 0, 'DAT_1', 'categorical', 3);
INSERT INTO `db_data_dimension` VALUES (11, 'ads_rpa_w_icn_eco_spe_company_add_num_m', 'time', '时间', 'time_id', 1, 1, NULL, 'temporal', 1);
INSERT INTO `db_data_dimension` VALUES (12, 'ads_rpa_w_icn_eco_spe_company_add_num_m', 'region', '地区', 'region_id', 1, 0, '100000', 'categorical', 2);
INSERT INTO `db_data_dimension` VALUES (13, 'ads_rpa_w_icn_eco_spe_company_add_num_m', 'economic_type', '经济类型', 'economic_type_id', 0, 0, 'TOTAL', 'categorical', 3);
INSERT INTO `db_data_dimension` VALUES (14, 'ads_rpa_w_icn_eco_spe_company_add_num_m', 'spe_tag', '特殊资质', 'spe_tag_id', 0, 0, 'TOTAL', 'categorical', 4);
INSERT INTO `db_data_dimension` VALUES (15, 'ads_rpa_w_icn_eco_spe_company_add_num_m', 'data_attr', '数据属性', 'data_attr_id', 1, 0, 'DAT_1', 'categorical', 5);
INSERT INTO `db_data_dimension` VALUES (16, 'ads_rpa_w_icn_eco_spe_company_cancel_num_m', 'time', '时间', 'time_id', 1, 1, NULL, 'temporal', 1);
INSERT INTO `db_data_dimension` VALUES (17, 'ads_rpa_w_icn_eco_spe_company_cancel_num_m', 'region', '地区', 'region_id', 1, 0, '100000', 'categorical', 2);
INSERT INTO `db_data_dimension` VALUES (18, 'ads_rpa_w_icn_eco_spe_company_cancel_num_m', 'economic_type', '经济类型', 'economic_type_id', 0, 0, 'TOTAL', 'categorical', 3);
INSERT INTO `db_data_dimension` VALUES (19, 'ads_rpa_w_icn_eco_spe_company_cancel_num_m', 'spe_tag', '特殊资质', 'spe_tag_id', 0, 0, 'TOTAL', 'categorical', 4);
INSERT INTO `db_data_dimension` VALUES (20, 'ads_rpa_w_icn_eco_spe_company_cancel_num_m', 'data_attr', '数据属性', 'data_attr_id', 1, 0, 'DAT_1', 'categorical', 5);
INSERT INTO `db_data_dimension` VALUES (21, 'ads_rpa_w_icn_eco_spe_company_on_num_m', 'time', '时间', 'time_id', 1, 1, NULL, 'temporal', 1);
INSERT INTO `db_data_dimension` VALUES (22, 'ads_rpa_w_icn_eco_spe_company_on_num_m', 'region', '地区', 'region_id', 1, 0, '100000', 'categorical', 2);
INSERT INTO `db_data_dimension` VALUES (23, 'ads_rpa_w_icn_eco_spe_company_on_num_m', 'economic_type', '经济类型', 'economic_type_id', 0, 0, 'TOTAL', 'categorical', 3);
INSERT INTO `db_data_dimension` VALUES (24, 'ads_rpa_w_icn_eco_spe_company_on_num_m', 'spe_tag', '特殊资质', 'spe_tag_id', 0, 0, 'TOTAL', 'categorical', 4);
INSERT INTO `db_data_dimension` VALUES (25, 'ads_rpa_w_icn_eco_spe_company_on_num_m', 'data_attr', '数据属性', 'data_attr_id', 1, 0, 'DAT_1', 'categorical', 5);
INSERT INTO `db_data_dimension` VALUES (26, 'ads_rpa_w_icn_typ_com_patent_application_num_m', 'time', '时间', 'time_id', 1, 1, NULL, 'temporal', 1);
INSERT INTO `db_data_dimension` VALUES (27, 'ads_rpa_w_icn_typ_com_patent_application_num_m', 'region', '地区', 'region_id', 1, 0, '100000', 'categorical', 2);
INSERT INTO `db_data_dimension` VALUES (28, 'ads_rpa_w_icn_typ_com_patent_application_num_m', 'patent_type', '专利类型', 'patent_type_id', 0, 0, 'TOTAL', 'categorical', 3);
INSERT INTO `db_data_dimension` VALUES (29, 'ads_rpa_w_icn_typ_com_patent_application_num_m', 'company_type', '申请人类型', 'company_type_id', 0, 0, 'TOTAL', 'categorical', 4);
INSERT INTO `db_data_dimension` VALUES (30, 'ads_rpa_w_icn_typ_com_patent_application_num_m', 'data_attr', '数据属性', 'data_attr_id', 1, 0, 'DAT_1', 'categorical', 5);
INSERT INTO `db_data_dimension` VALUES (31, 'ads_rpa_w_icn_sce_pri_government_procurement_amount_m', 'time', '时间', 'time_id', 1, 1, NULL, 'temporal', 1);
INSERT INTO `db_data_dimension` VALUES (32, 'ads_rpa_w_icn_sce_pri_government_procurement_amount_m', 'region', '地区', 'region_id', 1, 0, '100000', 'categorical', 2);
INSERT INTO `db_data_dimension` VALUES (33, 'ads_rpa_w_icn_sce_pri_government_procurement_amount_m', 'price_range', '价格区间', 'price_range_id', 0, 0, 'TOTAL', 'categorical', 3);
INSERT INTO `db_data_dimension` VALUES (34, 'ads_rpa_w_icn_sce_pri_government_procurement_amount_m', 'scene_type', '场景类型', 'scene_type_id', 0, 0, 'TOTAL', 'categorical', 4);
INSERT INTO `db_data_dimension` VALUES (35, 'ads_rpa_w_icn_sce_pri_government_procurement_amount_m', 'data_attr', '数据属性', 'data_attr_id', 1, 0, 'DAT_1', 'categorical', 5);
INSERT INTO `db_data_dimension` VALUES (36, 'ads_rpa_w_icn_sce_pri_government_procurement_num_m', 'time', '时间', 'time_id', 1, 1, NULL, 'temporal', 1);
INSERT INTO `db_data_dimension` VALUES (37, 'ads_rpa_w_icn_sce_pri_government_procurement_num_m', 'region', '地区', 'region_id', 1, 0, '100000', 'categorical', 2);
INSERT INTO `db_data_dimension` VALUES (38, 'ads_rpa_w_icn_sce_pri_government_procurement_num_m', 'price_range', '价格区间', 'price_range_id', 0, 0, 'TOTAL', 'categorical', 3);
INSERT INTO `db_data_dimension` VALUES (39, 'ads_rpa_w_icn_sce_pri_government_procurement_num_m', 'scene_type', '场景类型', 'scene_type_id', 0, 0, 'TOTAL', 'categorical', 4);
INSERT INTO `db_data_dimension` VALUES (40, 'ads_rpa_w_icn_sce_pri_government_procurement_num_m', 'data_attr', '数据属性', 'data_attr_id', 1, 0, 'DAT_1', 'categorical', 5);
INSERT INTO `db_data_dimension` VALUES (41, 'ads_rpa_w_icn_sce_pri_government_procurement_avg_m', 'time', '时间', 'time_id', 1, 1, NULL, 'temporal', 1);
INSERT INTO `db_data_dimension` VALUES (42, 'ads_rpa_w_icn_sce_pri_government_procurement_avg_m', 'region', '地区', 'region_id', 1, 0, '100000', 'categorical', 2);
INSERT INTO `db_data_dimension` VALUES (43, 'ads_rpa_w_icn_sce_pri_government_procurement_avg_m', 'price_range', '价格区间', 'price_range_id', 0, 0, 'TOTAL', 'categorical', 3);
INSERT INTO `db_data_dimension` VALUES (44, 'ads_rpa_w_icn_sce_pri_government_procurement_avg_m', 'scene_type', '场景类型', 'scene_type_id', 0, 0, 'TOTAL', 'categorical', 4);
INSERT INTO `db_data_dimension` VALUES (45, 'ads_rpa_w_icn_sce_pri_government_procurement_avg_m', 'data_attr', '数据属性', 'data_attr_id', 1, 0, 'DAT_1', 'categorical', 5);
INSERT INTO `db_data_dimension` VALUES (46, 'ads_rpa_w_icn_edu_recruit_position_num_m', 'icn_chain_area', '产业链领域', 'icn_chain_area_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 5);
INSERT INTO `db_data_dimension` VALUES (47, 'ads_rpa_w_icn_edu_recruit_position_num_m', 'icn_chain_link', '产业链环节', 'icn_chain_link_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 6);
INSERT INTO `db_data_dimension` VALUES (48, 'ads_rpa_w_icn_recruit_company_num_m', 'icn_chain_area', '产业链领域', 'icn_chain_area_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 5);
INSERT INTO `db_data_dimension` VALUES (49, 'ads_rpa_w_icn_recruit_company_num_m', 'icn_chain_link', '产业链环节', 'icn_chain_link_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 6);
INSERT INTO `db_data_dimension` VALUES (50, 'ads_rpa_w_icn_recruit_salary_amount_m', 'icn_chain_area', '产业链领域', 'icn_chain_area_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 5);
INSERT INTO `db_data_dimension` VALUES (51, 'ads_rpa_w_icn_recruit_salary_amount_m', 'icn_chain_link', '产业链环节', 'icn_chain_link_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 6);
INSERT INTO `db_data_dimension` VALUES (52, 'ads_rpa_w_icn_eco_spe_company_add_num_m', 'icn_chain_area', '产业链领域', 'icn_chain_area_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 6);
INSERT INTO `db_data_dimension` VALUES (53, 'ads_rpa_w_icn_eco_spe_company_add_num_m', 'icn_chain_link', '产业链环节', 'icn_chain_link_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 7);
INSERT INTO `db_data_dimension` VALUES (54, 'ads_rpa_w_icn_eco_spe_company_cancel_num_m', 'icn_chain_area', '产业链领域', 'icn_chain_area_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 6);
INSERT INTO `db_data_dimension` VALUES (55, 'ads_rpa_w_icn_eco_spe_company_cancel_num_m', 'icn_chain_link', '产业链环节', 'icn_chain_link_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 7);
INSERT INTO `db_data_dimension` VALUES (56, 'ads_rpa_w_icn_eco_spe_company_on_num_m', 'icn_chain_area', '产业链领域', 'icn_chain_area_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 6);
INSERT INTO `db_data_dimension` VALUES (57, 'ads_rpa_w_icn_eco_spe_company_on_num_m', 'icn_chain_link', '产业链环节', 'icn_chain_link_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 7);
INSERT INTO `db_data_dimension` VALUES (58, 'ads_rpa_w_icn_typ_com_patent_application_num_m', 'icn_chain_area', '产业链领域', 'icn_chain_area_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 6);
INSERT INTO `db_data_dimension` VALUES (59, 'ads_rpa_w_icn_typ_com_patent_application_num_m', 'icn_chain_link', '产业链环节', 'icn_chain_link_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 7);
INSERT INTO `db_data_dimension` VALUES (60, 'ads_rpa_w_icn_sce_pri_government_procurement_amount_m', 'icn_chain_area', '产业链领域', 'icn_chain_area_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 6);
INSERT INTO `db_data_dimension` VALUES (61, 'ads_rpa_w_icn_sce_pri_government_procurement_amount_m', 'icn_chain_link', '产业链环节', 'icn_chain_link_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 7);
INSERT INTO `db_data_dimension` VALUES (62, 'ads_rpa_w_icn_sce_pri_government_procurement_num_m', 'icn_chain_area', '产业链领域', 'icn_chain_area_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 6);
INSERT INTO `db_data_dimension` VALUES (63, 'ads_rpa_w_icn_sce_pri_government_procurement_num_m', 'icn_chain_link', '产业链环节', 'icn_chain_link_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 7);
INSERT INTO `db_data_dimension` VALUES (64, 'ads_rpa_w_icn_sce_pri_government_procurement_avg_m', 'icn_chain_area', '产业链领域', 'icn_chain_area_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 6);
INSERT INTO `db_data_dimension` VALUES (65, 'ads_rpa_w_icn_sce_pri_government_procurement_avg_m', 'icn_chain_link', '产业链环节', 'icn_chain_link_id', 1, 0, 'ICN_CHAIN_6', 'categorical', 7);

-- ----------------------------
-- Table structure for db_data_source
-- ----------------------------
DROP TABLE IF EXISTS `db_data_source`;
CREATE TABLE `db_data_source`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `source_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源标识',
  `source_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源名称',
  `source_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型：mysql/kylin/api',
  `host` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主机地址',
  `port` int(11) NULL DEFAULT NULL COMMENT '端口',
  `database_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据库名',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码',
  `connection_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '连接参数JSON',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `source_id`(`source_id`) USING BTREE,
  INDEX `idx_type`(`source_type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据源配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of db_data_source
-- ----------------------------
INSERT INTO `db_data_source` VALUES (1, 'mysql_prod', '生产MySQL', 'mysql', 'localhost', 3306, '', '', '', NULL, 1, '2026-03-18 10:52:11', '2026-03-19 18:36:29');

-- ----------------------------
-- Table structure for db_data_table
-- ----------------------------
DROP TABLE IF EXISTS `db_data_table`;
CREATE TABLE `db_data_table`  (
  `table_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表唯一标识',
  `table_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '物理表名',
  `table_alias` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表别名/中文名',
  `source_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据源ID',
  `database_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据库名',
  `schema_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Schema名',
  `table_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'fact' COMMENT '类型：fact(事实表)/dim(维度表)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表业务描述',
  `time_column` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'time_id' COMMENT '时间维度字段名',
  `region_column` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'region_id' COMMENT '地区维度字段名',
  `value_column` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'fact_value' COMMENT '数值字段名',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`table_id`) USING BTREE,
  INDEX `idx_source`(`source_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据表登记' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of db_data_table
-- ----------------------------
INSERT INTO `db_data_table` VALUES ('ads_rpa_w_icn_eco_spe_company_add_num_m', 'ads_rpa_w_icn_eco_spe_company_add_num_m', '新增企业数量', 'mysql_prod', 'metric_db', NULL, 'fact', '新增企业数量统计表', 'time_id', 'region_id', 'fact_value', 1, '2026-03-18 10:52:11', '2026-03-18 10:52:11');
INSERT INTO `db_data_table` VALUES ('ads_rpa_w_icn_eco_spe_company_cancel_num_m', 'ads_rpa_w_icn_eco_spe_company_cancel_num_m', '注销企业数量', 'mysql_prod', 'metric_db', NULL, 'fact', '注销企业数量统计表', 'time_id', 'region_id', 'fact_value', 1, '2026-03-18 10:52:11', '2026-03-18 10:52:11');
INSERT INTO `db_data_table` VALUES ('ads_rpa_w_icn_eco_spe_company_on_num_m', 'ads_rpa_w_icn_eco_spe_company_on_num_m', '在营企业数量', 'mysql_prod', 'metric_db', NULL, 'fact', '在营企业数量统计表', 'time_id', 'region_id', 'fact_value', 1, '2026-03-18 10:52:11', '2026-03-18 10:52:11');
INSERT INTO `db_data_table` VALUES ('ads_rpa_w_icn_edu_recruit_position_num_m', 'ads_rpa_w_icn_edu_recruit_position_num_m', '招聘岗位数量', 'mysql_prod', 'metric_db', NULL, 'fact', '招聘岗位数量统计表', 'time_id', 'region_id', 'fact_value', 1, '2026-03-18 10:52:11', '2026-03-18 10:52:11');
INSERT INTO `db_data_table` VALUES ('ads_rpa_w_icn_recruit_company_num_m', 'ads_rpa_w_icn_recruit_company_num_m', '招聘市场主体数量', 'mysql_prod', 'metric_db', NULL, 'fact', '招聘市场主体数量统计表', 'time_id', 'region_id', 'fact_value', 1, '2026-03-18 10:52:11', '2026-03-18 10:52:11');
INSERT INTO `db_data_table` VALUES ('ads_rpa_w_icn_recruit_salary_amount_m', 'ads_rpa_w_icn_recruit_salary_amount_m', '招聘岗位平均薪酬', 'mysql_prod', 'metric_db', NULL, 'fact', '招聘岗位平均薪酬统计表', 'time_id', 'region_id', 'fact_value', 1, '2026-03-18 10:52:11', '2026-03-18 10:52:11');
INSERT INTO `db_data_table` VALUES ('ads_rpa_w_icn_sce_pri_government_procurement_amount_m', 'ads_rpa_w_icn_sce_pri_government_procurement_amount_m', '政府采购金额', 'mysql_prod', 'metric_db', NULL, 'fact', '政府采购金额统计表', 'time_id', 'region_id', 'fact_value', 1, '2026-03-18 10:52:11', '2026-03-18 10:52:11');
INSERT INTO `db_data_table` VALUES ('ads_rpa_w_icn_sce_pri_government_procurement_avg_m', 'ads_rpa_w_icn_sce_pri_government_procurement_avg_m', '政府采购平均价格', 'mysql_prod', 'metric_db', NULL, 'fact', '政府采购平均价格统计表', 'time_id', 'region_id', 'fact_value', 1, '2026-03-18 10:52:11', '2026-03-18 10:52:11');
INSERT INTO `db_data_table` VALUES ('ads_rpa_w_icn_sce_pri_government_procurement_num_m', 'ads_rpa_w_icn_sce_pri_government_procurement_num_m', '政府采购数量', 'mysql_prod', 'metric_db', NULL, 'fact', '政府采购数量统计表', 'time_id', 'region_id', 'fact_value', 1, '2026-03-18 10:52:11', '2026-03-18 10:52:11');
INSERT INTO `db_data_table` VALUES ('ads_rpa_w_icn_typ_com_patent_application_num_m', 'ads_rpa_w_icn_typ_com_patent_application_num_m', '专利申请数量', 'mysql_prod', 'metric_db', NULL, 'fact', '专利申请数量统计表', 'time_id', 'region_id', 'fact_value', 1, '2026-03-18 10:52:11', '2026-03-18 10:52:11');

-- ----------------------------
-- Table structure for db_indicator
-- ----------------------------
DROP TABLE IF EXISTS `db_indicator`;
CREATE TABLE `db_indicator`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `indicator_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '指标编码',
  `indicator_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '指标名称',
  `unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '单位',
  `frequency` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '频率：D/W/M/Q/Y',
  `valid_measures` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '有效度量',
  `table_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '对应数据表',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '备注',
  `domain` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '大领域分类',
  `subdomain` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '子领域分类',
  `tags` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标签，逗号分隔',
  `indexed` tinyint(1) NULL DEFAULT 0 COMMENT '是否已建立向量索引',
  `index_version` bigint(20) NULL DEFAULT 0 COMMENT '索引版本号',
  `last_indexed_at` datetime(0) NULL DEFAULT NULL COMMENT '最后索引时间',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_indicator_id`(`indicator_id`) USING BTREE,
  INDEX `idx_domain`(`domain`) USING BTREE,
  INDEX `idx_table_id`(`table_id`) USING BTREE,
  CONSTRAINT `fk_indicator_table_id` FOREIGN KEY (`table_id`) REFERENCES `db_data_table` (`table_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '指标元数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of db_indicator
-- ----------------------------
INSERT INTO `db_indicator` VALUES (1, 'I_RPA_ICN_RAE_POSITION_NUM', '招聘岗位数量', '个', 'M', '当期，当期同比', 'ads_rpa_w_icn_edu_recruit_position_num_m', '企业发布的招聘岗位数量，反映用工需求景气度', '招聘就业', '招聘需求', '招聘,岗位,职位,用工,就业,劳动力,人才,招工', 1, 1773831608642, '2026-03-18 19:00:09', '2026-03-18 10:52:11', '2026-03-18 19:00:09');
INSERT INTO `db_indicator` VALUES (2, 'I_RPA_ICN_RAE_COMPANY_NUM', '招聘市场主体数量', '个', 'M', '当期，当期同比', 'ads_rpa_w_icn_recruit_company_num_m', '参与招聘活动的企业主体数量，反映招聘活跃度', '招聘就业', '招聘主体', '招聘,企业,雇主,招聘方,招聘主体,公司', 1, 1773831608642, '2026-03-18 19:00:09', '2026-03-18 10:52:11', '2026-03-18 19:00:09');
INSERT INTO `db_indicator` VALUES (3, 'I_RPA_ICN_RAE_SALARY_AMOUNT', '招聘岗位平均薪酬', '元', 'M', '当期，当期同比', 'ads_rpa_w_icn_recruit_salary_amount_m', '招聘岗位的平均薪资水平，反映劳动力市场价格', '招聘就业', '薪资水平', '薪资,工资,薪酬,收入,待遇,报酬,月薪,年薪', 1, 1773831608642, '2026-03-18 19:00:09', '2026-03-18 10:52:11', '2026-03-18 19:00:09');
INSERT INTO `db_indicator` VALUES (4, 'I_RPA_ICN_MKE_COMPANY_ADD_NUM', '新增企业数量', '个', 'M', '当期，当期同比，去年同期值差额，当期环比，上期差额', 'ads_rpa_w_icn_eco_spe_company_add_num_m', '新注册登记的企业数量，反映市场创业活力', '市场主体', '企业增量', '新增企业,注册企业,新成立,创业,新设企业,市场活力', 1, 1773831608642, '2026-03-18 19:00:09', '2026-03-18 10:52:11', '2026-03-18 19:00:09');
INSERT INTO `db_indicator` VALUES (5, 'I_RPA_ICN_MKE_COMPANY_CANCEL_NUM', '注销企业数量', '个', 'M', '当期，当期同比，去年同期值差额，当期环比，上期差额', 'ads_rpa_w_icn_eco_spe_company_cancel_num_m', '注销登记的企业数量，反映市场退出情况', '市场主体', '企业减量', '注销企业,吊销,退出,倒闭,关停,歇业', 1, 1773831608642, '2026-03-18 19:00:09', '2026-03-18 10:52:11', '2026-03-18 19:00:09');
INSERT INTO `db_indicator` VALUES (6, 'I_RPA_ICN_MKE_COMPANY_ON_NUM', '在营企业数量', '个', 'M', '当期，当期同比，去年同期值差额，当期环比，上期差额', 'ads_rpa_w_icn_eco_spe_company_on_num_m', '处于在营状态的企业总量，反映市场主体规模', '市场主体', '企业存量', '在营企业,存续企业,运营企业,活跃企业,企业总数', 1, 1773831608642, '2026-03-18 19:00:09', '2026-03-18 10:52:11', '2026-03-18 19:00:09');
INSERT INTO `db_indicator` VALUES (7, 'I_RPA_ICN_PAT_APPLICATION_NUM', '专利申请数量', '个', 'M', '当期，当期同比，累计，累计同比', 'ads_rpa_w_icn_typ_com_patent_application_num_m', '提交的专利申请数量，反映创新活跃度', '知识产权', '专利创新', '专利,申请,知识产权,发明,创新,技术,研发', 1, 1773831608642, '2026-03-18 19:00:09', '2026-03-18 10:52:11', '2026-03-18 19:00:09');
INSERT INTO `db_indicator` VALUES (8, 'I_RPA_ICN_GVP_AMOUNT', '政府采购金额', '万元', 'M', '当期，当期同比，累计，累计同比', 'ads_rpa_w_icn_sce_pri_government_procurement_amount_m', '政府采购项目的成交金额，反映政府支出规模', '政府采购', '采购规模', '采购,政府采购,中标金额,成交金额,政府支出,招标', 1, 1773831608642, '2026-03-18 19:00:09', '2026-03-18 10:52:11', '2026-03-18 19:00:09');
INSERT INTO `db_indicator` VALUES (9, 'I_RPA_ICN_GVP_NUM', '政府采购数量', '个', 'M', '当期，当期同比，累计，累计同比', 'ads_rpa_w_icn_sce_pri_government_procurement_num_m', '政府采购项目的成交数量，反映采购活跃度', '政府采购', '采购频次', '采购,政府采购,中标,成交,招标数量,项目数', 1, 1773831608642, '2026-03-18 19:00:09', '2026-03-18 10:52:11', '2026-03-18 19:00:09');
INSERT INTO `db_indicator` VALUES (10, 'I_RPA_ICN_GVP_AMOUNT_AVG', '政府采购平均价格', '万元/个', 'M', '当期，当期同比，累计，累计同比', 'ads_rpa_w_icn_sce_pri_government_procurement_avg_m', '政府采购项目的平均成交价格，反映采购效率', '政府采购', '采购效率', '采购均价,平均价格,单价,平均中标,采购效率', 1, 1773831608642, '2026-03-18 19:00:09', '2026-03-18 10:52:11', '2026-03-18 19:00:09');

-- ----------------------------
-- Table structure for dimension_values
-- ----------------------------
DROP TABLE IF EXISTS `dimension_values`;
CREATE TABLE `dimension_values`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dimension_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '维度标识',
  `dimension_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '维度名称',
  `value_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '值编码',
  `value_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '值名称',
  `synonyms` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '同义词，逗号分隔',
  `parent_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '父级编码',
  `sort_order` int(11) NULL DEFAULT 0 COMMENT '排序',
  `indexed` tinyint(1) NULL DEFAULT 0 COMMENT '是否已索引',
  `created_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `updated_at` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dimension`(`dimension_id`) USING BTREE,
  INDEX `idx_value_code`(`value_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 129 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '维度值表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dimension_values
-- ----------------------------
INSERT INTO `dimension_values` VALUES (62, 'region', '地区', '100000', '全国', '中国,全国,全中国', NULL, 1, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (63, 'region', '地区', '110000', '北京市', '北京,帝都,京城', '100000', 2, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (64, 'region', '地区', '310000', '上海市', '上海,魔都,沪', '100000', 3, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (65, 'region', '地区', '440000', '广东省', '广东,粤,岭南', '100000', 4, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (66, 'region', '地区', '440100', '广州市', '广州,羊城,穗', '440000', 5, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (67, 'region', '地区', '440300', '深圳市', '深圳,鹏城,深', '440000', 6, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (68, 'region', '地区', '320000', '江苏省', '江苏,苏,吴越', '100000', 7, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (69, 'region', '地区', '320100', '南京市', '南京,金陵,宁', '320000', 8, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (70, 'region', '地区', '330000', '浙江省', '浙江,浙,钱塘', '100000', 9, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (71, 'region', '地区', '330100', '杭州市', '杭州,杭,钱塘', '330000', 10, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (72, 'education', '学历', 'TOTAL', '全部学历', '汇总,全部,所有', NULL, 0, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (73, 'education', '学历', 'RAE_EDU_1', '不限', '不限学历,学历不限', NULL, 1, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (74, 'education', '学历', 'RAE_EDU_2', '中专/中技', '中专,中技,中等专业学校,技工学校', NULL, 2, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (75, 'education', '学历', 'RAE_EDU_3', '初中', '初中,初级中学', NULL, 3, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (76, 'education', '学历', 'RAE_EDU_4', '博士', '博士,博士学位,PhD,博士生', NULL, 4, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (77, 'education', '学历', 'RAE_EDU_5', '大专', '大专,专科,大学专科,高职', NULL, 5, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (78, 'education', '学历', 'RAE_EDU_6', '本科', '本科,大学本科,本科及以上,学士,本科生', NULL, 6, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (79, 'education', '学历', 'RAE_EDU_7', '硕士', '硕士,硕士学位,研究生,硕士生', NULL, 7, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (80, 'education', '学历', 'RAE_EDU_8', '高中', '高中,高级中学,职高', NULL, 8, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (81, 'patent_type', '专利类型', 'TOTAL', '全部专利类型', '全部,所有,Total,All', NULL, 1, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (82, 'patent_type', '专利类型', 'PAT_PTT_0', '外观设计', '外观,外观设计专利,Design', NULL, 2, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (83, 'patent_type', '专利类型', 'PAT_PTT_1', '实用新型', '实用新型,新型,Utility Model', NULL, 3, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (84, 'patent_type', '专利类型', 'PAT_PTT_2', '发明专利', '发明,发明专利,Invention', NULL, 4, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (85, 'company_type', '申请人类型', 'TOTAL', '全部申请人类型汇总', '全部,所有', NULL, 0, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (86, 'company_type', '申请人类型', 'RPA_CTP_1', '企业', '企业,公司,Enterprise', NULL, 1, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (87, 'company_type', '申请人类型', 'RPA_CTP_2', '研究机构', '研究机构,研究院,研究所', NULL, 2, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (88, 'company_type', '申请人类型', 'RPA_CTP_3', '研究机构_企业', '产学研,校企合作,联合申请', NULL, 3, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (89, 'company_type', '申请人类型', 'RPA_CTP_4', '其他', '其他,个人,Other', NULL, 4, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (90, 'economic_type', '经济类型', 'TOTAL', '全部类型', '全部,所有', NULL, 0, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (91, 'economic_type', '经济类型', 'MKE_ECO_1', '国有', '国有,国有企业,国企,国有经济', NULL, 1, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (92, 'economic_type', '经济类型', 'MKE_ECO_2', '集体', '集体,集体企业,集体经济', NULL, 2, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (93, 'economic_type', '经济类型', 'MKE_ECO_3', '私营', '私营,私营企业,民企,民营经济', NULL, 3, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (94, 'economic_type', '经济类型', 'MKE_ECO_4', '个体', '个体,个体户,个体工商户', NULL, 4, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (95, 'economic_type', '经济类型', 'MKE_ECO_5', '合资', '合资,合资企业,中外合资', NULL, 5, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (96, 'economic_type', '经济类型', 'MKE_ECO_6', '股份', '股份,股份制,股份有限公司', NULL, 6, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (97, 'economic_type', '经济类型', 'MKE_ECO_7', '外资', '外资,外资企业,外企,外商投资', NULL, 7, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (98, 'economic_type', '经济类型', 'MKE_ECO_8', '港澳台资', '港澳台资,港澳台企业,港资,台资', NULL, 8, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (99, 'economic_type', '经济类型', 'MKE_ECO_9', '其他', '其他,其他类型', NULL, 9, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (100, 'economic_type', '经济类型', 'MKE_ECO_10', '联营', '联营,联营企业', NULL, 10, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (101, 'spe_tag', '特殊资质', 'TOTAL', '全部资质', '全部,所有', NULL, 0, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (102, 'spe_tag', '特殊资质', 'MKE_SPE_1', '专精特新小巨人', '小巨人,专精特新小巨人企业,国家级小巨人', NULL, 1, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (103, 'spe_tag', '特殊资质', 'MKE_SPE_2', '专精特新企业', '专精特新,省级专精特新', NULL, 2, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (104, 'spe_tag', '特殊资质', 'MKE_SPE_3', '科技小巨人企业', '科技小巨人,科技小巨人企业', NULL, 3, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (105, 'spe_tag', '特殊资质', 'MKE_SPE_4', '高新技术企业', '高新,高新技术企业,高企', NULL, 4, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (106, 'spe_tag', '特殊资质', 'MKE_SPE_5', '隐形冠军企业', '隐形冠军', NULL, 5, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (107, 'spe_tag', '特殊资质', 'MKE_SPE_6', '制造业单项冠军', '单项冠军,制造业单项冠军企业', NULL, 6, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (108, 'spe_tag', '特殊资质', 'MKE_SPE_7', '瞪羚企业', '瞪羚,高成长企业', NULL, 7, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (109, 'spe_tag', '特殊资质', 'MKE_SPE_8', '雏鹰企业', '雏鹰,初创企业', NULL, 8, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (110, 'price_range', '价格区间', 'TOTAL', '全部价格', '全部,所有', NULL, 0, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (111, 'price_range', '价格区间', 'RPA_PRI_1', '0-20万元', '0-20万,20万以下,小额采购', NULL, 1, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (112, 'price_range', '价格区间', 'RPA_PRI_2', '20-100万元', '20-100万,中小额', NULL, 2, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (113, 'price_range', '价格区间', 'RPA_PRI_3', '100-200万元', '100-200万,中额', NULL, 3, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (114, 'price_range', '价格区间', 'RPA_PRI_5', '200-500万元', '200-500万,大额', NULL, 4, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (115, 'price_range', '价格区间', 'RPA_PRI_6', '500万元以上', '500万以上,超大规模,大额采购', NULL, 5, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (116, 'scene_type', '场景类型', 'TOTAL', '全部场景', '全部,所有场景,所有', NULL, 0, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (117, 'scene_type', '场景类型', 'RPA_SCE_1', '环境保护', '环保,环境治理,生态保护', NULL, 1, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (118, 'scene_type', '场景类型', 'RPA_SCE_2', '教育科研', '教育,科研,教学研究', NULL, 2, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (119, 'scene_type', '场景类型', 'RPA_SCE_3', '应急管理', '应急,灾害,救援', NULL, 3, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (120, 'scene_type', '场景类型', 'RPA_SCE_4', '农业水利', '农业,水利,农林牧渔', NULL, 4, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (121, 'scene_type', '场景类型', 'RPA_SCE_5', '交通运输', '交通,运输,物流', NULL, 5, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (122, 'scene_type', '场景类型', 'RPA_SCE_6', '政府服务', '政务,行政,公共服务', NULL, 6, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (123, 'scene_type', '场景类型', 'RPA_SCE_7', '地质勘探', '地质,勘探,矿产', NULL, 7, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (124, 'scene_type', '场景类型', 'RPA_SCE_8', '文化旅游', '文旅,文化,旅游,娱乐', NULL, 8, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (125, 'scene_type', '场景类型', 'RPA_SCE_9', '其他', '其他场景,其他行业', NULL, 9, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (126, 'scene_type', '场景类型', 'RPA_SCE_10', '公共卫生', '卫生,医疗,公共健康', NULL, 10, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (127, 'data_attr', '数据属性', 'DAT_1', '当期', '当月,当期值,本月', NULL, 1, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');
INSERT INTO `dimension_values` VALUES (128, 'data_attr', '数据属性', 'DAT_2', '累计', '累计值,本年累计,年度累计', NULL, 2, 1, '2026-03-18 18:59:15', '2026-03-18 19:00:13');

SET FOREIGN_KEY_CHECKS = 1;
