-- 系统后台管理员模块 RequestMapping 权限初始化脚本。
-- 依赖先执行 01_rbac.sql。
-- 相同 code 已存在时保留原记录；当前管理员自助接口会统一收紧为非公开访问。

INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), 'GET:/sys/admin/menu/access-codes', 'GET:/sys/admin/menu/access-codes', '/sys/admin/menu/access-codes', 'GET', 1 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'GET:/sys/admin/menu/access-codes');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), 'GET:/sys/admin/role-codes', 'GET:/sys/admin/role-codes', '/sys/admin/role-codes', 'GET', 1 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'GET:/sys/admin/role-codes');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), 'GET:/sys/admin/user-info', 'GET:/sys/admin/user-info', '/sys/admin/user-info', 'GET', 1 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'GET:/sys/admin/user-info');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), 'POST:/sys/admin/create', 'POST:/sys/admin/create', '/sys/admin/create', 'POST', 1 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/admin/create');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), 'POST:/sys/admin/login', 'POST:/sys/admin/login', '/sys/admin/login', 'POST', 1 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/admin/login');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), 'POST:/sys/admin/logout', 'POST:/sys/admin/logout', '/sys/admin/logout', 'POST', 1 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/admin/logout');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), 'POST:/sys/admin/page', 'POST:/sys/admin/page', '/sys/admin/page', 'POST', 1 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/admin/page');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), 'POST:/sys/admin/password/update', 'POST:/sys/admin/password/update', '/sys/admin/password/update', 'POST', 0 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/admin/password/update');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), 'POST:/sys/admin/refresh', 'POST:/sys/admin/refresh', '/sys/admin/refresh', 'POST', 1 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/admin/refresh');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), 'POST:/sys/admin/remove/{id}', 'POST:/sys/admin/remove/{id}', '/sys/admin/remove/{id}', 'POST', 1 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/admin/remove/{id}');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), 'POST:/sys/admin/update', 'POST:/sys/admin/update', '/sys/admin/update', 'POST', 1 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/admin/update');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), 'POST:/sys/admin/update-roles', 'POST:/sys/admin/update-roles', '/sys/admin/update-roles', 'POST', 1 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/admin/update-roles');
INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), 'POST:/sys/admin/user-info/update', 'POST:/sys/admin/user-info/update', '/sys/admin/user-info/update', 'POST', 0 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = 'POST:/sys/admin/user-info/update');

UPDATE az_permission
SET public_access = 0
WHERE code IN (
    'GET:/sys/admin/menu/access-codes',
    'GET:/sys/admin/role-codes',
    'GET:/sys/admin/user-info',
    'POST:/sys/admin/password/update',
    'POST:/sys/admin/user-info/update'
);

INSERT INTO az_role_permission (del, create_time, update_time, role_id, permission_id)
SELECT 0, NOW(), NULL, r.id, p.id
FROM az_role r
JOIN az_permission p ON p.code IN (
    'GET:/sys/admin/menu/access-codes',
    'GET:/sys/admin/role-codes',
    'GET:/sys/admin/user-info',
    'POST:/sys/admin/password/update',
    'POST:/sys/admin/user-info/update'
)
WHERE r.code = 'admin'
  AND r.del = 0
  AND NOT EXISTS (
      SELECT 1
      FROM az_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
        AND rp.del = 0
  );
