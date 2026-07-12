-- RBAC 当前版本空库表结构。
-- 用于干净 schema 首次创建当前版本 RBAC 表。

CREATE TABLE IF NOT EXISTS az_role (
    id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    create_time datetime NOT NULL COMMENT '创建时间',
    update_time datetime DEFAULT NULL COMMENT '修改时间',
    code varchar(255) NOT NULL COMMENT '角色标识',
    name varchar(255) NOT NULL COMMENT '角色名称',
    remark varchar(500) DEFAULT NULL COMMENT '描述/备注',
    built_in tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否系统内置,0否、1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色';

CREATE TABLE IF NOT EXISTS az_permission (
    id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    create_time datetime NOT NULL COMMENT '创建时间',
    update_time datetime DEFAULT NULL COMMENT '修改时间',
    code varchar(255) NOT NULL COMMENT '权限标识',
    name varchar(255) NOT NULL COMMENT '权限名称',
    target_identifier varchar(500) DEFAULT NULL COMMENT '访问目标标识',
    target_qualifier varchar(100) DEFAULT NULL COMMENT '访问目标限定符',
    remark varchar(500) DEFAULT NULL COMMENT '描述/备注',
    public_access tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否公开访问,0否、1是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_target_identifier (target_identifier),
    KEY idx_target_qualifier (target_qualifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限';

CREATE TABLE IF NOT EXISTS az_menu (
    id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    create_time datetime NOT NULL COMMENT '创建时间',
    update_time datetime DEFAULT NULL COMMENT '修改时间',
    pid bigint NOT NULL DEFAULT '0' COMMENT '父级ID',
    type varchar(16) NOT NULL COMMENT '菜单类型',
    status tinyint(1) NOT NULL DEFAULT '1' COMMENT '菜单状态,0已禁用、1已启用',
    access_code varchar(255) DEFAULT NULL COMMENT '后端权限标识',
    name varchar(255) NOT NULL COMMENT '菜单名称',
    path varchar(500) DEFAULT NULL COMMENT '路由路径',
    component varchar(255) DEFAULT NULL COMMENT '组件',
    redirect varchar(500) DEFAULT NULL COMMENT '重定向',
    active_path varchar(500) DEFAULT NULL COMMENT '指定当前激活的菜单',
    affix_tab tinyint(1) NOT NULL DEFAULT '0' COMMENT '固定标签页',
    affix_tab_order int DEFAULT NULL COMMENT '固定标签页排序',
    badge varchar(100) DEFAULT NULL COMMENT '徽标',
    badge_type varchar(16) DEFAULT NULL COMMENT '徽标类型',
    badge_variants varchar(32) DEFAULT NULL COMMENT '徽标样式',
    full_path_key tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否使用完整路径作为标签页 key',
    hide_children_in_menu tinyint(1) NOT NULL DEFAULT '0' COMMENT '在菜单中隐藏子级',
    hide_in_breadcrumb tinyint(1) NOT NULL DEFAULT '0' COMMENT '在面包屑中隐藏',
    hide_in_menu tinyint(1) NOT NULL DEFAULT '0' COMMENT '在菜单中隐藏',
    hide_in_tab tinyint(1) NOT NULL DEFAULT '0' COMMENT '在标签页中隐藏',
    icon varchar(255) DEFAULT NULL COMMENT '图标',
    iframe_src varchar(500) DEFAULT NULL COMMENT '内嵌 iframe 地址',
    ignore_access tinyint(1) NOT NULL DEFAULT '0' COMMENT '忽略权限访问控制',
    keep_alive tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否缓存页面',
    link varchar(500) DEFAULT NULL COMMENT '外链地址',
    loaded tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已加载',
    max_num_of_open_tab int DEFAULT NULL COMMENT '同名标签页最大打开数量',
    menu_visible_with_forbidden tinyint(1) NOT NULL DEFAULT '0' COMMENT '菜单可见但访问时跳转 403',
    no_basic_layout tinyint(1) NOT NULL DEFAULT '0' COMMENT '不使用基础布局',
    open_in_new_window tinyint(1) NOT NULL DEFAULT '0' COMMENT '在新窗口打开',
    `order` int NOT NULL DEFAULT '999' COMMENT '排序',
    query json DEFAULT NULL COMMENT '路由查询参数',
    title varchar(255) NOT NULL COMMENT '菜单标题',
    PRIMARY KEY (id),
    KEY idx_pid (pid),
    KEY idx_type (type),
    KEY idx_order (`order`),
    UNIQUE KEY uk_access_code (access_code),
    UNIQUE KEY uk_name (name),
    UNIQUE KEY uk_path (path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单';

CREATE TABLE IF NOT EXISTS az_user (
    id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    create_time datetime NOT NULL COMMENT '创建时间',
    update_time datetime DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';

CREATE TABLE IF NOT EXISTS az_user_role (
    id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    create_time datetime NOT NULL COMMENT '创建时间',
    update_time datetime DEFAULT NULL COMMENT '修改时间',
    user_id bigint NOT NULL COMMENT '用户ID',
    role_id bigint NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_role (role_id),
    KEY idx_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关系';

CREATE TABLE IF NOT EXISTS az_role_permission (
    id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    create_time datetime NOT NULL COMMENT '创建时间',
    update_time datetime DEFAULT NULL COMMENT '修改时间',
    role_id bigint NOT NULL COMMENT '角色ID',
    permission_id bigint NOT NULL COMMENT '权限ID',
    PRIMARY KEY (id),
    KEY idx_role (role_id),
    KEY idx_permission (permission_id),
    KEY idx_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关系';

CREATE TABLE IF NOT EXISTS az_role_menu (
    id bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    del tinyint NOT NULL DEFAULT '0' COMMENT '是否删除,0未删除、1已删除',
    create_time datetime NOT NULL COMMENT '创建时间',
    update_time datetime DEFAULT NULL COMMENT '修改时间',
    role_id bigint NOT NULL COMMENT '角色ID',
    menu_id bigint NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (id),
    KEY idx_role (role_id),
    KEY idx_menu (menu_id),
    KEY idx_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关系';
