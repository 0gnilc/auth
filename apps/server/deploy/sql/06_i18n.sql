-- 系统动态国际化消息表、默认数据、菜单和权限。
-- 依赖依次执行 01_rbac.sql 和 02_admin.sql。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_i18n (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    category varchar(64) NOT NULL COMMENT '消息分类，例如 default、admin',
    message_key varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '国际化 key，大小写敏感',
    locale varchar(20) NOT NULL COMMENT '语言代码，例如 zh-CN',
    i18n_value text NOT NULL COMMENT '翻译值',
    create_time datetime(6) NOT NULL COMMENT '创建时间（UTC）',
    update_time datetime(6) DEFAULT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_key_locale (message_key, locale),
    KEY idx_category_message_key (category, message_key),
    KEY idx_category_locale_key (category, locale, message_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态国际化消息';

SET @has_i18n_client_column := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_i18n'
      AND column_name = 'client'
);
SET @has_i18n_category_column := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_i18n'
      AND column_name = 'category'
);
SET @rename_i18n_client_sql := IF(
    @has_i18n_client_column = 1 AND @has_i18n_category_column = 0,
    'ALTER TABLE sys_i18n CHANGE COLUMN client category varchar(64) NOT NULL COMMENT ''消息分类，例如 default、admin''',
    'SELECT 1'
);
PREPARE rename_i18n_client_stmt FROM @rename_i18n_client_sql;
EXECUTE rename_i18n_client_stmt;
DEALLOCATE PREPARE rename_i18n_client_stmt;

-- The old client-scoped identity allowed duplicate Key/locale pairs. Prefer the
-- admin value, collapse the remainder deterministically, then map other clients
-- to the default category before creating the global unique index.
DELETE duplicate_message
FROM sys_i18n duplicate_message
JOIN sys_i18n keeper
  ON keeper.message_key = duplicate_message.message_key
 AND keeper.locale = duplicate_message.locale
 AND (
      (keeper.category = 'admin' AND duplicate_message.category <> 'admin')
      OR (
          (keeper.category = 'admin') = (duplicate_message.category = 'admin')
          AND keeper.id < duplicate_message.id
      )
 );

UPDATE sys_i18n current_message
JOIN (
    SELECT message_key, MAX(category = 'admin') AS has_admin
    FROM sys_i18n
    GROUP BY message_key
) message_category
  ON message_category.message_key = current_message.message_key
SET current_message.category = IF(message_category.has_admin = 1, 'admin', 'default');

SET @drop_i18n_old_unique_sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_i18n'
          AND index_name = 'uk_message_key_locale_client'
    ),
    'ALTER TABLE sys_i18n DROP INDEX uk_message_key_locale_client',
    'SELECT 1'
);
PREPARE drop_i18n_old_unique_stmt FROM @drop_i18n_old_unique_sql;
EXECUTE drop_i18n_old_unique_stmt;
DEALLOCATE PREPARE drop_i18n_old_unique_stmt;

SET @drop_i18n_old_key_sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_i18n'
          AND index_name = 'idx_message_key_client'
    ),
    'ALTER TABLE sys_i18n DROP INDEX idx_message_key_client',
    'SELECT 1'
);
PREPARE drop_i18n_old_key_stmt FROM @drop_i18n_old_key_sql;
EXECUTE drop_i18n_old_key_stmt;
DEALLOCATE PREPARE drop_i18n_old_key_stmt;

SET @drop_i18n_old_locale_sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_i18n'
          AND index_name = 'idx_client_locale_key'
    ),
    'ALTER TABLE sys_i18n DROP INDEX idx_client_locale_key',
    'SELECT 1'
);
PREPARE drop_i18n_old_locale_stmt FROM @drop_i18n_old_locale_sql;
EXECUTE drop_i18n_old_locale_stmt;
DEALLOCATE PREPARE drop_i18n_old_locale_stmt;

SET @add_i18n_unique_sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_i18n'
          AND index_name = 'uk_message_key_locale'
    ),
    'SELECT 1',
    'ALTER TABLE sys_i18n ADD UNIQUE KEY uk_message_key_locale (message_key, locale)'
);
PREPARE add_i18n_unique_stmt FROM @add_i18n_unique_sql;
EXECUTE add_i18n_unique_stmt;
DEALLOCATE PREPARE add_i18n_unique_stmt;

SET @add_i18n_category_key_sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_i18n'
          AND index_name = 'idx_category_message_key'
    ),
    'SELECT 1',
    'ALTER TABLE sys_i18n ADD KEY idx_category_message_key (category, message_key)'
);
PREPARE add_i18n_category_key_stmt FROM @add_i18n_category_key_sql;
EXECUTE add_i18n_category_key_stmt;
DEALLOCATE PREPARE add_i18n_category_key_stmt;

SET @add_i18n_locale_key_sql := IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_i18n'
          AND index_name = 'idx_category_locale_key'
    ),
    'SELECT 1',
    'ALTER TABLE sys_i18n ADD KEY idx_category_locale_key (category, locale, message_key)'
);
PREPARE add_i18n_locale_key_stmt FROM @add_i18n_locale_key_sql;
EXECUTE add_i18n_locale_key_stmt;
DEALLOCATE PREPARE add_i18n_locale_key_stmt;

UPDATE sys_i18n AS old_message
LEFT JOIN sys_i18n AS current_message
  ON current_message.category = old_message.category
 AND current_message.locale = old_message.locale
 AND current_message.message_key = 'menu.i18nMessage.title'
SET old_message.message_key = 'menu.i18nMessage.title',
    old_message.update_time = UTC_TIMESTAMP(6)
WHERE old_message.message_key = 'menu.i18n.title'
  AND old_message.category = 'admin'
  AND current_message.id IS NULL;

DELETE FROM sys_i18n
WHERE category = 'admin'
  AND message_key = 'menu.i18n.title';

UPDATE az_menu
SET title = 'menu.dashboard.title',
    update_time = UTC_TIMESTAMP(6)
WHERE name = 'Dashboard'
  AND del = 0;

UPDATE az_menu
SET title = 'menu.profile.title',
    update_time = UTC_TIMESTAMP(6)
WHERE name = 'Profile'
  AND del = 0;

INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT COALESCE((
    SELECT category FROM sys_i18n
    WHERE message_key = 'menu.dashboard.title'
    LIMIT 1
), 'admin'), 'menu.dashboard.title', 'zh-CN', '首页', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE message_key = 'menu.dashboard.title' AND locale = 'zh-CN'
);
INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT COALESCE((
    SELECT category FROM sys_i18n
    WHERE message_key = 'menu.dashboard.title'
    LIMIT 1
), 'admin'), 'menu.dashboard.title', 'en-US', 'Dashboard', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE message_key = 'menu.dashboard.title' AND locale = 'en-US'
);
INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT COALESCE((
    SELECT category FROM sys_i18n
    WHERE message_key = 'menu.profile.title'
    LIMIT 1
), 'admin'), 'menu.profile.title', 'zh-CN', '个人中心', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE message_key = 'menu.profile.title' AND locale = 'zh-CN'
);
INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT COALESCE((
    SELECT category FROM sys_i18n
    WHERE message_key = 'menu.profile.title'
    LIMIT 1
), 'admin'), 'menu.profile.title', 'en-US', 'Profile', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE message_key = 'menu.profile.title' AND locale = 'en-US'
);
INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT COALESCE((
    SELECT category FROM sys_i18n
    WHERE message_key = 'menu.system.title'
    LIMIT 1
), 'admin'), 'menu.system.title', 'zh-CN', '系统管理', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE message_key = 'menu.system.title' AND locale = 'zh-CN'
);
INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT COALESCE((
    SELECT category FROM sys_i18n
    WHERE message_key = 'menu.system.title'
    LIMIT 1
), 'admin'), 'menu.system.title', 'en-US', 'System', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE message_key = 'menu.system.title' AND locale = 'en-US'
);
INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT COALESCE((
    SELECT category FROM sys_i18n
    WHERE message_key = 'menu.i18nMessage.title'
    LIMIT 1
), 'admin'), 'menu.i18nMessage.title', 'zh-CN', '国际化消息管理', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE message_key = 'menu.i18nMessage.title' AND locale = 'zh-CN'
);
INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT COALESCE((
    SELECT category FROM sys_i18n
    WHERE message_key = 'menu.i18nMessage.title'
    LIMIT 1
), 'admin'), 'menu.i18nMessage.title', 'en-US', 'I18n Messages', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE message_key = 'menu.i18nMessage.title' AND locale = 'en-US'
);
INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT COALESCE((
    SELECT category FROM sys_i18n
    WHERE message_key = 'menu.i18nMessage.save'
    LIMIT 1
), 'admin'), 'menu.i18nMessage.save', 'zh-CN', '保存国际化消息', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE message_key = 'menu.i18nMessage.save' AND locale = 'zh-CN'
);
INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT COALESCE((
    SELECT category FROM sys_i18n
    WHERE message_key = 'menu.i18nMessage.save'
    LIMIT 1
), 'admin'), 'menu.i18nMessage.save', 'en-US', 'Save i18n message', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE message_key = 'menu.i18nMessage.save' AND locale = 'en-US'
);
INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT COALESCE((
    SELECT category FROM sys_i18n
    WHERE message_key = 'menu.i18nMessage.remove'
    LIMIT 1
), 'admin'), 'menu.i18nMessage.remove', 'zh-CN', '删除国际化消息', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE message_key = 'menu.i18nMessage.remove' AND locale = 'zh-CN'
);
INSERT INTO sys_i18n (category, message_key, locale, i18n_value, create_time)
SELECT COALESCE((
    SELECT category FROM sys_i18n
    WHERE message_key = 'menu.i18nMessage.remove'
    LIMIT 1
), 'admin'), 'menu.i18nMessage.remove', 'en-US', 'Remove i18n message', UTC_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE message_key = 'menu.i18nMessage.remove' AND locale = 'en-US'
);

-- 将旧角色标识迁移为统一的冒号格式，并在新旧角色并存时合并授权关系。
SET @legacy_i18n_manager_role_id := (
    SELECT id FROM az_role WHERE code = 'i18n-manager' AND del = 0 LIMIT 1
);
SET @i18n_manager_role_id := (
    SELECT id FROM az_role WHERE code = 'i18n:manager' AND del = 0 LIMIT 1
);

UPDATE az_role
SET code = 'i18n:manager',
    update_time = UTC_TIMESTAMP(6)
WHERE id = @legacy_i18n_manager_role_id
  AND @i18n_manager_role_id IS NULL;

SET @i18n_manager_role_id := (
    SELECT id FROM az_role WHERE code = 'i18n:manager' AND del = 0 LIMIT 1
);

INSERT INTO az_user_role (del, create_time, update_time, user_id, role_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, legacy_binding.user_id, @i18n_manager_role_id
FROM az_user_role legacy_binding
WHERE legacy_binding.role_id = @legacy_i18n_manager_role_id
  AND legacy_binding.del = 0
  AND @legacy_i18n_manager_role_id <> @i18n_manager_role_id
  AND NOT EXISTS (
      SELECT 1 FROM az_user_role current_binding
      WHERE current_binding.user_id = legacy_binding.user_id
        AND current_binding.role_id = @i18n_manager_role_id
        AND current_binding.del = 0
  );

INSERT INTO az_role_permission (del, create_time, update_time, role_id, permission_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @i18n_manager_role_id, legacy_binding.permission_id
FROM az_role_permission legacy_binding
WHERE legacy_binding.role_id = @legacy_i18n_manager_role_id
  AND legacy_binding.del = 0
  AND @legacy_i18n_manager_role_id <> @i18n_manager_role_id
  AND NOT EXISTS (
      SELECT 1 FROM az_role_permission current_binding
      WHERE current_binding.role_id = @i18n_manager_role_id
        AND current_binding.permission_id = legacy_binding.permission_id
        AND current_binding.del = 0
  );

INSERT INTO az_role_menu (del, create_time, update_time, role_id, menu_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @i18n_manager_role_id, legacy_binding.menu_id
FROM az_role_menu legacy_binding
WHERE legacy_binding.role_id = @legacy_i18n_manager_role_id
  AND legacy_binding.del = 0
  AND @legacy_i18n_manager_role_id <> @i18n_manager_role_id
  AND NOT EXISTS (
      SELECT 1 FROM az_role_menu current_binding
      WHERE current_binding.role_id = @i18n_manager_role_id
        AND current_binding.menu_id = legacy_binding.menu_id
        AND current_binding.del = 0
  );

UPDATE az_user_role
SET del = 1, update_time = UTC_TIMESTAMP(6)
WHERE role_id = @legacy_i18n_manager_role_id
  AND @legacy_i18n_manager_role_id <> @i18n_manager_role_id;
UPDATE az_role_permission
SET del = 1, update_time = UTC_TIMESTAMP(6)
WHERE role_id = @legacy_i18n_manager_role_id
  AND @legacy_i18n_manager_role_id <> @i18n_manager_role_id;
UPDATE az_role_menu
SET del = 1, update_time = UTC_TIMESTAMP(6)
WHERE role_id = @legacy_i18n_manager_role_id
  AND @legacy_i18n_manager_role_id <> @i18n_manager_role_id;
UPDATE az_role
SET del = 1, update_time = UTC_TIMESTAMP(6)
WHERE id = @legacy_i18n_manager_role_id
  AND @legacy_i18n_manager_role_id <> @i18n_manager_role_id;

UPDATE az_role
SET del = 0,
    built_in = 1,
    name = '国际化配置管理员',
    remark = '跨分类维护动态国际化配置',
    update_time = UTC_TIMESTAMP(6)
WHERE code = 'i18n:manager';

INSERT INTO az_role (del, create_time, update_time, code, name, remark, built_in)
SELECT 0, UTC_TIMESTAMP(6), NULL, 'i18n:manager', '国际化配置管理员', '跨分类维护动态国际化配置', 1
WHERE NOT EXISTS (
    SELECT 1 FROM az_role WHERE code = 'i18n:manager'
);

INSERT INTO az_menu (
    del, create_time, update_time, pid, type, status, name, path, component,
    affix_tab, hide_in_menu, icon, `order`, title
)
SELECT
    0, UTC_TIMESTAMP(6), NULL, 0, 'catalog', 1, 'System', '/system', 'BasicLayout',
    0, 0, 'lucide:settings', 100, 'menu.system.title'
WHERE NOT EXISTS (
    SELECT 1 FROM az_menu WHERE name = 'System'
);

UPDATE az_menu
SET del = 0,
    pid = 0,
    type = 'catalog',
    status = 1,
    path = '/system',
    component = 'BasicLayout',
    hide_in_menu = 0,
    icon = 'lucide:settings',
    `order` = 100,
    title = 'menu.system.title',
    update_time = UTC_TIMESTAMP(6)
WHERE name = 'System';

SET @system_menu_id := (
    SELECT id FROM az_menu WHERE name = 'System' AND del = 0 LIMIT 1
);

INSERT INTO az_menu (
    del, create_time, update_time, pid, type, status, name, path, component,
    affix_tab, hide_in_menu, icon, `order`, title
)
SELECT
    0, UTC_TIMESTAMP(6), NULL, @system_menu_id, 'menu', 1, 'I18nMessage', '/system/i18n-message', '/system/i18n-message/index',
    0, 0, 'lucide:languages', 10, 'menu.i18nMessage.title'
WHERE @system_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_menu WHERE name IN ('I18n', 'I18nMessage')
  );

UPDATE az_menu
SET name = 'I18nMessage',
    del = 0,
    pid = @system_menu_id,
    type = 'menu',
    status = 1,
    path = '/system/i18n-message',
    component = '/system/i18n-message/index',
    hide_in_menu = 0,
    icon = 'lucide:languages',
    `order` = 10,
    title = 'menu.i18nMessage.title',
    update_time = UTC_TIMESTAMP(6)
WHERE name IN ('I18n', 'I18nMessage')
  AND @system_menu_id IS NOT NULL;

SET @i18n_message_menu_id := (
    SELECT id FROM az_menu WHERE name = 'I18nMessage' AND del = 0 LIMIT 1
);

INSERT INTO az_menu (
    del, create_time, update_time, pid, type, status, access_code,
    name, `order`, title
)
SELECT
    0, UTC_TIMESTAMP(6), NULL, @i18n_message_menu_id, 'button', 1,
    'system:i18n-message:save', 'I18nMessageSave', 1,
    'menu.i18nMessage.save'
WHERE @i18n_message_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_menu WHERE name = 'I18nMessageSave'
  );

INSERT INTO az_menu (
    del, create_time, update_time, pid, type, status, access_code,
    name, `order`, title
)
SELECT
    0, UTC_TIMESTAMP(6), NULL, @i18n_message_menu_id, 'button', 1,
    'system:i18n-message:remove', 'I18nMessageRemove', 2,
    'menu.i18nMessage.remove'
WHERE @i18n_message_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_menu WHERE name = 'I18nMessageRemove'
  );

UPDATE az_menu
SET del = 0,
    pid = @i18n_message_menu_id,
    type = 'button',
    status = 1,
    access_code = 'system:i18n-message:save',
    `order` = 1,
    title = 'menu.i18nMessage.save',
    update_time = UTC_TIMESTAMP(6)
WHERE name = 'I18nMessageSave'
  AND @i18n_message_menu_id IS NOT NULL;

UPDATE az_menu
SET del = 0,
    pid = @i18n_message_menu_id,
    type = 'button',
    status = 1,
    access_code = 'system:i18n-message:remove',
    `order` = 2,
    title = 'menu.i18nMessage.remove',
    update_time = UTC_TIMESTAMP(6)
WHERE name = 'I18nMessageRemove'
  AND @i18n_message_menu_id IS NOT NULL;

UPDATE az_permission
SET code = 'POST:/sys/i18n-message/bundle/{category}',
    name = 'POST:/sys/i18n-message/bundle/{category}',
    target_identifier = '/sys/i18n-message/bundle/{category}'
WHERE code IN ('POST:/sys/i18n/bundle', 'POST:/sys/i18n-message/bundle');

UPDATE az_permission
SET code = 'POST:/sys/i18n-message/categories',
    name = 'POST:/sys/i18n-message/categories',
    target_identifier = '/sys/i18n-message/categories'
WHERE code = 'POST:/sys/i18n-message/clients';

UPDATE az_permission
SET code = 'POST:/sys/i18n-message/page',
    name = 'POST:/sys/i18n-message/page',
    target_identifier = '/sys/i18n-message/page'
WHERE code = 'POST:/sys/i18n/page';

UPDATE az_permission
SET code = 'POST:/sys/i18n-message/values/{messageKey}',
    name = 'POST:/sys/i18n-message/values/{messageKey}',
    target_identifier = '/sys/i18n-message/values/{messageKey}'
WHERE code = 'POST:/sys/i18n/values'
   OR code LIKE 'POST:/sys/i18n/values/{%'
   OR code LIKE 'POST:/sys/i18n-message/values/{%';

UPDATE az_permission
SET code = 'POST:/sys/i18n-message/save',
    name = 'POST:/sys/i18n-message/save',
    target_identifier = '/sys/i18n-message/save'
WHERE code = 'POST:/sys/i18n/save';

UPDATE az_permission
SET code = 'POST:/sys/i18n-message/remove/{messageKey}',
    name = 'POST:/sys/i18n-message/remove/{messageKey}',
    target_identifier = '/sys/i18n-message/remove/{messageKey}'
WHERE code = 'POST:/sys/i18n/remove'
   OR code LIKE 'POST:/sys/i18n/remove/{%'
   OR code LIKE 'POST:/sys/i18n-message/remove/{%';

INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access)
SELECT UTC_TIMESTAMP(6), 'POST:/sys/i18n-message/bundle/{category}', 'POST:/sys/i18n-message/bundle/{category}', '/sys/i18n-message/bundle/{category}', 'POST', 0
WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/i18n-message/bundle/{category}');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access)
SELECT UTC_TIMESTAMP(6), 'POST:/sys/i18n-message/page', 'POST:/sys/i18n-message/page', '/sys/i18n-message/page', 'POST', 0
WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/i18n-message/page');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access)
SELECT UTC_TIMESTAMP(6), 'POST:/sys/i18n-message/categories', 'POST:/sys/i18n-message/categories', '/sys/i18n-message/categories', 'POST', 0
WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/i18n-message/categories');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access)
SELECT UTC_TIMESTAMP(6), 'POST:/sys/i18n-message/values/{messageKey}', 'POST:/sys/i18n-message/values/{messageKey}', '/sys/i18n-message/values/{messageKey}', 'POST', 0
WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/i18n-message/values/{messageKey}');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access)
SELECT UTC_TIMESTAMP(6), 'POST:/sys/i18n-message/save', 'POST:/sys/i18n-message/save', '/sys/i18n-message/save', 'POST', 0
WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/i18n-message/save');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access)
SELECT UTC_TIMESTAMP(6), 'POST:/sys/i18n-message/remove/{messageKey}', 'POST:/sys/i18n-message/remove/{messageKey}', '/sys/i18n-message/remove/{messageKey}', 'POST', 0
WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/i18n-message/remove/{messageKey}');

UPDATE az_permission
SET public_access = 0
WHERE code IN (
    'POST:/sys/i18n-message/bundle/{category}',
    'POST:/sys/i18n-message/categories',
    'POST:/sys/i18n-message/page',
    'POST:/sys/i18n-message/values/{messageKey}',
    'POST:/sys/i18n-message/save',
    'POST:/sys/i18n-message/remove/{messageKey}'
);

SET @admin_role_id := (
    SELECT id FROM az_role WHERE code = 'admin' AND del = 0 LIMIT 1
);
SET @i18n_manager_role_id := (
    SELECT id FROM az_role WHERE code = 'i18n:manager' AND del = 0 LIMIT 1
);
SET @default_admin_user_id := (
    SELECT user_id FROM sys_admin WHERE username = 'admin' AND del = 0 LIMIT 1
);
INSERT INTO az_role_permission (del, create_time, update_time, role_id, permission_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @admin_role_id, p.id
FROM az_permission p
WHERE p.code = 'POST:/sys/i18n-message/bundle/{category}'
  AND @admin_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_role_permission rp
      WHERE rp.role_id = @admin_role_id AND rp.permission_id = p.id AND rp.del = 0
  );

INSERT INTO az_role_permission (del, create_time, update_time, role_id, permission_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @i18n_manager_role_id, p.id
FROM az_permission p
WHERE p.code IN (
    'POST:/sys/i18n-message/categories',
    'POST:/sys/i18n-message/page',
    'POST:/sys/i18n-message/values/{messageKey}',
    'POST:/sys/i18n-message/save',
    'POST:/sys/i18n-message/remove/{messageKey}'
)
  AND @i18n_manager_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_role_permission rp
      WHERE rp.role_id = @i18n_manager_role_id AND rp.permission_id = p.id AND rp.del = 0
  );

INSERT INTO az_role_menu (del, create_time, update_time, role_id, menu_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @i18n_manager_role_id, m.id
FROM az_menu m
WHERE m.name IN (
    'System',
    'I18nMessage',
    'I18nMessageSave',
    'I18nMessageRemove'
)
  AND m.del = 0
  AND @i18n_manager_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_role_menu rm
      WHERE rm.role_id = @i18n_manager_role_id AND rm.menu_id = m.id AND rm.del = 0
  );

INSERT INTO az_user_role (del, create_time, update_time, user_id, role_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @default_admin_user_id, @i18n_manager_role_id
WHERE @default_admin_user_id IS NOT NULL
  AND @i18n_manager_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_user_role ur
      WHERE ur.user_id = @default_admin_user_id
        AND ur.role_id = @i18n_manager_role_id
        AND ur.del = 0
  );
