-- Grant pdf.read-own-jobs to every role that already holds lab-file.read.
-- ON CONFLICT DO NOTHING keeps the changeset idempotent on re-runs.
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT rp.role_id,
       (SELECT id FROM permissions WHERE name = 'pdf.read-own-jobs')
FROM role_permissions rp
JOIN permissions p ON p.id = rp.permission_id
WHERE p.name = 'lab-file.read'
ON CONFLICT DO NOTHING;
