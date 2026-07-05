-- Demo credentials for local development:
-- landlord@test / demo-password
-- tenant@test / demo-password

INSERT INTO organizations (legal_name, tax_id)
VALUES
    ('Demo Landlord LLC', '1000000001'),
    ('Demo Tenant LLC', '1000000002')
ON CONFLICT (tax_id) DO NOTHING;

INSERT INTO users (organization_id, email, email_normalized, password_hash, role)
SELECT id, 'landlord@test', 'landlord@test',
       '{bcrypt}$2a$10$rlq2m3Qg9lzk0xfNmRk28eHKLst8KN5tqmNS7CMhJgsd6apSoz4p.',
       'LANDLORD'
FROM organizations
WHERE tax_id = '1000000001'
ON CONFLICT (email_normalized) DO NOTHING;

INSERT INTO users (organization_id, email, email_normalized, password_hash, role)
SELECT id, 'tenant@test', 'tenant@test',
       '{bcrypt}$2a$10$rlq2m3Qg9lzk0xfNmRk28eHKLst8KN5tqmNS7CMhJgsd6apSoz4p.',
       'TENANT'
FROM organizations
WHERE tax_id = '1000000002'
ON CONFLICT (email_normalized) DO NOTHING;

UPDATE listings
SET owner_organization_id = (
    SELECT id
    FROM organizations
    WHERE tax_id = '1000000001'
);
