-- Spring Boot 框架默认 RequestMapping 权限初始化脚本。
-- 依赖先执行 01_rbac.sql。
-- 相同 code 已存在时保持原记录不变。

INSERT INTO az_permission (create_time, code, name, target_identifier, target_qualifier, public_access) SELECT NOW(), '*:/error', '*:/error', '/error', '*', 1 WHERE NOT EXISTS (SELECT 1 FROM az_permission WHERE code = '*:/error');
