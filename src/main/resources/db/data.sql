
-- 大区数据
INSERT INTO sa_sales_region (id, name) VALUES
    (1, '华东区'),
    (2, '华南区'),
    (3, '华北区'),
    (4, '西南区')
ON DUPLICATE KEY UPDATE
    name = VALUES(name);

-- 本地初始化账号：每区 1 名主管、2 名销售员，另有 1 名全国总监。
-- password_hash 保存本地测试口令的 BCrypt 摘要，原始口令见 README。
INSERT INTO sa_sales_rep
    (id, name, login_name, password_hash, region_id, role, email)
VALUES
-- 华东区
(1,  '李明', 'east_manager',  '$2a$10$fitA7TSZjW2r0sLhVNb1ZeLqP0fGODUX21oENJ/0KL9wdQqxpPajm', 1, 'SALES_MANAGER',  'liming@jichi.com'),
(2,  '张伟', 'east_zhangwei', '$2a$10$fitA7TSZjW2r0sLhVNb1ZeLqP0fGODUX21oENJ/0KL9wdQqxpPajm', 1, 'SALES_REP',      'zhangwei@jichi.com'),
(3,  '王芳', 'east_wangfang', '$2a$10$fitA7TSZjW2r0sLhVNb1ZeLqP0fGODUX21oENJ/0KL9wdQqxpPajm', 1, 'SALES_REP',      'wangfang@jichi.com'),
-- 华南区
(4,  '陈强', 'south_manager', '$2a$10$fitA7TSZjW2r0sLhVNb1ZeLqP0fGODUX21oENJ/0KL9wdQqxpPajm', 2, 'SALES_MANAGER',  'chenqiang@jichi.com'),
(5,  '刘洋', 'south_liuyang', '$2a$10$fitA7TSZjW2r0sLhVNb1ZeLqP0fGODUX21oENJ/0KL9wdQqxpPajm', 2, 'SALES_REP',      'liuyang@jichi.com'),
(6,  '赵雪', 'south_zhaoxue', '$2a$10$fitA7TSZjW2r0sLhVNb1ZeLqP0fGODUX21oENJ/0KL9wdQqxpPajm', 2, 'SALES_REP',      'zhaoxue@jichi.com'),
-- 华北区
(7,  '孙磊', 'north_manager', '$2a$10$fitA7TSZjW2r0sLhVNb1ZeLqP0fGODUX21oENJ/0KL9wdQqxpPajm', 3, 'SALES_MANAGER',  'sunlei@jichi.com'),
(8,  '张磊', 'north_zhanglei','$2a$10$fitA7TSZjW2r0sLhVNb1ZeLqP0fGODUX21oENJ/0KL9wdQqxpPajm', 3, 'SALES_REP',      'zhanglei@jichi.com'),
(9,  '周丽', 'north_zhouli',  '$2a$10$fitA7TSZjW2r0sLhVNb1ZeLqP0fGODUX21oENJ/0KL9wdQqxpPajm', 3, 'SALES_REP',      'zhouli@jichi.com'),
-- 西南区
(10, '吴刚', 'west_manager',  '$2a$10$fitA7TSZjW2r0sLhVNb1ZeLqP0fGODUX21oENJ/0KL9wdQqxpPajm', 4, 'SALES_MANAGER',  'wugang@jichi.com'),
(11, '郑华', 'west_zhenghua', '$2a$10$fitA7TSZjW2r0sLhVNb1ZeLqP0fGODUX21oENJ/0KL9wdQqxpPajm', 4, 'SALES_REP',      'zhenghua@jichi.com'),
(12, '林敏', 'west_linmin',   '$2a$10$fitA7TSZjW2r0sLhVNb1ZeLqP0fGODUX21oENJ/0KL9wdQqxpPajm', 4, 'SALES_REP',      'linmin@jichi.com'),
-- 总监（全国）
(13, '黄总', 'sales_director','$2a$10$fitA7TSZjW2r0sLhVNb1ZeLqP0fGODUX21oENJ/0KL9wdQqxpPajm', 1, 'SALES_DIRECTOR', 'huang@jichi.com')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    login_name = VALUES(login_name),
    region_id = VALUES(region_id),
    role = VALUES(role),
    email = VALUES(email);


-- 产品数据
-- 产品（4 个品类，20 个 SKU）
INSERT INTO sa_product (id, sku_code, name, category, unit_price, cost, status) VALUES
-- 数码产品（高客单价）
(1,  'SKU-1001', '华为 Mate 60 Pro 手机',    '数码产品', 6999.00, 4200.00, 'ACTIVE'),
(2,  'SKU-1002', '苹果 iPhone 15 手机',       '数码产品', 7999.00, 5100.00, 'ACTIVE'),
(3,  'SKU-1003', '联想 ThinkPad X1 笔记本',  '数码产品', 9999.00, 6800.00, 'ACTIVE'),
(4,  'SKU-1004', '索尼 WH-1000XM5 耳机',     '数码产品', 2299.00, 1100.00, 'ACTIVE'),
(5,  'SKU-1005', '小米 14 Ultra 手机',        '数码产品', 5999.00, 3600.00, 'ACTIVE'),
-- 数码产品（包含用于零销售预警的 SKU）
(6,  'SKU-8821', '智能手表 Pro',              '数码产品', 1299.00,  650.00, 'ACTIVE'),
-- 家用电器（中高客单价）
(7,  'SKU-2001', '戴森 V15 吸尘器',           '家用电器', 4990.00, 2800.00, 'ACTIVE'),
(8,  'SKU-2002', '西门子洗碗机',              '家用电器', 5999.00, 3500.00, 'ACTIVE'),
(9,  'SKU-2003', '美的空调 1.5P',             '家用电器', 3299.00, 1900.00, 'ACTIVE'),
(10, 'SKU-2004', '苏泊尔电饭煲',              '家用电器',  599.00,  280.00, 'ACTIVE'),
(11, 'SKU-2005', '飞利浦空气净化器',          '家用电器', 2199.00, 1200.00, 'ACTIVE'),
-- 服装配饰（低客单价、销量较大）
(12, 'SKU-3001', '耐克 Air Max 运动鞋',       '服装配饰',  899.00,  420.00, 'ACTIVE'),
(13, 'SKU-3002', '优衣库羊绒大衣',            '服装配饰',  799.00,  350.00, 'ACTIVE'),
(14, 'SKU-3003', '阿迪达斯运动套装',          '服装配饰',  699.00,  310.00, 'ACTIVE'),
(15, 'SKU-3004', '蔻驰女包',                  '服装配饰', 2599.00, 1100.00, 'ACTIVE'),
-- 其他
(16, 'SKU-4001', '得力文具套装',              '其他',       99.00,   40.00, 'ACTIVE'),
(17, 'SKU-4002', '金融理财书籍套装',          '其他',      299.00,  120.00, 'ACTIVE'),
(18, 'SKU-4003', '瑜伽垫专业版',              '其他',      399.00,  160.00, 'ACTIVE'),
(19, 'SKU-4004', '咖啡机胶囊套装',            '其他',      699.00,  280.00, 'ACTIVE'),
(20, 'SKU-4005', '护肤品礼盒',                '其他',      899.00,  350.00, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    sku_code = VALUES(sku_code),
    name = VALUES(name),
    category = VALUES(category),
    unit_price = VALUES(unit_price),
    cost = VALUES(cost),
    status = VALUES(status);


-- 订单数据
-- 使用固定 order_no + INSERT IGNORE，保留已有订单并避免重复启动时重复插入。
-- 时间分布覆盖 7 个月、4 个大区、完成/退款/取消状态，以及增长、下滑、零销售场景。
INSERT IGNORE INTO sa_sales_order
    (order_no, rep_id, product_id, region_id, customer_name,
     quantity, unit_price, amount, cost, profit, status, order_date)
VALUES

-- B01：约 7 个月前（正常基线，四个大区均有订单）
('ORD-B01-001', 2,  1,  1, '上海云帆科技有限公司',       2, 6999.00, 13998.00,  8400.00,  5598.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 205 DAY)),
('ORD-B01-002', 3, 12,  1, '南京锐动体育用品有限公司',  10,  899.00,  8990.00,  4200.00,  4790.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 202 DAY)),
('ORD-B01-003', 5,  7,  2, '广州臻选家电有限公司',        3, 4990.00, 14970.00,  8400.00,  6570.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 200 DAY)),
('ORD-B01-004', 6, 13,  2, '深圳优品服饰有限公司',       15,  799.00, 11985.00,  5250.00,  6735.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 198 DAY)),
('ORD-B01-005', 8,  3,  3, '北京中科智联有限公司',        4, 9999.00, 39996.00, 27200.00, 12796.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 196 DAY)),
('ORD-B01-006', 9, 10,  3, '天津海河商贸有限公司',       20,  599.00, 11980.00,  5600.00,  6380.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 193 DAY)),
('ORD-B01-007',11,  8,  4, '成都安居电器有限公司',        3, 5999.00, 17997.00, 10500.00,  7497.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 190 DAY)),
('ORD-B01-008',12, 18,  4, '昆明悦动健身有限公司',       18,  399.00,  7182.00,  2880.00,  4302.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 187 DAY)),
('ORD-B01-009', 2,  6,  1, '苏州星链数码有限公司',       16, 1299.00, 20784.00, 10400.00, 10384.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 184 DAY)),
('ORD-B01-010', 5, 20,  2, '佛山悦己美妆有限公司',        8,  899.00,  7192.00,  2800.00,  4392.00, 'REFUNDED',  DATE_SUB(CURDATE(), INTERVAL 181 DAY)),

-- B02：约 6 个月前（整体平稳）
('ORD-B02-001', 2,  2,  1, '杭州澄海信息技术有限公司',    3, 7999.00, 23997.00, 15300.00,  8697.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 170 DAY)),
('ORD-B02-002', 3, 15,  1, '上海尚品供应链有限公司',       5, 2599.00, 12995.00,  5500.00,  7495.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 166 DAY)),
('ORD-B02-003', 5,  9,  2, '广州南粤工程服务有限公司',    10, 3299.00, 32990.00, 19000.00, 13990.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 164 DAY)),
('ORD-B02-004', 6, 16,  2, '深圳文创办公用品有限公司',    60,   99.00,  5940.00,  2400.00,  3540.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 160 DAY)),
('ORD-B02-005', 8,  5,  3, '北京北辰数码有限公司',         6, 5999.00, 35994.00, 21600.00, 14394.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 158 DAY)),
('ORD-B02-006', 9, 11,  3, '石家庄清新环境科技有限公司',   8, 2199.00, 17592.00,  9600.00,  7992.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 154 DAY)),
('ORD-B02-007',11, 14,  4, '重庆拓步体育有限公司',         20,  699.00, 13980.00,  6200.00,  7780.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 151 DAY)),
('ORD-B02-008',12, 19,  4, '贵阳醇享咖啡有限公司',         15,  699.00, 10485.00,  4200.00,  6285.00, 'CANCELLED', DATE_SUB(CURDATE(), INTERVAL 148 DAY)),


-- B03：约 5 个月前（旺季高峰）
('ORD-B03-001', 2,  3,  1, '上海数智产业园运营有限公司',   10, 9999.00, 99990.00, 68000.00, 31990.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 142 DAY)),
('ORD-B03-002', 3, 12,  1, '宁波活力体育有限公司',         35,  899.00, 31465.00, 14700.00, 16765.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 139 DAY)),
('ORD-B03-003', 5,  8,  2, '东莞品质生活电器有限公司',     12, 5999.00, 71988.00, 42000.00, 29988.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 136 DAY)),
('ORD-B03-004', 8,  3,  3, '北京华创企业服务有限公司',     12, 9999.00,119988.00, 81600.00, 38388.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 133 DAY)),
('ORD-B03-005', 9,  1,  3, '济南泉城通信设备有限公司',      8, 6999.00, 55992.00, 33600.00, 22392.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 130 DAY)),
('ORD-B03-006',11,  7,  4, '成都蓉城家居有限公司',         10, 4990.00, 49900.00, 28000.00, 21900.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 127 DAY)),
('ORD-B03-007',12, 20,  4, '重庆悦颜美妆有限公司',         25,  899.00, 22475.00,  8750.00, 13725.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 124 DAY)),
('ORD-B03-008', 6, 17,  2, '深圳前海金融培训中心',         30,  299.00,  8970.00,  3600.00,  5370.00, 'REFUNDED',  DATE_SUB(CURDATE(), INTERVAL 121 DAY)),


-- B04：约 4 个月前（华东继续增长，其他大区正常波动）
('ORD-B04-001', 2,  1,  1, '上海浦东软件园采购中心',       12, 6999.00, 83988.00, 50400.00, 33588.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 112 DAY)),
('ORD-B04-002', 3,  4,  1, '无锡声学设备有限公司',         18, 2299.00, 41382.00, 19800.00, 21582.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 108 DAY)),
('ORD-B04-003', 5,  2,  2, '广州天河数码广场有限公司',      8, 7999.00, 63992.00, 40800.00, 23192.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 105 DAY)),
('ORD-B04-004', 6, 10,  2, '中山悦厨电器有限公司',         25,  599.00, 14975.00,  7000.00,  7975.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 101 DAY)),
('ORD-B04-005', 8,  9,  3, '北京恒温设备工程有限公司',      7, 3299.00, 23093.00, 13300.00,  9793.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  98 DAY)),
('ORD-B04-006', 9, 13,  3, '太原冬日服饰有限公司',         16,  799.00, 12784.00,  5600.00,  7184.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  95 DAY)),
('ORD-B04-007',11,  5,  4, '成都锦城移动终端有限公司',      6, 5999.00, 35994.00, 21600.00, 14394.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  92 DAY)),
('ORD-B04-008',12, 18,  4, '昆明云上瑜伽有限公司',         24,  399.00,  9576.00,  3840.00,  5736.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  89 DAY)),


-- B05：约 3 个月前（张磊开始掉量，SKU-8821 仍正常销售）
('ORD-B05-001', 2,  2,  1, '杭州未来科技城采购中心',       10, 7999.00, 79990.00, 51000.00, 28990.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  82 DAY)),
('ORD-B05-002', 3, 15,  1, '上海海派时尚有限公司',          6, 2599.00, 15594.00,  6600.00,  8994.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  78 DAY)),
-- 张磊本阶段只有 1 单，用于“业绩骤降”场景的对比数据
('ORD-B05-003', 8,  9,  3, '北京京北公共服务有限公司',      3, 3299.00,  9897.00,  5700.00,  4197.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  74 DAY)),
('ORD-B05-004', 5,  7,  2, '珠海洁净家电有限公司',          5, 4990.00, 24950.00, 14000.00, 10950.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  72 DAY)),
('ORD-B05-005', 6, 14,  2, '深圳南山运动生活有限公司',     20,  699.00, 13980.00,  6200.00,  7780.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  70 DAY)),
('ORD-B05-006',11,  8,  4, '重庆山城品质家电有限公司',      4, 5999.00, 23996.00, 14000.00,  9996.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  68 DAY)),
-- SKU-8821 正常出单
('ORD-B05-007', 2,  6,  1, '杭州星耀数码有限公司',         12, 1299.00, 15588.00,  7800.00,  7788.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  66 DAY)),
('ORD-B05-008',12, 20,  4, '贵阳悦美生活有限公司',         12,  899.00, 10788.00,  4200.00,  6588.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  64 DAY)),
('ORD-B05-009', 9, 11,  3, '天津蓝天环境设备有限公司',      5, 2199.00, 10995.00,  6000.00,  4995.00, 'CANCELLED', DATE_SUB(CURDATE(), INTERVAL  62 DAY)),
-- 王芳退单
('ORD-B05-010', 3, 15,  1, '上海尚雅奢品有限公司',          1, 2599.00,  2599.00,  1100.00,  1499.00, 'REFUNDED',  DATE_SUB(CURDATE(), INTERVAL  60 DAY)),


-- B06：约 50～20 天前（华北区最后一批；SKU-8821 最后一单）
('ORD-B06-001', 2,  3,  1, '苏州工业园区信息中心',          8, 9999.00, 79992.00, 54400.00, 25592.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  52 DAY)),
('ORD-B06-002', 3, 12,  1, '南京奔跑体育有限公司',         25,  899.00, 22475.00, 10500.00, 11975.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  48 DAY)),
('ORD-B06-003', 5,  1,  2, '广州智联通信有限公司',          6, 6999.00, 41994.00, 25200.00, 16794.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  45 DAY)),
('ORD-B06-004', 6, 16,  2, '东莞创意办公有限公司',         80,   99.00,  7920.00,  3200.00,  4720.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  42 DAY)),
('ORD-B06-005',11,  9,  4, '成都暖通工程有限公司',          8, 3299.00, 26392.00, 15200.00, 11192.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  38 DAY)),
-- SKU-8821 最后一笔，之后刻意不再插入该产品订单，用于连续零销售预警
('ORD-B06-006', 2,  6,  1, '义乌新城数码批发有限公司',     10, 1299.00, 12990.00,  6500.00,  6490.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  30 DAY)),
('ORD-B06-007',12, 14,  4, '昆明高原运动有限公司',         18,  699.00, 12582.00,  5580.00,  7002.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  27 DAY)),
-- 华北区最后一批，近 14 天不再插入华北订单，用于大区骤降预警
('ORD-B06-008', 8,  3,  3, '北京中关村企业服务有限公司',    2, 9999.00, 19998.00, 13600.00,  6398.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  22 DAY)),
('ORD-B06-009', 9,  4,  3, '天津滨海数码商贸有限公司',      5, 2299.00, 11495.00,  5500.00,  5995.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  20 DAY)),
('ORD-B06-010', 6, 20,  2, '深圳悦容美妆有限公司',          4,  899.00,  3596.00,  1400.00,  2196.00, 'REFUNDED',  DATE_SUB(CURDATE(), INTERVAL  19 DAY)),


-- B07：近 14 天（华东、华南、西南正常；华北区和 SKU-8821 刻意留空）
('ORD-B07-001', 2,  2,  1, '上海虹桥数码授权中心',          5, 7999.00, 39995.00, 25500.00, 14495.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  14 DAY)),
('ORD-B07-002', 3,  7,  1, '南京品质家居有限公司',          4, 4990.00, 19960.00, 11200.00,  8760.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  12 DAY)),
('ORD-B07-003', 5,  3,  2, '广州琶洲会展服务有限公司',      3, 9999.00, 29997.00, 20400.00,  9597.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL  10 DAY)),
('ORD-B07-004', 6, 18,  2, '深圳湾健身管理有限公司',       20,  399.00,  7980.00,  3200.00,  4780.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL   8 DAY)),
('ORD-B07-005',11,  5,  4, '成都天府数码体验中心',          4, 5999.00, 23996.00, 14400.00,  9596.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL   6 DAY)),
('ORD-B07-006', 2,  1,  1, '杭州滨江旗舰体验中心',          3, 6999.00, 20997.00, 12600.00,  8397.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL   4 DAY)),
('ORD-B07-007', 5,  2,  2, '广州天环数码授权中心',          2, 7999.00, 15998.00, 10200.00,  5798.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL   2 DAY)),
('ORD-B07-008',12, 10,  4, '昆明春城生活电器有限公司',     12,  599.00,  7188.00,  3360.00,  3828.00, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL   1 DAY)),
('ORD-B07-009', 3, 13,  1, '苏州暖冬服饰有限公司',          5,  799.00,  3995.00,  1750.00,  2245.00, 'CANCELLED', DATE_SUB(CURDATE(), INTERVAL   3 DAY));

-- 统一本地测试订单中的占位客户名称。
UPDATE sa_sales_order SET customer_name = '上海云帆科技有限公司'      WHERE order_no = 'DEMO-E-150-01';
UPDATE sa_sales_order SET customer_name = '杭州澄海信息技术有限公司'  WHERE order_no = 'DEMO-E-120-01';
UPDATE sa_sales_order SET customer_name = '南京锐动体育用品有限公司'  WHERE order_no = 'DEMO-E-090-01';
UPDATE sa_sales_order SET customer_name = '苏州工业园区采购中心'      WHERE order_no = 'DEMO-E-060-01';
UPDATE sa_sales_order SET customer_name = '上海浦东软件园采购中心'    WHERE order_no = 'DEMO-E-030-01';
UPDATE sa_sales_order SET customer_name = '杭州滨江旗舰体验中心'      WHERE order_no = 'DEMO-E-010-01';
UPDATE sa_sales_order SET customer_name = '宁波活力体育有限公司'      WHERE order_no = 'DEMO-E-020-02';
UPDATE sa_sales_order SET customer_name = '上海尚雅奢品有限公司'      WHERE order_no = 'DEMO-E-008-R1';
UPDATE sa_sales_order SET customer_name = '南京文创办公有限公司'      WHERE order_no = 'DEMO-E-006-C1';
UPDATE sa_sales_order SET customer_name = '广州臻选家电有限公司'      WHERE order_no = 'DEMO-S-150-01';
UPDATE sa_sales_order SET customer_name = '深圳智联通信有限公司'      WHERE order_no = 'DEMO-S-120-01';
UPDATE sa_sales_order SET customer_name = '东莞品质生活电器有限公司'  WHERE order_no = 'DEMO-S-090-01';
UPDATE sa_sales_order SET customer_name = '佛山暖通工程有限公司'      WHERE order_no = 'DEMO-S-060-01';
UPDATE sa_sales_order SET customer_name = '广州天河数码广场有限公司'  WHERE order_no = 'DEMO-S-030-01';
UPDATE sa_sales_order SET customer_name = '深圳前海办公服务有限公司'  WHERE order_no = 'DEMO-S-012-01';
UPDATE sa_sales_order SET customer_name = '深圳悦容美妆有限公司'      WHERE order_no = 'DEMO-S-009-R1';
UPDATE sa_sales_order SET customer_name = '中山悦厨电器有限公司'      WHERE order_no = 'DEMO-S-005-C1';


-- 数据场景检查（只查询，不修改数据），验证数据正确性
-- 各大区订单数量和金额
SELECT r.name, COUNT(*) AS order_count, SUM(o.amount) AS total_amount
FROM sa_sales_order o
JOIN sa_sales_region r ON o.region_id = r.id
GROUP BY r.id, r.name
ORDER BY total_amount DESC;

-- 近 7 个月月度趋势
SELECT DATE_FORMAT(order_date, '%Y-%m') AS month,
       COUNT(*) AS orders,
       SUM(amount) AS total
FROM sa_sales_order
WHERE status = 'COMPLETED'
  AND order_date >= DATE_SUB(CURDATE(), INTERVAL 210 DAY)
GROUP BY month
ORDER BY month;

-- 华北区近 14 天订单数，预期为 0，用于大区骤降预警
SELECT COUNT(*) FROM sa_sales_order
WHERE region_id = 3
  AND order_date >= DATE_SUB(CURDATE(), INTERVAL 14 DAY);

-- SKU-8821 近 14 天订单数，预期为 0，用于连续零销售预警
SELECT COUNT(*) FROM sa_sales_order
WHERE product_id = 6
  AND order_date >= DATE_SUB(CURDATE(), INTERVAL 14 DAY);

-- 张磊近期订单量，用于和历史正常期对比
SELECT COUNT(*) FROM sa_sales_order
WHERE rep_id = 8
  AND order_date >= DATE_SUB(CURDATE(), INTERVAL 60 DAY);
