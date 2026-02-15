CREATE TABLE IF NOT EXISTS account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255),
    nickname VARCHAR(255),
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    joined_at TIMESTAMP,
    bio VARCHAR(255),
    url VARCHAR(255),
    location VARCHAR(255),
    profile_image TEXT,
    hobby_created_by_email BOOLEAN NOT NULL DEFAULT FALSE,
    hobby_created_by_web BOOLEAN NOT NULL DEFAULT TRUE,
    hobby_enrollment_result_by_email BOOLEAN NOT NULL DEFAULT FALSE,
    hobby_enrollment_result_by_web BOOLEAN NOT NULL DEFAULT TRUE,
    hobby_updated_by_email BOOLEAN NOT NULL DEFAULT FALSE,
    hobby_updated_by_web BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP,
    UNIQUE (provider, provider_id)
);

CREATE UNIQUE INDEX uq_account_nickname_active ON account (nickname) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS tag (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS zone (
    id BIGSERIAL PRIMARY KEY,
    country VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    local_name_of_city VARCHAR(255),
    province VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (city, province)
);

CREATE TABLE IF NOT EXISTS hobby (
    id BIGSERIAL PRIMARY KEY,
    path VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    short_description VARCHAR(255),
    full_description TEXT,
    image TEXT,
    published_date_time TIMESTAMP,
    closed_date_time TIMESTAMP,
    recruiting_updated_date_time TIMESTAMP,
    recruiting BOOLEAN NOT NULL DEFAULT FALSE,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    use_banner BOOLEAN NOT NULL DEFAULT FALSE,
    member_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX uq_hobby_path_active ON hobby (path) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_hobby_title_active ON hobby (title) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS event (
    id BIGSERIAL PRIMARY KEY,
    hobby_id BIGINT REFERENCES hobby(id),
    created_by_id UUID REFERENCES account(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    create_date_time TIMESTAMP NOT NULL,
    end_enrollment_date_time TIMESTAMP NOT NULL,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    limit_of_enrollments INT,
    event_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS enrollment (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT REFERENCES event(id),
    account_id UUID REFERENCES account(id),
    enrolled_at TIMESTAMP,
    accepted BOOLEAN NOT NULL DEFAULT FALSE,
    attended BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    link VARCHAR(255),
    message VARCHAR(255),
    checked BOOLEAN NOT NULL DEFAULT FALSE,
    account_id UUID REFERENCES account(id),
    notification_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Join tables (entity-based with surrogate keys)
CREATE TABLE IF NOT EXISTS account_tag (
    id BIGSERIAL PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES account(id),
    tag_id BIGINT NOT NULL REFERENCES tag(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (account_id, tag_id)
);

CREATE TABLE IF NOT EXISTS account_zone (
    id BIGSERIAL PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES account(id),
    zone_id BIGINT NOT NULL REFERENCES zone(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (account_id, zone_id)
);

CREATE TABLE IF NOT EXISTS hobby_manager (
    id BIGSERIAL PRIMARY KEY,
    hobby_id BIGINT NOT NULL REFERENCES hobby(id),
    account_id UUID NOT NULL REFERENCES account(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (hobby_id, account_id)
);

CREATE TABLE IF NOT EXISTS hobby_member (
    id BIGSERIAL PRIMARY KEY,
    hobby_id BIGINT NOT NULL REFERENCES hobby(id),
    account_id UUID NOT NULL REFERENCES account(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (hobby_id, account_id)
);

CREATE TABLE IF NOT EXISTS hobby_tag (
    id BIGSERIAL PRIMARY KEY,
    hobby_id BIGINT NOT NULL REFERENCES hobby(id),
    tag_id BIGINT NOT NULL REFERENCES tag(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (hobby_id, tag_id)
);

CREATE TABLE IF NOT EXISTS hobby_zone (
    id BIGSERIAL PRIMARY KEY,
    hobby_id BIGINT NOT NULL REFERENCES hobby(id),
    zone_id BIGINT NOT NULL REFERENCES zone(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (hobby_id, zone_id)
);

-- Refresh token table (Token Family Tracking)
CREATE TABLE refresh_token (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id       UUID         NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    token_hash       VARCHAR(64)  NOT NULL UNIQUE,
    family_id        VARCHAR(36)  NOT NULL,
    generation       INT          NOT NULL DEFAULT 0,
    device_info      VARCHAR(256),
    ip_address       VARCHAR(45),
    issued_at        TIMESTAMP    NOT NULL DEFAULT now(),
    expires_at       TIMESTAMP    NOT NULL,
    revoked          BOOLEAN      NOT NULL DEFAULT FALSE,
    replaced_by_hash VARCHAR(64),
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_rt_account_id ON refresh_token(account_id);
CREATE INDEX idx_rt_family_id ON refresh_token(family_id);
CREATE INDEX idx_rt_cleanup ON refresh_token(expires_at) WHERE revoked = FALSE;
