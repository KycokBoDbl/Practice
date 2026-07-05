UPDATE users
SET email = 'landlord@test.com',
    email_normalized = 'landlord@test.com'
WHERE email_normalized = 'landlord@test';

UPDATE users
SET email = 'tenant@test.com',
    email_normalized = 'tenant@test.com'
WHERE email_normalized = 'tenant@test';
