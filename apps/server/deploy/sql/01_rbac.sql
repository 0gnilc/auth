-- RBAC 当前版本空库表结构。
-- 用于干净 schema 首次创建当前版本 RBAC 表。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS az_role (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '角色记录主键',
    del tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0 表示有效，1 表示已删除',
    create_time datetime(6) NOT NULL COMMENT '记录创建的 UTC 时间点',
    update_time datetime(6) DEFAULT NULL COMMENT '记录最近更新的 UTC 时间点；尚未记录更新时可为空',
    code varchar(320) NOT NULL COMMENT '角色稳定标识码',
    name varchar(255) NOT NULL COMMENT '角色显示名称',
    remark varchar(500) DEFAULT NULL COMMENT '可选的管理备注',
    built_in tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为系统维护的内置角色：0 否、1 是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RBAC 角色定义';

ALTER TABLE az_role
    MODIFY COLUMN code varchar(320) NOT NULL COMMENT '角色稳定标识码';

CREATE TABLE IF NOT EXISTS az_permission (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '权限记录主键',
    del tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0 表示有效，1 表示已删除',
    create_time datetime(6) NOT NULL COMMENT '记录创建的 UTC 时间点',
    update_time datetime(6) DEFAULT NULL COMMENT '记录最近更新的 UTC 时间点；尚未记录更新时可为空',
    code varchar(320) NOT NULL COMMENT '权限的稳定标识码',
    name varchar(255) NOT NULL COMMENT '权限显示名称',
    target_identifier varchar(500) DEFAULT NULL COMMENT '受保护访问目标标识，例如后端请求路径；可为 NULL',
    target_qualifier varchar(100) DEFAULT NULL COMMENT '目标限定符，例如 HTTP 方法；NULL 表示不限定变体',
    remark varchar(500) DEFAULT NULL COMMENT '可选的管理备注',
    public_access tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否允许无需角色绑定的公开访问：0 否、1 是；不授予菜单可见性',
    built_in tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为系统维护的内置权限：0 否、1 是',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_target_identifier (target_identifier),
    KEY idx_target_qualifier (target_qualifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问目标权限定义';

ALTER TABLE az_permission
    MODIFY COLUMN code varchar(320) NOT NULL COMMENT '权限的稳定标识码';

CREATE TABLE IF NOT EXISTS az_menu (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '菜单记录主键',
    del tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0 表示有效，1 表示已删除',
    create_time datetime(6) NOT NULL COMMENT '记录创建的 UTC 时间点',
    update_time datetime(6) DEFAULT NULL COMMENT '记录最近更新的 UTC 时间点；尚未记录更新时可为空',
    pid bigint NOT NULL DEFAULT '0' COMMENT '父菜单主键，0 表示根节点',
    type varchar(16) NOT NULL COMMENT '菜单类型：catalog 目录、menu 页面、embedded 内嵌、link 外链、button 按钮；创建后不可变',
    status tinyint(1) NOT NULL DEFAULT '1' COMMENT '菜单是否启用：0 停用、1 启用；停用时不参与可见导航',
    access_code varchar(320) DEFAULT NULL COMMENT '菜单操作的前端访问码，控制可见性；NULL 表示未配置，不替代后端目标权限',
    name varchar(320) NOT NULL COMMENT '唯一前端路由名称，用于路由身份和页面缓存标识',
    path varchar(560) DEFAULT NULL COMMENT '前端路由路径；页面、内嵌和外链菜单需要提供，可空',
    component varchar(255) DEFAULT NULL COMMENT '页面映射所使用的前端组件路径；可为 NULL',
    redirect varchar(500) DEFAULT NULL COMMENT '可选的导航重定向路径',
    active_path varchar(500) DEFAULT NULL COMMENT '打开该路由时需要高亮的菜单路径；可为 NULL',
    affix_tab tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否将该路由固定在标签栏。0 表示否或停用，1 表示是或启用',
    affix_tab_order int DEFAULT NULL COMMENT '固定标签页的排序值；可为 NULL',
    badge varchar(100) DEFAULT NULL COMMENT '菜单徽标显示文本；可为 NULL',
    badge_type varchar(16) DEFAULT NULL COMMENT '菜单徽标的显示类型；可为 NULL',
    badge_variants varchar(32) DEFAULT NULL COMMENT '菜单徽标的视觉样式标识；可为 NULL',
    full_path_key tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否以包含查询参数的完整路径区分标签页。0 表示否或停用，1 表示是或启用',
    hide_children_in_menu tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否在菜单导航中隐藏子级。0 表示否或停用，1 表示是或启用',
    hide_in_breadcrumb tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否在面包屑导航中隐藏该项。0 表示否或停用，1 表示是或启用',
    hide_in_menu tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否在菜单导航中隐藏该项。0 表示否或停用，1 表示是或启用',
    hide_in_tab tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否在标签栏中隐藏该项。0 表示否或停用，1 表示是或启用',
    icon varchar(255) DEFAULT NULL COMMENT '菜单显示的图标标识；可为 NULL',
    iframe_src varchar(500) DEFAULT NULL COMMENT '内嵌页面的 HTTP 或 HTTPS 地址；可为 NULL',
    keep_alive tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否缓存对应页面实例。0 表示否或停用，1 表示是或启用',
    link varchar(500) DEFAULT NULL COMMENT '外链菜单目标的 HTTP 或 HTTPS 地址；可为 NULL',
    max_num_of_open_tab int DEFAULT NULL COMMENT '同一路由名称允许同时打开的标签页数量上限；可为 NULL',
    no_basic_layout tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否跳过前端基础布局。0 表示否或停用，1 表示是或启用',
    open_in_new_window tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否在新窗口打开目标。0 表示否或停用，1 表示是或启用',
    `order` int NOT NULL DEFAULT '999' COMMENT '菜单显示排序值，数值越小越靠前',
    query json DEFAULT NULL COMMENT '导航附带的 JSON 查询参数对象；NULL 表示未配置',
    title varchar(255) NOT NULL COMMENT '菜单显示标题，可使用对应的本地化消息键',
    built_in tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为系统维护的内置菜单：0 否、1 是',
    PRIMARY KEY (id),
    KEY idx_pid (pid),
    KEY idx_type (type),
    KEY idx_order (`order`),
    UNIQUE KEY uk_access_code (access_code),
    UNIQUE KEY uk_name (name),
    UNIQUE KEY uk_path (path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='前端菜单、导航和可见性配置';

ALTER TABLE az_menu
    MODIFY COLUMN access_code varchar(320) DEFAULT NULL COMMENT '菜单操作的前端访问码，控制可见性；NULL 表示未配置，不替代后端目标权限',
    MODIFY COLUMN name varchar(320) NOT NULL COMMENT '唯一前端路由名称，用于路由身份和页面缓存标识',
    MODIFY COLUMN path varchar(560) DEFAULT NULL COMMENT '前端路由路径；页面、内嵌和外链菜单需要提供，可空';

CREATE TABLE IF NOT EXISTS az_user (
    id bigint NOT NULL AUTO_INCREMENT COMMENT 'RBAC 全局用户主键，与管理员或客户资料主键区分',
    del tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0 表示有效，1 表示已删除',
    create_time datetime(6) NOT NULL COMMENT '记录创建的 UTC 时间点',
    update_time datetime(6) DEFAULT NULL COMMENT '记录最近更新的 UTC 时间点；尚未记录更新时可为空',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='独立于应用账号资料的 RBAC 全局用户身份';

CREATE TABLE IF NOT EXISTS az_user_role (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '记录的数据库主键',
    del tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0 表示有效，1 表示已删除',
    create_time datetime(6) NOT NULL COMMENT '记录创建的 UTC 时间点',
    update_time datetime(6) DEFAULT NULL COMMENT '记录最近更新的 UTC 时间点；尚未记录更新时可为空',
    user_id bigint NOT NULL COMMENT '关联的 RBAC 全局用户主键 az_user.id',
    role_id bigint NOT NULL COMMENT '关联角色主键 az_role.id',
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_role (role_id),
    KEY idx_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RBAC 用户与角色绑定';

CREATE TABLE IF NOT EXISTS az_role_permission (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '记录的数据库主键',
    del tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0 表示有效，1 表示已删除',
    create_time datetime(6) NOT NULL COMMENT '记录创建的 UTC 时间点',
    update_time datetime(6) DEFAULT NULL COMMENT '记录最近更新的 UTC 时间点；尚未记录更新时可为空',
    role_id bigint NOT NULL COMMENT '关联角色主键 az_role.id',
    permission_id bigint NOT NULL COMMENT '关联访问目标权限主键 az_permission.id',
    PRIMARY KEY (id),
    KEY idx_role (role_id),
    KEY idx_permission (permission_id),
    KEY idx_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色与访问目标权限绑定';

CREATE TABLE IF NOT EXISTS az_role_menu (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '记录的数据库主键',
    del tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0 表示有效，1 表示已删除',
    create_time datetime(6) NOT NULL COMMENT '记录创建的 UTC 时间点',
    update_time datetime(6) DEFAULT NULL COMMENT '记录最近更新的 UTC 时间点；尚未记录更新时可为空',
    role_id bigint NOT NULL COMMENT '关联角色主键 az_role.id',
    menu_id bigint NOT NULL COMMENT '关联菜单主键 az_menu.id',
    PRIMARY KEY (id),
    KEY idx_role (role_id),
    KEY idx_menu (menu_id),
    KEY idx_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色与可见菜单绑定';
