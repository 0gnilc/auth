-- 系统后台管理员初始化脚本。
-- 依赖先执行 01_rbac.sql。
-- 包含 sys_admin 表结构、默认管理员账号和 RBAC 绑定数据。

CREATE TABLE IF NOT EXISTS sys_admin (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    del tinyint NOT NULL DEFAULT '0' COMMENT '删除标记',
    create_time datetime NOT NULL COMMENT '创建时间',
    update_time datetime DEFAULT NULL COMMENT '更新时间',
    user_id bigint NOT NULL COMMENT 'RBAC 全局用户 ID',
    username varchar(255) NOT NULL COMMENT '登录用户名',
    password varchar(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    nickname varchar(255) NOT NULL COMMENT '昵称',
    avatar varchar(500) DEFAULT NULL COMMENT '头像地址',
    description varchar(500) DEFAULT NULL COMMENT '管理员描述',
    home_path varchar(500) NOT NULL DEFAULT '/workspace' COMMENT '默认首页路径',
    status tinyint(1) NOT NULL DEFAULT '1' COMMENT '启用状态',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台管理员';

UPDATE az_role
SET del = 0,
    built_in = 1
WHERE code = 'admin'
  AND (del <> 0 OR built_in <> 1);

INSERT INTO az_role (del, create_time, update_time, code, name, remark, built_in)
SELECT 0, NOW(), NULL, 'admin', '管理员', '系统管理员', 1
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

INSERT INTO az_user (id, del, create_time, update_time)
SELECT @default_admin_existing_user_id, 0, NOW(), NULL
WHERE @default_admin_existing_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM az_user
      WHERE id = @default_admin_existing_user_id
  );

INSERT INTO az_user (del, create_time, update_time)
SELECT 0, NOW(), NULL
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
    NOW(),
    NULL,
    @default_admin_user_id,
    'admin',
    '$2y$10$vjUNB/mAmPcweognGYbnyOeeQQzjL5DCQeThxucH1pC6nJfskup7G',
    '管理员',
    NULL,
    '系统管理员',
    '/workspace',
    1
WHERE @default_admin_existing_user_id IS NULL;

UPDATE sys_admin
SET del = 0
WHERE username = 'admin'
  AND del <> 0;

SET @default_admin_role_id := (
    SELECT id
    FROM az_role
    WHERE code = 'admin'
      AND del = 0
    LIMIT 1
);

UPDATE az_user_role
SET del = 0,
    update_time = NOW()
WHERE user_id = @default_admin_user_id
  AND role_id = @default_admin_role_id
  AND del <> 0
ORDER BY id
LIMIT 1;

INSERT INTO az_user_role (del, create_time, update_time, user_id, role_id)
SELECT 0, NOW(), NULL, @default_admin_user_id, @default_admin_role_id
WHERE @default_admin_user_id IS NOT NULL
  AND @default_admin_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM az_user_role
      WHERE user_id = @default_admin_user_id
        AND role_id = @default_admin_role_id
        AND del = 0
  );
