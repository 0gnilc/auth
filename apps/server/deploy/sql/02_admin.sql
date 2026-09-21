-- 系统后台管理员初始化脚本。
-- 依赖先执行 01_rbac.sql。
-- 包含 sys_admin 表结构、默认管理员账号和 RBAC 绑定数据。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_admin (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '管理员资料主键 sys_admin.id',
    del tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记：0 表示有效，1 表示已删除',
    create_time datetime(6) NOT NULL COMMENT '记录创建时间，使用 UTC 时间点',
    update_time datetime(6) DEFAULT NULL COMMENT '记录最近更新时间，使用 UTC 时间点；尚未更新时可为空',
    user_id bigint NOT NULL COMMENT '对应的 RBAC 全局用户主键 az_user.id',
    username varchar(320) NOT NULL COMMENT '管理员登录用户名',
    password varchar(100) NOT NULL COMMENT '管理员登录密码的 BCrypt 哈希',
    nickname varchar(255) NOT NULL COMMENT '展示昵称',
    avatar varchar(500) DEFAULT NULL COMMENT '可选头像 URL；NULL 表示未设置',
    description varchar(500) DEFAULT NULL COMMENT '可选的管理员说明',
    home_path varchar(500) NOT NULL DEFAULT '/dashboard' COMMENT '管理员登录后的默认首页路径，默认为 /dashboard',
    status tinyint(1) NOT NULL DEFAULT '1' COMMENT '管理员账户启用状态：0 停用、1 启用',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员资料与登录凭据';

ALTER TABLE sys_admin
    MODIFY COLUMN username varchar(320) NOT NULL COMMENT '管理员登录用户名';
ALTER TABLE sys_admin ALTER COLUMN home_path SET DEFAULT '/dashboard';

UPDATE az_role
SET del = 0,
    name = 'Admin 基础访问',
    remark = 'Admin User 基础访问',
    built_in = 1
WHERE code = 'admin'
  AND (del <> 0 OR built_in <> 1
       OR name <> 'Admin 基础访问'
       OR remark <> 'Admin User 基础访问');

INSERT INTO az_role (del, create_time, update_time, code, name, remark, built_in)
SELECT 0, UTC_TIMESTAMP(6), NULL, 'admin', 'Admin 基础访问',
       'Admin User 基础访问', 1
WHERE NOT EXISTS (
    SELECT 1
    FROM az_role
    WHERE code = 'admin'
);

SET @default_admin_existing_user_id := (
    SELECT user_id
    FROM sys_admin
    WHERE username = 'admin'
    LIMIT 1
);

-- 仅完整初始化链路首次创建默认管理员时，由后续脚本授予管理能力。
SET @default_admin_created := @default_admin_existing_user_id IS NULL;

INSERT INTO az_user (id, del, create_time, update_time)
SELECT @default_admin_existing_user_id, 0, UTC_TIMESTAMP(6), NULL
WHERE @default_admin_existing_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM az_user
      WHERE id = @default_admin_existing_user_id
  );

INSERT INTO az_user (del, create_time, update_time)
SELECT 0, UTC_TIMESTAMP(6), NULL
WHERE @default_admin_existing_user_id IS NULL;

SET @default_admin_user_id := COALESCE(@default_admin_existing_user_id, LAST_INSERT_ID());

UPDATE az_user
SET del = 0
WHERE id = @default_admin_user_id
  AND del <> 0;

INSERT INTO sys_admin (
    del,
    create_time,
    update_time,
    user_id,
    username,
    password,
    nickname,
    avatar,
    description,
    home_path,
    status
)
SELECT
    0,
    UTC_TIMESTAMP(6),
    NULL,
    @default_admin_user_id,
    'admin',
    '$2y$10$vjUNB/mAmPcweognGYbnyOeeQQzjL5DCQeThxucH1pC6nJfskup7G',
    '管理员',
    NULL,
    '系统管理员',
    '/dashboard',
    1
WHERE @default_admin_existing_user_id IS NULL;

UPDATE sys_admin
SET del = 0,
    status = 1
WHERE username = 'admin'
  AND (del <> 0 OR status <> 1);

SET @default_admin_role_id := (
    SELECT id
    FROM az_role
    WHERE code = 'admin'
      AND del = 0
    LIMIT 1
);

INSERT INTO az_menu (
    del, create_time, update_time, pid, type, status, name, path, component,
    affix_tab, hide_in_menu, icon, `order`, title
)
SELECT
    0, UTC_TIMESTAMP(6), NULL, 0, 'menu', 1, 'Dashboard', '/dashboard', '/dashboard/index',
    1, 0, 'lucide:layout-dashboard', -1, 'dashboard.title'
WHERE NOT EXISTS (
    SELECT 1 FROM az_menu WHERE name = 'Dashboard'
);

INSERT INTO az_menu (
    del, create_time, update_time, pid, type, status, name, path, component,
    affix_tab, hide_in_menu, icon, `order`, title
)
SELECT
    0, UTC_TIMESTAMP(6), NULL, 0, 'menu', 1, 'Profile', '/profile', '/_core/profile/index',
    0, 1, 'lucide:user', 999, 'auth.profile'
WHERE NOT EXISTS (
    SELECT 1 FROM az_menu WHERE name = 'Profile'
);

INSERT INTO az_role_menu (del, create_time, update_time, role_id, menu_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @default_admin_role_id, m.id
FROM az_menu m
WHERE m.name IN ('Dashboard', 'Profile')
  AND m.del = 0
  AND @default_admin_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM az_role_menu rm
      WHERE rm.role_id = @default_admin_role_id
        AND rm.menu_id = m.id
        AND rm.del = 0
  );

INSERT INTO az_user_role (del, create_time, update_time, user_id, role_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, @default_admin_user_id, @default_admin_role_id
WHERE @default_admin_user_id IS NOT NULL
  AND @default_admin_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM az_user_role
      WHERE user_id = @default_admin_user_id
        AND role_id = @default_admin_role_id
        AND del = 0
  );

-- 每个有效管理员身份保留内置 admin 基础访问角色。
INSERT INTO az_user_role (del, create_time, update_time, user_id, role_id)
SELECT 0, UTC_TIMESTAMP(6), NULL, a.user_id, @default_admin_role_id
FROM sys_admin a
WHERE a.del = 0
  AND @default_admin_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM az_user_role ur
      WHERE ur.user_id = a.user_id
        AND ur.role_id = @default_admin_role_id
        AND ur.del = 0
  );
