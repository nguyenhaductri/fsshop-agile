-- =======================================================
-- FSSHOP AGILE - DATABASE SCRIPT (MS SQL SERVER)
-- Project: Website Ban Quan Ao FS SHOP
-- =======================================================

CREATE DATABASE agile_db;
GO
USE agile_db;
GO

-- 1. USERS & ROLES
CREATE TABLE dbo.users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL UNIQUE,
    email NVARCHAR(100) NOT NULL UNIQUE,
    password NVARCHAR(255) NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    avatar_url NVARCHAR(255),
    role NVARCHAR(20) NOT NULL DEFAULT 'CUSTOMER', -- 'CUSTOMER', 'ADMIN', 'OWNER'
    status INT NOT NULL DEFAULT 1, -- 1: Active, 0: Disabled
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);
GO

INSERT INTO dbo.users (username, email, password, full_name, role, status)
VALUES 
(N'admin', N'admin@fsshop.com', N'admin123', N'Quản Trị Viên (Owner)', N'OWNER', 1),
(N'long_staff', N'longnmth04742@gmail.com', N'long123', N'Nguyễn Minh Long (Admin)', N'ADMIN', 1),
(N'mai_customer', N'Mainnth06849@gmail.com', N'mai123', N'Ngọc Mai', N'CUSTOMER', 1),
(N'tri_customer', N'nguyenhaductri2@gmail.com', N'tri123', N'Nguyễn Hà Đức Trí', N'CUSTOMER', 1),
(N'kenko_customer', N'hungpham24725@gmail.com', N'kenko123', N'Phạm Gia Hưng (Ken Ko)', N'CUSTOMER', 1);
GO
