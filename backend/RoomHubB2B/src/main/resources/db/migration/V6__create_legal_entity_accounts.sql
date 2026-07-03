CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    legal_name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_organizations_tax_id UNIQUE (tax_id),
    CONSTRAINT chk_organizations_legal_name_not_blank CHECK (btrim(legal_name) <> ''),
    CONSTRAINT chk_organizations_tax_id_format CHECK (tax_id ~ '^[0-9]{10}$')
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    email VARCHAR(320) NOT NULL,
    email_normalized VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT uk_users_organization UNIQUE (organization_id),
    CONSTRAINT uk_users_email_normalized UNIQUE (email_normalized),
    CONSTRAINT chk_users_email_not_blank CHECK (btrim(email) <> ''),
    CONSTRAINT chk_users_email_normalized CHECK (
        email_normalized = lower(btrim(email_normalized))
        AND btrim(email_normalized) <> ''
    ),
    CONSTRAINT chk_users_password_hash_not_blank CHECK (btrim(password_hash) <> ''),
    CONSTRAINT chk_users_role CHECK (role IN ('LANDLORD', 'TENANT'))
);
