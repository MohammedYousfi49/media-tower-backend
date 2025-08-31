-- ============================
-- Table USERS
-- ============================
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                     email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    uid VARCHAR(255) UNIQUE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone_number VARCHAR(255),
    address TEXT,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    preferred_language VARCHAR(10),
    fcm_token VARCHAR(255),
    avatar_url VARCHAR(500),
    email_verified BOOLEAN DEFAULT FALSE,
    verification_token VARCHAR(255),
    verification_token_expiry_date TIMESTAMP,
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    temp_mfa_secret VARCHAR(255)
    );

-- ============================
-- Table CONTENT
-- ============================
CREATE TABLE IF NOT EXISTS content (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                       slug VARCHAR(255) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS content_titles (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              content_id BIGINT NOT NULL,
                                              language_code VARCHAR(10) NOT NULL,
    title VARCHAR(255) NOT NULL,
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS content_bodies (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              content_id BIGINT NOT NULL,
                                              language_code VARCHAR(10) NOT NULL,
    body TEXT NOT NULL,
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE
    );

-- ============================
-- Table SETTINGS
-- ============================
CREATE TABLE IF NOT EXISTS settings (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        setting_key VARCHAR(255) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL
    );

-- ============================
-- Table ORDERS
-- ============================
CREATE TABLE IF NOT EXISTS orders (
                                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      user_id BIGINT NOT NULL,
                                      order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      status VARCHAR(50),
    total_amount DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- ============================
-- Table BOOKINGS
-- ============================
CREATE TABLE IF NOT EXISTS bookings (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        user_id BIGINT NOT NULL,
                                        service_id BIGINT,
                                        assigned_admin_id BIGINT,
                                        status VARCHAR(50),
    payment_due_date DATE,
    customer_notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
