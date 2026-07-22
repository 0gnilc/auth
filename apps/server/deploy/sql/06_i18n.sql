-- 系统动态国际化消息表、默认数据、菜单和权限。
-- 依赖依次执行 01_rbac.sql 和 02_admin.sql。

CREATE TABLE IF NOT EXISTS sys_i18n (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    client varchar(64) NOT NULL COMMENT '客户端标识，例如 admin',
    i18n_key varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '国际化 key，大小写敏感',
    locale varchar(20) NOT NULL COMMENT '语言代码，例如 zh-CN',
    i18n_value text NOT NULL COMMENT '翻译值',
    create_time datetime NOT NULL COMMENT '创建时间',
    update_time datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_i18n_key_locale_client (i18n_key, locale, client),
    KEY idx_i18n_key_client (i18n_key, client),
    KEY idx_client_locale_key (client, locale, i18n_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端国际化消息';

UPDATE sys_i18n AS old_message
LEFT JOIN sys_i18n AS current_message
  ON current_message.client = old_message.client
 AND current_message.locale = old_message.locale
 AND current_message.i18n_key = 'menu.i18nMessage.title'
SET old_message.i18n_key = 'menu.i18nMessage.title',
    old_message.update_time = NOW()
WHERE old_message.i18n_key = 'menu.i18n.title'
  AND old_message.client = 'admin'
  AND current_message.id IS NULL;

DELETE FROM sys_i18n
WHERE client = 'admin'
  AND i18n_key = 'menu.i18n.title';

UPDATE az_menu
SET title = 'menu.dashboard.title',
    update_time = NOW()
WHERE name = 'Dashboard'
  AND del = 0;

UPDATE az_menu
SET title = 'menu.profile.title',
    update_time = NOW()
WHERE name = 'Profile'
  AND del = 0;

INSERT INTO sys_i18n (client, i18n_key, locale, i18n_value, create_time)
SELECT 'admin', 'menu.dashboard.title', 'zh-CN', '首页', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE client = 'admin' AND i18n_key = 'menu.dashboard.title' AND locale = 'zh-CN'
);
INSERT INTO sys_i18n (client, i18n_key, locale, i18n_value, create_time)
SELECT 'admin', 'menu.dashboard.title', 'en-US', 'Dashboard', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE client = 'admin' AND i18n_key = 'menu.dashboard.title' AND locale = 'en-US'
);
INSERT INTO sys_i18n (client, i18n_key, locale, i18n_value, create_time)
SELECT 'admin', 'menu.profile.title', 'zh-CN', '个人中心', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE client = 'admin' AND i18n_key = 'menu.profile.title' AND locale = 'zh-CN'
);
INSERT INTO sys_i18n (client, i18n_key, locale, i18n_value, create_time)
SELECT 'admin', 'menu.profile.title', 'en-US', 'Profile', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE client = 'admin' AND i18n_key = 'menu.profile.title' AND locale = 'en-US'
);
INSERT INTO sys_i18n (client, i18n_key, locale, i18n_value, create_time)
SELECT 'admin', 'menu.system.title', 'zh-CN', '系统管理', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE client = 'admin' AND i18n_key = 'menu.system.title' AND locale = 'zh-CN'
);
INSERT INTO sys_i18n (client, i18n_key, locale, i18n_value, create_time)
SELECT 'admin', 'menu.system.title', 'en-US', 'System', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE client = 'admin' AND i18n_key = 'menu.system.title' AND locale = 'en-US'
);
INSERT INTO sys_i18n (client, i18n_key, locale, i18n_value, create_time)
SELECT 'admin', 'menu.i18nMessage.title', 'zh-CN', '国际化消息管理', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE client = 'admin' AND i18n_key = 'menu.i18nMessage.title' AND locale = 'zh-CN'
);
INSERT INTO sys_i18n (client, i18n_key, locale, i18n_value, create_time)
SELECT 'admin', 'menu.i18nMessage.title', 'en-US', 'I18n Messages', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_i18n
    WHERE client = 'admin' AND i18n_key = 'menu.i18nMessage.title' AND locale = 'en-US'
);

UPDATE az_role
SET del = 0,
    built_in = 1
WHERE code = 'i18n-manager'
  AND (del <> 0 OR built_in <> 1);

INSERT INTO az_role (del, create_time, update_time, code, name, remark, built_in)
SELECT 0, NOW(), NULL, 'i18n-manager', '国际化配置管理员', '维护客户端动态国际化配置', 1
WHERE NOT EXISTS (
    SELECT 1 FROM az_role WHERE code = 'i18n-manager'
);

INSERT INTO az_menu (
    del, create_time, update_time, pid, type, status, name, path, component,
    affix_tab, hide_in_menu, icon, `order`, title
)
SELECT
    0, NOW(), NULL, 0, 'catalog', 1, 'System', '/system', 'BasicLayout',
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
    update_time = NOW()
WHERE name = 'System';

SET @system_menu_id := (
    SELECT id FROM az_menu WHERE name = 'System' AND del = 0 LIMIT 1
);

INSERT INTO az_menu (
    del, create_time, update_time, pid, type, status, name, path, component,
    affix_tab, hide_in_menu, icon, `order`, title
)
SELECT
    0, NOW(), NULL, @system_menu_id, 'menu', 1, 'I18nMessage', '/system/i18n-message', '/system/i18n-message/index',
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
    update_time = NOW()
WHERE name IN ('I18n', 'I18nMessage')
  AND @system_menu_id IS NOT NULL;

UPDATE az_permission
SET code = 'POST:/sys/i18n-message/bundle',
    name = 'POST:/sys/i18n-message/bundle',
    target_identifier = '/sys/i18n-message/bundle'
WHERE code = 'POST:/sys/i18n/bundle';

UPDATE az_permission
SET code = 'POST:/sys/i18n-message/page',
    name = 'POST:/sys/i18n-message/page',
    target_identifier = '/sys/i18n-message/page'
WHERE code = 'POST:/sys/i18n/page';

UPDATE az_permission
SET code = 'POST:/sys/i18n-message/values/{i18nKey}',
    name = 'POST:/sys/i18n-message/values/{i18nKey}',
    target_identifier = '/sys/i18n-message/values/{i18nKey}'
WHERE code IN ('POST:/sys/i18n/values', 'POST:/sys/i18n/values/{i18nKey}');

UPDATE az_permission
SET code = 'POST:/sys/i18n-message/save',
    name = 'POST:/sys/i18n-message/save',
    target_identifier = '/sys/i18n-message/save'
WHERE code = 'POST:/sys/i18n/save';

UPDATE az_permission
SET code = 'POST:/sys/i18n-message/remove/{i18nKey}',
    name = 'POST:/sys/i18n-message/remove/{i18nKey}',
    target_identifier = '/sys/i18n-message/remove/{i18nKey}'
WHERE code IN ('POST:/sys/i18n/remove', 'POST:/sys/i18n/remove/{i18nKey}');

INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access)
SELECT NOW(), 'POST:/sys/i18n-message/bundle', 'POST:/sys/i18n-message/bundle', '/sys/i18n-message/bundle', 'POST', 0
WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/i18n-message/bundle');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access)
SELECT NOW(), 'POST:/sys/i18n-message/page', 'POST:/sys/i18n-message/page', '/sys/i18n-message/page', 'POST', 0
WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/i18n-message/page');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access)
SELECT NOW(), 'POST:/sys/i18n-message/values/{i18nKey}', 'POST:/sys/i18n-message/values/{i18nKey}', '/sys/i18n-message/values/{i18nKey}', 'POST', 0
WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/i18n-message/values/{i18nKey}');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access)
SELECT NOW(), 'POST:/sys/i18n-message/save', 'POST:/sys/i18n-message/save', '/sys/i18n-message/save', 'POST', 0
WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/i18n-message/save');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access)
SELECT NOW(), 'POST:/sys/i18n-message/remove/{i18nKey}', 'POST:/sys/i18n-message/remove/{i18nKey}', '/sys/i18n-message/remove/{i18nKey}', 'POST', 0
WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/i18n-message/remove/{i18nKey}');

UPDATE az_permission
SET public_access = 0
WHERE code IN (
    'POST:/sys/i18n-message/bundle',
    'POST:/sys/i18n-message/page',
    'POST:/sys/i18n-message/values/{i18nKey}',
    'POST:/sys/i18n-message/save',
    'POST:/sys/i18n-message/remove/{i18nKey}'
);

SET @admin_role_id := (
    SELECT id FROM az_role WHERE code = 'admin' AND del = 0 LIMIT 1
);
SET @i18n_manager_role_id := (
    SELECT id FROM az_role WHERE code = 'i18n-manager' AND del = 0 LIMIT 1
);
SET @default_admin_user_id := (
    SELECT user_id FROM sys_admin WHERE username = 'admin' AND del = 0 LIMIT 1
);
INSERT INTO az_role_permission (del, create_time, update_time, role_id, permission_id)
SELECT 0, NOW(), NULL, @admin_role_id, p.id
FROM az_permission p
WHERE p.code = 'POST:/sys/i18n-message/bundle'
  AND @admin_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_role_permission rp
      WHERE rp.role_id = @admin_role_id AND rp.permission_id = p.id AND rp.del = 0
  );

INSERT INTO az_role_permission (del, create_time, update_time, role_id, permission_id)
SELECT 0, NOW(), NULL, @i18n_manager_role_id, p.id
FROM az_permission p
WHERE p.code IN (
    'POST:/sys/i18n-message/page',
    'POST:/sys/i18n-message/values/{i18nKey}',
    'POST:/sys/i18n-message/save',
    'POST:/sys/i18n-message/remove/{i18nKey}'
)
  AND @i18n_manager_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_role_permission rp
      WHERE rp.role_id = @i18n_manager_role_id AND rp.permission_id = p.id AND rp.del = 0
  );

INSERT INTO az_role_menu (del, create_time, update_time, role_id, menu_id)
SELECT 0, NOW(), NULL, @i18n_manager_role_id, m.id
FROM az_menu m
WHERE m.name IN ('System', 'I18nMessage')
  AND m.del = 0
  AND @i18n_manager_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_role_menu rm
      WHERE rm.role_id = @i18n_manager_role_id AND rm.menu_id = m.id AND rm.del = 0
  );

INSERT INTO az_user_role (del, create_time, update_time, user_id, role_id)
SELECT 0, NOW(), NULL, @default_admin_user_id, @i18n_manager_role_id
WHERE @default_admin_user_id IS NOT NULL
  AND @i18n_manager_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM az_user_role ur
      WHERE ur.user_id = @default_admin_user_id
        AND ur.role_id = @i18n_manager_role_id
        AND ur.del = 0
  );
