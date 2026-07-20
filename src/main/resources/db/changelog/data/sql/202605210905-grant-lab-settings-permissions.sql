-- Grant lab-settings.read to every role that already holds lab-file.read.
-- DISTINCT + ON CONFLICT DO NOTHING make the statement idempotent on re-runs
-- (matches the precedent at data/sql/202605200905-grant-pdf-permission-to-lab-file-read-roles.sql).
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT rp.role_id,
       (SELECT id FROM permissions WHERE name = 'lab-settings.read')
FROM role_permissions rp
JOIN permissions p ON p.id = rp.permission_id
WHERE p.name = 'lab-file.read'
ON CONFLICT DO NOTHING;

-- Grant lab-settings.manage to the ADMIN role only.
-- (The seed-iam-catalog changeset names this role "ADMIN", not "ROLE_ADMIN" —
-- "ROLE_" is the secured-x authority-string prefix, not the persisted name.)
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT r.id,
       (SELECT id FROM permissions WHERE name = 'lab-settings.manage')
FROM roles r
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;
