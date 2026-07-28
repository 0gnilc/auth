-- RBAC 与后台管理员管理功能初始化脚本。
-- 依赖依次执行 01_rbac.sql 至 06_i18n.sql。
-- 除空库初始化外，本脚本还可为上一版本当前表结构幂等补齐 built_in 字段。

SET NAMES utf8mb4;

SET @permission_built_in_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'az_permission'
      AND column_name = 'built_in'
);
SET @permission_built_in_ddl := IF(
    @permission_built_in_exists = 0,
    'ALTER TABLE az_permission ADD COLUMN built_in tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否系统内置,0否、1是'' AFTER public_access',
    'SELECT 1'
);
PREPARE permission_built_in_statement FROM @permission_built_in_ddl;
EXECUTE permission_built_in_statement;
DEALLOCATE PREPARE permission_built_in_statement;

SET @menu_built_in_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'az_menu'
      AND column_name = 'built_in'
);
SET @menu_built_in_ddl := IF(
    @menu_built_in_exists = 0,
    'ALTER TABLE az_menu ADD COLUMN built_in tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否系统内置,0否、1是'' AFTER title',
    'SELECT 1'
);
PREPARE menu_built_in_statement FROM @menu_built_in_ddl;
EXECUTE menu_built_in_statement;
DEALLOCATE PREPARE menu_built_in_statement;

-- 将旧角色标识迁移为统一的冒号格式，并在新旧角色并存时合并授权关系。
SET @legacy_rbac_manager_role_id := (
    SELECT id FROM az_role WHERE code = 'rbac-manager' AND del = 0 LIMIT 1
);
SET @rbac_manager_role_id := (
    SELECT id FROM az_role WHERE code = 'rbac:manager' AND del = 0 LIMIT 1
);

UPDATE az_role
SET code = 'rbac:manager',
    update_time = UTC_TIMESTAMP(6)
WHERE id = @legacy_rbac_manager_role_id
  AND @rbac_manager_role_id IS NULL;

SET @rbac_manager_role_id := (
    SELECT id FROM az_role WHERE code = 'rbac:manager' AND del = 0 LIMIT 1
);

INSERT INTO az_user_role (del, create_time, update_time, user_id, role_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, legacy_binding.user_id, @rbac_manager_role_id
FROM az_user_role legacy_binding
WHERE legacy_binding.role_id = @legacy_rbac_manager_role_id
  AND legacy_binding.del = 0
  AND @legacy_rbac_manager_role_id <> @rbac_manager_role_id
  AND NOT EXISTS (
      SELECT 1 FROM az_user_role current_binding
      WHERE current_binding.user_id = legacy_binding.user_id
        AND current_binding.role_id = @rbac_manager_role_id
        AND current_binding.del = 0
  );

INSERT INTO az_role_permission (del, create_time, update_time, role_id, permission_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @rbac_manager_role_id, legacy_binding.permission_id
FROM az_role_permission legacy_binding
WHERE legacy_binding.role_id = @legacy_rbac_manager_role_id
  AND legacy_binding.del = 0
  AND @legacy_rbac_manager_role_id <> @rbac_manager_role_id
  AND NOT EXISTS (
      SELECT 1 FROM az_role_permission current_binding
      WHERE current_binding.role_id = @rbac_manager_role_id
        AND current_binding.permission_id = legacy_binding.permission_id
        AND current_binding.del = 0
  );

INSERT INTO az_role_menu (del, create_time, update_time, role_id, menu_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @rbac_manager_role_id, legacy_binding.menu_id
FROM az_role_menu legacy_binding
WHERE legacy_binding.role_id = @legacy_rbac_manager_role_id
  AND legacy_binding.del = 0
  AND @legacy_rbac_manager_role_id <> @rbac_manager_role_id
  AND NOT EXISTS (
      SELECT 1 FROM az_role_menu current_binding
      WHERE current_binding.role_id = @rbac_manager_role_id
        AND current_binding.menu_id = legacy_binding.menu_id
        AND current_binding.del = 0
  );

UPDATE az_user_role
SET del = 1, update_time = UTC_TIMESTAMP(6)
WHERE role_id = @legacy_rbac_manager_role_id
  AND @legacy_rbac_manager_role_id <> @rbac_manager_role_id;
UPDATE az_role_permission
SET del = 1, update_time = UTC_TIMESTAMP(6)
WHERE role_id = @legacy_rbac_manager_role_id
  AND @legacy_rbac_manager_role_id <> @rbac_manager_role_id;
UPDATE az_role_menu
SET del = 1, update_time = UTC_TIMESTAMP(6)
WHERE role_id = @legacy_rbac_manager_role_id
  AND @legacy_rbac_manager_role_id <> @rbac_manager_role_id;
UPDATE az_role
SET del = 1, update_time = UTC_TIMESTAMP(6)
WHERE id = @legacy_rbac_manager_role_id
  AND @legacy_rbac_manager_role_id <> @rbac_manager_role_id;

UPDATE az_role
SET del = 0,
    built_in = 1,
    update_time = UTC_TIMESTAMP(6)
WHERE code = 'rbac:manager'
  AND (del <> 0 OR built_in <> 1);

INSERT INTO az_role (del, create_time, update_time, code, name, remark, built_in)
SELECT 0, UTC_TIMESTAMP(6), NULL, 'rbac:manager', 'RBAC 管理员', '维护后台管理员、角色、权限和菜单', 1
WHERE NOT EXISTS (
    SELECT 1 FROM az_role WHERE code = 'rbac:manager'
);

-- 关联集合保存接口取代旧 update 路径，并保留已有角色授权。
DROP TEMPORARY TABLE IF EXISTS permission_route_rename;
CREATE TEMPORARY TABLE permission_route_rename (
    old_code varchar(255) NOT NULL,
    new_code varchar(255) NOT NULL,
    new_target_identifier varchar(500) NOT NULL,
    PRIMARY KEY (old_code),
    UNIQUE KEY uk_new_code (new_code)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO permission_route_rename (old_code, new_code, new_target_identifier)
VALUES
    ('POST:/sys/admin/update-roles', 'POST:/sys/admin/roles/save', '/sys/admin/roles/save'),
    ('POST:/authz/role-permission/update', 'POST:/authz/role-permission/save', '/authz/role-permission/save'),
    ('POST:/authz/role-menu/update', 'POST:/authz/role-menu/save', '/authz/role-menu/save');

UPDATE az_permission old_permission
JOIN permission_route_rename route_rename ON route_rename.old_code = old_permission.code
LEFT JOIN az_permission new_permission ON new_permission.code = route_rename.new_code
SET old_permission.code = route_rename.new_code,
    old_permission.name = route_rename.new_code,
    old_permission.target_identifier = route_rename.new_target_identifier,
    old_permission.update_time = UTC_TIMESTAMP(6)
WHERE new_permission.id IS NULL;

UPDATE az_permission permission
JOIN permission_route_rename route_rename ON route_rename.new_code = permission.code
SET permission.del = 0,
    permission.name = route_rename.new_code,
    permission.target_identifier = route_rename.new_target_identifier,
    permission.update_time = UTC_TIMESTAMP(6);

INSERT INTO az_role_permission (del, create_time, update_time, role_id, permission_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, old_binding.role_id, new_permission.id
FROM permission_route_rename route_rename
JOIN az_permission old_permission ON old_permission.code = route_rename.old_code
JOIN az_permission new_permission ON new_permission.code = route_rename.new_code
JOIN az_role_permission old_binding ON old_binding.permission_id = old_permission.id
WHERE old_binding.del = 0
  AND NOT EXISTS (
      SELECT 1
      FROM az_role_permission new_binding
      WHERE new_binding.role_id = old_binding.role_id
        AND new_binding.permission_id = new_permission.id
        AND new_binding.del = 0
  );

UPDATE az_role_permission old_binding
JOIN az_permission old_permission ON old_permission.id = old_binding.permission_id
JOIN permission_route_rename route_rename ON route_rename.old_code = old_permission.code
SET old_binding.del = 1,
    old_binding.update_time = UTC_TIMESTAMP(6)
WHERE old_binding.del = 0;

UPDATE az_permission old_permission
JOIN permission_route_rename route_rename ON route_rename.old_code = old_permission.code
SET old_permission.del = 1,
    old_permission.update_time = UTC_TIMESTAMP(6)
WHERE old_permission.del = 0;

DROP TEMPORARY TABLE permission_route_rename;

-- 系统端点权限是部署配置，不允许通过权限管理界面修改或删除。
UPDATE az_permission
SET built_in = 1,
    update_time = UTC_TIMESTAMP(6)
WHERE code = '*:/error'
   OR target_identifier LIKE '/authz/%'
   OR target_identifier LIKE '/sys/admin/%'
   OR target_identifier LIKE '/sys/i18n-message/%';

-- RBAC 管理接口和后台管理员管理接口统一要求 rbac:manager 权限。
UPDATE az_permission
SET public_access = 0,
    built_in = 1,
    update_time = UTC_TIMESTAMP(6)
WHERE target_identifier LIKE '/authz/%'
   OR code IN (
       'POST:/sys/admin/page',
       'POST:/sys/admin/create',
       'POST:/sys/admin/update',
       'POST:/sys/admin/roles/save',
       'POST:/sys/admin/remove/{id}'
   );

SET @rbac_manager_role_id := (
    SELECT id FROM az_role WHERE code = 'rbac:manager' AND del = 0 LIMIT 1
);
SET @default_admin_user_id := (
    SELECT user_id FROM sys_admin WHERE username = 'admin' AND del = 0 LIMIT 1
);

INSERT INTO az_role_permission (del, create_time, update_time, role_id, permission_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @rbac_manager_role_id, p.id
FROM az_permission p
WHERE p.del = 0
  AND (
      p.target_identifier LIKE '/authz/%'
      OR p.code IN (
          'POST:/sys/admin/page',
          'POST:/sys/admin/create',
          'POST:/sys/admin/update',
          'POST:/sys/admin/roles/save',
          'POST:/sys/admin/remove/{id}',
          'POST:/sys/i18n-message/values/{messageKey}',
          'POST:/sys/i18n-message/save'
      )
  )
  AND @rbac_manager_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM az_role_permission rp
      WHERE rp.role_id = @rbac_manager_role_id
        AND rp.permission_id = p.id
        AND rp.del = 0
  );

DROP TEMPORARY TABLE IF EXISTS rbac_admin_menu_seed;
CREATE TEMPORARY TABLE rbac_admin_menu_seed (
    name varchar(255) NOT NULL,
    parent_name varchar(255) NOT NULL,
    type varchar(16) NOT NULL,
    access_code varchar(255) DEFAULT NULL,
    path varchar(500) DEFAULT NULL,
    component varchar(255) DEFAULT NULL,
    icon varchar(255) DEFAULT NULL,
    menu_order int NOT NULL,
    title varchar(255) NOT NULL,
    PRIMARY KEY (name)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO rbac_admin_menu_seed
    (name, parent_name, type, access_code, path, component, icon, menu_order, title)
VALUES
    ('Admin', 'System', 'menu', NULL, '/system/admin', '/system/admin/index', 'lucide:user-cog', 20, 'menu.system.admin.title'),
    ('Role', 'System', 'menu', NULL, '/system/role', '/system/role/index', 'lucide:users-round', 30, 'menu.system.role.title'),
    ('Permission', 'System', 'menu', NULL, '/system/permission', '/system/permission/index', 'lucide:key-round', 40, 'menu.system.permission.title'),
    ('Menu', 'System', 'menu', NULL, '/system/menu', '/system/menu/index', 'lucide:list-tree', 50, 'menu.system.menu.title');

INSERT INTO az_menu (
    del, create_time, update_time, pid, type, status, access_code, name, path,
    component, affix_tab, hide_in_menu, icon, `order`, title, built_in
)
SELECT
    0, UTC_TIMESTAMP(6), NULL, parent.id, seed.type, 1, seed.access_code, seed.name, seed.path,
    seed.component, 0, 0, seed.icon, seed.menu_order, seed.title, 1
FROM rbac_admin_menu_seed seed
JOIN az_menu parent ON parent.name = seed.parent_name AND parent.del = 0
WHERE NOT EXISTS (
    SELECT 1 FROM az_menu current_menu WHERE current_menu.name = seed.name
);

INSERT INTO rbac_admin_menu_seed
    (name, parent_name, type, access_code, path, component, icon, menu_order, title)
VALUES
    ('AdminCreate', 'Admin', 'button', 'system:admin:create', NULL, NULL, NULL, 1, 'menu.system.admin.create'),
    ('AdminUpdate', 'Admin', 'button', 'system:admin:update', NULL, NULL, NULL, 2, 'menu.system.admin.update'),
    ('AdminRemove', 'Admin', 'button', 'system:admin:remove', NULL, NULL, NULL, 3, 'menu.system.admin.remove'),
    ('AdminRole', 'Admin', 'button', 'system:admin:manage-roles', NULL, NULL, NULL, 4, 'menu.system.admin.role'),
    ('RoleCreate', 'Role', 'button', 'system:role:create', NULL, NULL, NULL, 1, 'menu.system.role.create'),
    ('RoleUpdate', 'Role', 'button', 'system:role:update', NULL, NULL, NULL, 2, 'menu.system.role.update'),
    ('RoleRemove', 'Role', 'button', 'system:role:remove', NULL, NULL, NULL, 3, 'menu.system.role.remove'),
    ('RolePermission', 'Role', 'button', 'system:role:manage-permissions', NULL, NULL, NULL, 4, 'menu.system.role.permission'),
    ('RoleMenu', 'Role', 'button', 'system:role:manage-menus', NULL, NULL, NULL, 5, 'menu.system.role.menu'),
    ('PermissionCreate', 'Permission', 'button', 'system:permission:create', NULL, NULL, NULL, 1, 'menu.system.permission.create'),
    ('PermissionUpdate', 'Permission', 'button', 'system:permission:update', NULL, NULL, NULL, 2, 'menu.system.permission.update'),
    ('PermissionRemove', 'Permission', 'button', 'system:permission:remove', NULL, NULL, NULL, 3, 'menu.system.permission.remove'),
    ('MenuCreate', 'Menu', 'button', 'system:menu:create', NULL, NULL, NULL, 1, 'menu.system.menu.create'),
    ('MenuUpdate', 'Menu', 'button', 'system:menu:update', NULL, NULL, NULL, 2, 'menu.system.menu.update'),
    ('MenuRemove', 'Menu', 'button', 'system:menu:remove', NULL, NULL, NULL, 3, 'menu.system.menu.remove');

INSERT INTO az_menu (
    del, create_time, update_time, pid, type, status, access_code, name, path,
    component, affix_tab, hide_in_menu, `order`, title, built_in
)
SELECT
    0, UTC_TIMESTAMP(6), NULL, parent.id, seed.type, 1, seed.access_code, seed.name, seed.path,
    seed.component, 0, 0, seed.menu_order, seed.title, 1
FROM rbac_admin_menu_seed seed
JOIN az_menu parent ON parent.name = seed.parent_name AND parent.del = 0
WHERE seed.type = 'button'
  AND NOT EXISTS (
      SELECT 1 FROM az_menu current_menu WHERE current_menu.name = seed.name
  );

UPDATE az_menu current_menu
JOIN rbac_admin_menu_seed seed ON seed.name = current_menu.name
JOIN az_menu parent ON parent.name = seed.parent_name AND parent.del = 0
SET current_menu.del = 0,
    current_menu.pid = parent.id,
    current_menu.type = seed.type,
    current_menu.status = 1,
    current_menu.access_code = seed.access_code,
    current_menu.path = seed.path,
    current_menu.component = seed.component,
    current_menu.icon = seed.icon,
    current_menu.`order` = seed.menu_order,
    current_menu.title = seed.title,
    current_menu.built_in = 1,
    current_menu.update_time = UTC_TIMESTAMP(6);

UPDATE az_menu
SET built_in = 1,
    update_time = UTC_TIMESTAMP(6)
WHERE name IN (
    'Dashboard',
    'Profile',
    'System',
    'I18nMessage',
    'I18nMessageSave',
    'I18nMessageRemove'
)
  AND del = 0;

INSERT INTO az_role_menu (del, create_time, update_time, role_id, menu_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @rbac_manager_role_id, menu.id
FROM az_menu menu
WHERE menu.del = 0
  AND (menu.name = 'System' OR menu.name IN (SELECT name FROM rbac_admin_menu_seed))
  AND @rbac_manager_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM az_role_menu rm
      WHERE rm.role_id = @rbac_manager_role_id
        AND rm.menu_id = menu.id
        AND rm.del = 0
  );

DROP TEMPORARY TABLE IF EXISTS rbac_admin_i18n_seed;
CREATE TEMPORARY TABLE rbac_admin_i18n_seed (
    message_key varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    locale varchar(20) NOT NULL,
    i18n_value text NOT NULL,
    PRIMARY KEY (message_key, locale)
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO rbac_admin_i18n_seed (message_key, locale, i18n_value)
VALUES
    ('menu.system.admin.title', 'zh-CN', '后台管理员'),
    ('menu.system.admin.title', 'en-US', 'Administrators'),
    ('menu.system.admin.create', 'zh-CN', '新增后台管理员'),
    ('menu.system.admin.create', 'en-US', 'Create administrator'),
    ('menu.system.admin.update', 'zh-CN', '修改后台管理员'),
    ('menu.system.admin.update', 'en-US', 'Update administrator'),
    ('menu.system.admin.remove', 'zh-CN', '删除后台管理员'),
    ('menu.system.admin.remove', 'en-US', 'Delete administrator'),
    ('menu.system.admin.role', 'zh-CN', '分配角色'),
    ('menu.system.admin.role', 'en-US', 'Assign roles'),
    ('menu.system.role.title', 'zh-CN', '角色管理'),
    ('menu.system.role.title', 'en-US', 'Roles'),
    ('menu.system.role.create', 'zh-CN', '新增角色'),
    ('menu.system.role.create', 'en-US', 'Create role'),
    ('menu.system.role.update', 'zh-CN', '修改角色'),
    ('menu.system.role.update', 'en-US', 'Update role'),
    ('menu.system.role.remove', 'zh-CN', '删除角色'),
    ('menu.system.role.remove', 'en-US', 'Delete role'),
    ('menu.system.role.permission', 'zh-CN', '分配权限'),
    ('menu.system.role.permission', 'en-US', 'Assign permissions'),
    ('menu.system.role.menu', 'zh-CN', '菜单授权'),
    ('menu.system.role.menu', 'en-US', 'Assign menus'),
    ('menu.system.permission.title', 'zh-CN', '权限管理'),
    ('menu.system.permission.title', 'en-US', 'Permissions'),
    ('menu.system.permission.create', 'zh-CN', '新增权限'),
    ('menu.system.permission.create', 'en-US', 'Create permission'),
    ('menu.system.permission.update', 'zh-CN', '修改权限'),
    ('menu.system.permission.update', 'en-US', 'Update permission'),
    ('menu.system.permission.remove', 'zh-CN', '删除权限'),
    ('menu.system.permission.remove', 'en-US', 'Delete permission'),
    ('menu.system.menu.title', 'zh-CN', '菜单管理'),
    ('menu.system.menu.title', 'en-US', 'Menus'),
    ('menu.system.menu.create', 'zh-CN', '新增菜单'),
    ('menu.system.menu.create', 'en-US', 'Create menu'),
    ('menu.system.menu.update', 'zh-CN', '修改菜单'),
    ('menu.system.menu.update', 'en-US', 'Update menu'),
    ('menu.system.menu.remove', 'zh-CN', '删除菜单'),
    ('menu.system.menu.remove', 'en-US', 'Delete menu');

INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT COALESCE((
           SELECT current_category.category
           FROM sys_i18n current_category
           WHERE current_category.message_key = seed.message_key
           LIMIT 1
       ), 'admin'),
       seed.message_key,
       seed.locale,
       seed.i18n_value,
       UTC_TIMESTAMP(6)
FROM rbac_admin_i18n_seed seed
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_i18n current_message
    WHERE current_message.message_key = seed.message_key
      AND current_message.locale COLLATE utf8mb4_unicode_ci = seed.locale
);

INSERT INTO az_user_role (del, create_time, update_time, user_id, role_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @default_admin_user_id, @rbac_manager_role_id
WHERE @default_admin_user_id IS NOT NULL
  AND @rbac_manager_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM az_user_role ur
      WHERE ur.user_id = @default_admin_user_id
        AND ur.role_id = @rbac_manager_role_id
        AND ur.del = 0
  );

DROP TEMPORARY TABLE IF EXISTS rbac_admin_i18n_seed;
DROP TEMPORARY TABLE IF EXISTS rbac_admin_menu_seed;
