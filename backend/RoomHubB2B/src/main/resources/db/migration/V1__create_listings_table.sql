CREATE TABLE listings (
                          id BIGSERIAL PRIMARY KEY,
                          title VARCHAR(255) NOT NULL,
                          description TEXT,
                          city VARCHAR(100) NOT NULL,
                          address VARCHAR(255),
                          price_per_hour NUMERIC(10, 2) NOT NULL,
                          capacity INTEGER NOT NULL,
                          space_type VARCHAR(100) NOT NULL,
                          image_url TEXT,
                          status VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED',
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);