CREATE TABLE roles (
    id BIGINT IDENTITY PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name NVARCHAR(100) NOT NULL
);

CREATE TABLE app_users (
    id BIGINT IDENTITY PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name NVARCHAR(150) NOT NULL,
    email VARCHAR(150) NULL,
    enabled BIT NOT NULL DEFAULT 1,
    password_change_required BIT NOT NULL DEFAULT 1,
    last_login_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    row_version ROWVERSION
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT pk_user_roles PRIMARY KEY(user_id, role_id)
);

INSERT INTO roles(code, name) VALUES
('ADMIN', N'Quản trị hệ thống'),
('MANAGER', N'Ban quản lý'),
('ACCOUNTANT', N'Kế toán'),
('CASHIER', N'Thu ngân'),
('PARKING_OPERATOR', N'Nhân viên bãi xe'),
('LEASING_OFFICER', N'Nhân viên quản lý mặt bằng');
