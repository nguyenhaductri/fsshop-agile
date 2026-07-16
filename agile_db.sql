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

-- 2. CATEGORIES
CREATE TABLE dbo.categories (
    id BIGINT PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    slug NVARCHAR(100) NOT NULL UNIQUE,
    status INT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT GETDATE()
);
GO

-- 3. PRODUCTS
CREATE TABLE dbo.products (
    id BIGINT PRIMARY KEY,
    name NVARCHAR(200) NOT NULL,
    slug NVARCHAR(200) NOT NULL UNIQUE,
    description NVARCHAR(MAX),
    price DECIMAL(18,2) NOT NULL,
    category_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.categories(id),
    thumbnail NVARCHAR(255),
    status INT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);
GO

-- 4. PRODUCT IMAGES
CREATE TABLE dbo.product_images (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.products(id) ON DELETE CASCADE,
    image_url NVARCHAR(255) NOT NULL,
    is_primary BIT DEFAULT 0
);
GO

-- 5. PRODUCT VARIANTS (STOCK & ATTRIBUTES)
CREATE TABLE dbo.product_variants (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.products(id) ON DELETE CASCADE,
    size NVARCHAR(20) NOT NULL,
    color NVARCHAR(50) NOT NULL,
    color_code NVARCHAR(20) DEFAULT '#000000',
    stock_quantity INT NOT NULL DEFAULT 0,
    price DECIMAL(18,2) NOT NULL,
    status INT NOT NULL DEFAULT 1
);
GO

INSERT INTO dbo.categories (id, name, slug, status) VALUES
(1, N'Áo Thun', N'ao-thun', 1),
(2, N'Áo Sơ Mi', N'ao-so-mi', 1),
(3, N'Áo Khoác', N'ao-khoac', 1),
(4, N'Quần Jean', N'quan-jean', 1),
(5, N'Quần Tây', N'quan-tay', 1);
GO

INSERT INTO dbo.products (id, name, slug, description, price, category_id, thumbnail, status) VALUES
(1, N'Áo Thun Form Rộng Cotton FS01', N'ao-thun-form-rong-cotton-fs01', N'Chất liệu 100% cotton thoáng mát, thấm hút mồ hôi tốt, độ co giãn thoải mái khi vận động.', 189000, 1, N'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500&auto=format&fit=crop&q=60', 1),
(2, N'Áo Sơ Mi Tay Dài Chống Nhăn FS02', N'ao-so-mi-tay-dai-chong-nhan-fs02', N'Chất liệu lụa nến cao cấp không nhăn, form dáng ôm vừa vặn tôn dáng công sở.', 299000, 2, N'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=500&auto=format&fit=crop&q=60', 1),
(3, N'Quần Jean Nam Ống Đứng Co Giãn FS03', N'quan-jean-nam-ong-dung-co-gian-fs03', N'Vải denim co giãn 4 chiều mềm mại, wash màu retro cá tính bền màu theo thời gian.', 350000, 4, N'https://images.unsplash.com/photo-1542272604-787c3835535d?w=500&auto=format&fit=crop&q=60', 1),
(4, N'Áo Khoác Bomber Kaki 2 Lớp FS04', N'ao-khoac-bomber-kaki-2-lop-fs04', N'Thiết kế trẻ trung lót dù cản gió giữ ấm, phù hợp cả mùa đông lẫn thời tiết se lạnh.', 420000, 3, N'https://images.unsplash.com/photo-1548883354-7622d03aca27?w=500&auto=format&fit=crop&q=60', 1),
(5, N'Quần Tây Công Sở Dáng Slimfit FS05', N'quan-tay-cong-so-dang-slimfit-fs05', N'Form dáng lịch lãm chuẩn văn phòng, đường may tỉ mỉ sắc nét tôn vóc dáng.', 320000, 5, N'https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=500&auto=format&fit=crop&q=60', 1);
GO

INSERT INTO dbo.product_images (product_id, image_url, is_primary) VALUES
(1, N'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500&auto=format&fit=crop&q=60', 1),
(2, N'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=500&auto=format&fit=crop&q=60', 1),
(3, N'https://images.unsplash.com/photo-1542272604-787c3835535d?w=500&auto=format&fit=crop&q=60', 1),
(4, N'https://images.unsplash.com/photo-1548883354-7622d03aca27?w=500&auto=format&fit=crop&q=60', 1),
(5, N'https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=500&auto=format&fit=crop&q=60', 1);
GO

INSERT INTO dbo.product_variants (product_id, size, color, color_code, stock_quantity, price, status) VALUES
(1, N'M', N'Trắng', N'#FFFFFF', 4, 189000, 1),
(1, N'L', N'Đen', N'#000000', 15, 189000, 1),
(2, N'L', N'Xanh biển', N'#3B82F6', 3, 299000, 1),
(2, N'XL', N'Trắng', N'#FFFFFF', 8, 299000, 1),
(3, N'30', N'Xanh đậm', N'#1E3A8A', 20, 350000, 1),
(3, N'31', N'Đen wash', N'#18181B', 2, 350000, 1),
(4, N'L', N'Rêu', N'#3F6212', 12, 420000, 1),
(5, N'31', N'Đen', N'#000000', 25, 320000, 1);
GO
