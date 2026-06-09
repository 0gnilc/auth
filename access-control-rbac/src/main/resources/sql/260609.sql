-- RBAC schema v2 manual migration for access-control-rbac.
--
-- This repository does not own a Flyway/Liquibase runtime. Execute this script manually
-- or copy it into the consuming application's migration system after reviewing the
-- preflight duplicate checks below.
--
-- Confirmed conventions:
--   * Java fields use camelCase; database columns use snake_case.
--   * Every RBAC table keeps id, del, create_time, update_time.
--   * del remains the soft-delete column.
--   * tinyint(1) columns with 0/1 true/false semantics map to Java Boolean.

-- -----------------------------------------------------------------------------
-- Preflight duplicate checks. Resolve duplicates before adding unique indexes.
-- -----------------------------------------------------------------------------
SELECT code_or_symbol, cnt
FROM (
    SELECT symbol AS code_or_symbol, COUNT(*) AS cnt
    FROM authz_role
    GROUP BY symbol
) t
WHERE cnt > 1;

SELECT code_or_symbol, cnt
FROM (
    SELECT symbol AS code_or_symbol, COUNT(*) AS cnt
    FROM authz_permission
    GROUP BY symbol
) t
WHERE cnt > 1;

SELECT name, COUNT(*) AS cnt
FROM authz_menu
GROUP BY name
HAVING cnt > 1;

SELECT path, COUNT(*) AS cnt
FROM authz_menu
WHERE path IS NOT NULL
GROUP BY path
HAVING cnt > 1;

SELECT symbol AS access_code, COUNT(*) AS cnt
FROM authz_menu
WHERE symbol IS NOT NULL
  AND type = 3
GROUP BY symbol
HAVING cnt > 1;

SELECT user_id, role_id, COUNT(*) AS cnt
FROM authz_user_role
GROUP BY user_id, role_id
HAVING cnt > 1;

SELECT role_id, permission_id, COUNT(*) AS cnt
FROM authz_role_permission
GROUP BY role_id, permission_id
HAVING cnt > 1;

SELECT role_id, menu_id, COUNT(*) AS cnt
FROM authz_role_menu
GROUP BY role_id, menu_id
HAVING cnt > 1;

-- -----------------------------------------------------------------------------
-- authz_role
-- -----------------------------------------------------------------------------
ALTER TABLE authz_role
    CHANGE COLUMN symbol code varchar(255) NOT NULL COMMENT '角色标识',
    CHANGE COLUMN internal built_in tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否系统内置,0否、1是',
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    MODIFY COLUMN del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    MODIFY COLUMN create_time datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN update_time datetime DEFAULT NULL COMMENT '修改时间',
    MODIFY COLUMN name varchar(255) NOT NULL COMMENT '角色名称',
    MODIFY COLUMN remark varchar(500) DEFAULT NULL COMMENT '描述/备注';

CREATE UNIQUE INDEX uk_code ON authz_role (code);

-- -----------------------------------------------------------------------------
-- authz_permission
-- -----------------------------------------------------------------------------
ALTER TABLE authz_permission
    CHANGE COLUMN symbol code varchar(255) NOT NULL COMMENT '权限标识',
    CHANGE COLUMN resource target_identifier varchar(500) DEFAULT NULL COMMENT '访问目标标识',
    CHANGE COLUMN exposed public_access tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否公开访问,0否、1是',
    ADD COLUMN target_qualifier varchar(100) DEFAULT NULL COMMENT '访问目标限定符' AFTER target_identifier,
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    MODIFY COLUMN del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    MODIFY COLUMN create_time datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN update_time datetime DEFAULT NULL COMMENT '修改时间',
    MODIFY COLUMN name varchar(255) NOT NULL COMMENT '权限名称',
    MODIFY COLUMN remark varchar(500) DEFAULT NULL COMMENT '描述/备注';

CREATE UNIQUE INDEX uk_code ON authz_permission (code);
CREATE INDEX idx_target_identifier ON authz_permission (target_identifier);
CREATE INDEX idx_target_qualifier ON authz_permission (target_qualifier);

-- -----------------------------------------------------------------------------
-- authz_menu
-- -----------------------------------------------------------------------------
-- Preserve current ordering before making `order` NOT NULL.
UPDATE authz_menu SET sort = 999 WHERE sort IS NULL;

ALTER TABLE authz_menu
    CHANGE COLUMN parent_id pid bigint NOT NULL DEFAULT '0' COMMENT '父级ID',
    CHANGE COLUMN symbol access_code varchar(255) DEFAULT NULL COMMENT '后端权限标识',
    CHANGE COLUMN sort `order` int NOT NULL DEFAULT '999' COMMENT '排序',
    CHANGE COLUMN frame_src iframe_src varchar(500) DEFAULT NULL COMMENT '内嵌 iframe 地址',
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    MODIFY COLUMN del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    MODIFY COLUMN create_time datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN update_time datetime DEFAULT NULL COMMENT '修改时间',
    MODIFY COLUMN type varchar(16) NOT NULL COMMENT '菜单类型',
    MODIFY COLUMN name varchar(255) NOT NULL COMMENT '菜单名称',
    MODIFY COLUMN path varchar(500) DEFAULT NULL COMMENT '路由路径',
    MODIFY COLUMN component varchar(255) DEFAULT NULL COMMENT '组件',
    MODIFY COLUMN redirect varchar(500) DEFAULT NULL COMMENT '重定向',
    MODIFY COLUMN title varchar(255) NOT NULL COMMENT '菜单标题',
    MODIFY COLUMN icon varchar(255) DEFAULT NULL COMMENT '图标',
    MODIFY COLUMN keep_alive tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否缓存页面',
    ADD COLUMN status tinyint(1) NOT NULL DEFAULT '1' COMMENT '菜单状态,0已禁用、1已启用' AFTER type,
    ADD COLUMN active_path varchar(500) DEFAULT NULL COMMENT '指定当前激活的菜单' AFTER redirect,
    ADD COLUMN affix_tab tinyint(1) NOT NULL DEFAULT '0' COMMENT '固定标签页' AFTER active_path,
    ADD COLUMN affix_tab_order int DEFAULT NULL COMMENT '固定标签页排序' AFTER affix_tab,
    ADD COLUMN badge varchar(100) DEFAULT NULL COMMENT '徽标' AFTER affix_tab_order,
    ADD COLUMN badge_type varchar(16) DEFAULT NULL COMMENT '徽标类型' AFTER badge,
    ADD COLUMN badge_variants varchar(32) DEFAULT NULL COMMENT '徽标样式' AFTER badge_type,
    ADD COLUMN full_path_key tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否使用完整路径作为标签页 key' AFTER badge_variants,
    ADD COLUMN hide_children_in_menu tinyint(1) NOT NULL DEFAULT '0' COMMENT '在菜单中隐藏子级' AFTER full_path_key,
    ADD COLUMN hide_in_breadcrumb tinyint(1) NOT NULL DEFAULT '0' COMMENT '在面包屑中隐藏' AFTER hide_children_in_menu,
    ADD COLUMN hide_in_menu tinyint(1) NOT NULL DEFAULT '0' COMMENT '在菜单中隐藏' AFTER hide_in_breadcrumb,
    ADD COLUMN hide_in_tab tinyint(1) NOT NULL DEFAULT '0' COMMENT '在标签页中隐藏' AFTER hide_in_menu,
    ADD COLUMN ignore_access tinyint(1) NOT NULL DEFAULT '0' COMMENT '忽略权限访问控制' AFTER iframe_src,
    ADD COLUMN link varchar(500) DEFAULT NULL COMMENT '外链地址' AFTER keep_alive,
    ADD COLUMN loaded tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已加载' AFTER link,
    ADD COLUMN max_num_of_open_tab int DEFAULT NULL COMMENT '同名标签页最大打开数量' AFTER loaded,
    ADD COLUMN menu_visible_with_forbidden tinyint(1) NOT NULL DEFAULT '0' COMMENT '菜单可见但访问时跳转 403' AFTER max_num_of_open_tab,
    ADD COLUMN no_basic_layout tinyint(1) NOT NULL DEFAULT '0' COMMENT '不使用基础布局' AFTER menu_visible_with_forbidden,
    ADD COLUMN open_in_new_window tinyint(1) NOT NULL DEFAULT '0' COMMENT '在新窗口打开' AFTER no_basic_layout,
    ADD COLUMN query json DEFAULT NULL COMMENT '路由查询参数' AFTER `order`;

-- Map old integer menu types to new string values.
UPDATE authz_menu SET type = 'embedded' WHERE type = '1';
UPDATE authz_menu SET type = 'link' WHERE type = '2';
UPDATE authz_menu SET type = 'button' WHERE type = '3';

-- Old TYPE_MENU=0 did not distinguish catalog and page menu. Use children or empty
-- component as the first-pass catalog signal, then review the result manually.
CREATE TEMPORARY TABLE tmp_authz_menu_catalog_ids AS
SELECT DISTINCT parent.id
FROM authz_menu parent
LEFT JOIN authz_menu child ON child.pid = parent.id
WHERE parent.type = '0'
  AND (child.id IS NOT NULL OR parent.component IS NULL OR parent.component = '');

UPDATE authz_menu
SET type = 'catalog'
WHERE id IN (SELECT id FROM tmp_authz_menu_catalog_ids);

UPDATE authz_menu
SET type = 'menu'
WHERE type = '0';

DROP TEMPORARY TABLE tmp_authz_menu_catalog_ids;

-- access_code now means backend permission identifier. Keep it only for buttons.
UPDATE authz_menu SET access_code = NULL WHERE type <> 'button';

ALTER TABLE authz_menu
    DROP COLUMN extra_icon,
    DROP COLUMN show_link,
    DROP COLUMN show_parent;

CREATE INDEX idx_pid ON authz_menu (pid);
CREATE INDEX idx_type ON authz_menu (type);
CREATE INDEX idx_order ON authz_menu (`order`);
CREATE UNIQUE INDEX uk_access_code ON authz_menu (access_code);
CREATE UNIQUE INDEX uk_name ON authz_menu (name);
CREATE UNIQUE INDEX uk_path ON authz_menu (path);

-- -----------------------------------------------------------------------------
-- authz_user and relation tables
-- -----------------------------------------------------------------------------
ALTER TABLE authz_user
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    MODIFY COLUMN del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    MODIFY COLUMN create_time datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN update_time datetime DEFAULT NULL COMMENT '修改时间';

ALTER TABLE authz_user_role
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    MODIFY COLUMN del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    MODIFY COLUMN create_time datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN update_time datetime DEFAULT NULL COMMENT '修改时间',
    MODIFY COLUMN user_id bigint NOT NULL COMMENT '用户ID',
    MODIFY COLUMN role_id bigint NOT NULL COMMENT '角色ID';

ALTER TABLE authz_role_permission
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    MODIFY COLUMN del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    MODIFY COLUMN create_time datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN update_time datetime DEFAULT NULL COMMENT '修改时间',
    MODIFY COLUMN role_id bigint NOT NULL COMMENT '角色ID',
    MODIFY COLUMN permission_id bigint NOT NULL COMMENT '权限ID';

ALTER TABLE authz_role_menu
    MODIFY COLUMN id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    MODIFY COLUMN del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    MODIFY COLUMN create_time datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN update_time datetime DEFAULT NULL COMMENT '修改时间',
    MODIFY COLUMN role_id bigint NOT NULL COMMENT '角色ID',
    MODIFY COLUMN menu_id bigint NOT NULL COMMENT '菜单ID';

CREATE INDEX idx_user ON authz_user_role (user_id);
CREATE INDEX idx_role ON authz_user_role (role_id);
CREATE UNIQUE INDEX uk_user_role ON authz_user_role (user_id, role_id);

CREATE INDEX idx_role ON authz_role_permission (role_id);
CREATE INDEX idx_permission ON authz_role_permission (permission_id);
CREATE UNIQUE INDEX uk_role_permission ON authz_role_permission (role_id, permission_id);

CREATE INDEX idx_role ON authz_role_menu (role_id);
CREATE INDEX idx_menu ON authz_role_menu (menu_id);
CREATE UNIQUE INDEX uk_role_menu ON authz_role_menu (role_id, menu_id);

-- -----------------------------------------------------------------------------
-- Post-migration review queries
-- -----------------------------------------------------------------------------
SELECT id, type, status, pid, path, component, iframe_src, link, access_code
FROM authz_menu
ORDER BY id
LIMIT 50;

SELECT id, code, built_in
FROM authz_role
ORDER BY id
LIMIT 50;

SELECT id, code, target_identifier, target_qualifier, public_access
FROM authz_permission
ORDER BY id
LIMIT 50;
