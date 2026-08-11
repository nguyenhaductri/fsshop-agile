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

-- 6. CART & CART ITEMS
CREATE TABLE dbo.cart (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE FOREIGN KEY REFERENCES dbo.users(id) ON DELETE CASCADE,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);
GO

CREATE TABLE dbo.cart_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    cart_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.cart(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.products(id),
    variant_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.product_variants(id),
    quantity INT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT UQ_Cart_Variant UNIQUE(cart_id, variant_id)
);
GO

INSERT INTO dbo.cart (user_id) VALUES (3), (4), (5);
GO

INSERT INTO dbo.cart_items (cart_id, product_id, variant_id, quantity) VALUES
(1, 1, 2, 2),
(1, 4, 7, 1),
(2, 2, 4, 1);
GO

-- 6.1 USER ADDRESS BOOK
CREATE TABLE dbo.user_addresses (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.users(id) ON DELETE CASCADE,
    receiver_name NVARCHAR(100) NOT NULL,
    phone_number NVARCHAR(20) NOT NULL,
    province NVARCHAR(100) NOT NULL,
    district NVARCHAR(100) NOT NULL,
    ward NVARCHAR(100) NOT NULL,
    street_address NVARCHAR(255) NOT NULL,
    is_default BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);
GO

INSERT INTO dbo.user_addresses (user_id, receiver_name, phone_number, province, district, ward, street_address, is_default) VALUES
(3, N'Ngọc Mai', N'0987654321', N'Hà Nội', N'Quận Cầu Giấy', N'Phường Dịch Vọng Hậu', N'Tòa Landmark 72, Đường Phạm Hùng', 1),
(4, N'Nguyễn Hà Đức Trí', N'0912345678', N'Hồ Chí Minh', N'Quận 1', N'Phường Bến Nghé', N'123 Đường Lê Lợi', 1),
(5, N'Phạm Gia Hưng (Ken Ko)', N'0977889900', N'Đà Nẵng', N'Quận Hải Châu', N'Phường Thạch Thang', N'456 Đường Bạch Đằng', 1);
GO

-- 7. ORDERS & ORDER ITEMS
CREATE TABLE dbo.orders (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_code NVARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.users(id),
    total_amount DECIMAL(18,2) NOT NULL,
    discount_amount DECIMAL(18,2) DEFAULT 0,
    final_amount DECIMAL(18,2) NOT NULL,
    receiver_name NVARCHAR(100) NOT NULL,
    phone_number NVARCHAR(20) NOT NULL,
    shipping_address NVARCHAR(500) NOT NULL,
    payment_method NVARCHAR(50) NOT NULL DEFAULT 'COD',
    payment_status NVARCHAR(50) NOT NULL DEFAULT 'PENDING',
    status NVARCHAR(50) NOT NULL DEFAULT 'PENDING',
    note NVARCHAR(500),
    voucher_code NVARCHAR(50),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);
GO

CREATE TABLE dbo.order_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.products(id),
    variant_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.product_variants(id),
    product_name NVARCHAR(200) NOT NULL,
    size NVARCHAR(20) NOT NULL,
    color NVARCHAR(50) NOT NULL,
    price DECIMAL(18,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(18,2) NOT NULL
);
GO

INSERT INTO dbo.orders (order_code, user_id, total_amount, discount_amount, final_amount, receiver_name, phone_number, shipping_address, payment_method, payment_status, status, note, created_at)
VALUES
(N'ORD-20260615-1001', 3, 189000, 0, 189000, N'Ngọc Mai', N'0987654321', N'Tòa Landmark 72, Đường Phạm Hùng, Phường Dịch Vọng Hậu, Quận Cầu Giấy, Hà Nội', N'COD', N'PAID', N'DELIVERED', N'Giao giờ hành chính', '2026-06-15 09:30:00'),
(N'ORD-20260710-1002', 4, 299000, 0, 299000, N'Nguyễn Hà Đức Trí', N'0912345678', N'123 Đường Lê Lợi, Phường Bến Nghé, Quận 1, Hồ Chí Minh', N'VNPAY', N'PAID', N'DELIVERED', N'Gọi trước khi giao', '2026-07-10 14:15:00'),
(N'ORD-20260805-1003', 5, 420000, 0, 420000, N'Phạm Gia Hưng (Ken Ko)', N'0977889900', N'456 Đường Bạch Đằng, Phường Thạch Thang, Quận Hải Châu, Đà Nẵng', N'COD', N'PAID', N'DELIVERED', N'Giao hàng tận nơi', '2026-08-05 16:45:00'),
(N'ORD-20260812-1004', 3, 378000, 0, 378000, N'Ngọc Mai', N'0987654321', N'Tòa Landmark 72, Đường Phạm Hùng, Phường Dịch Vọng Hậu, Quận Cầu Giấy, Hà Nội', N'COD', N'PENDING', N'CONFIRMED', N'Áo đẹp chuẩn size giúp mình', '2026-08-12 10:00:00'),
(N'ORD-20260814-1005', 4, 350000, 0, 350000, N'Nguyễn Hà Đức Trí', N'0912345678', N'123 Đường Lê Lợi, Phường Bến Nghé, Quận 1, Hồ Chí Minh', N'MOMO', N'PENDING', N'SHIPPING', N'Giao nhanh giúp mình', '2026-08-14 11:30:00');
GO

INSERT INTO dbo.order_items (order_id, product_id, variant_id, product_name, size, color, price, quantity, subtotal)
VALUES
(1, 1, 2, N'Áo Thun Form Rộng Cotton FS01', N'L', N'Đen', 189000, 1, 189000),
(2, 2, 4, N'Áo Sơ Mi Tay Dài Chống Nhăn FS02', N'XL', N'Trắng', 299000, 1, 299000),
(3, 4, 7, N'Áo Khoác Bomber Kaki 2 Lớp FS04', N'L', N'Rêu', 420000, 1, 420000),
(4, 1, 2, N'Áo Thun Form Rộng Cotton FS01', N'L', N'Đen', 189000, 2, 378000),
(5, 3, 5, N'Quần Jean Nam Ống Đứng Co Giãn FS03', N'30', N'Xanh đậm', 350000, 1, 350000);
GO

-- 7.1 ORDER STATUS HISTORIES
CREATE TABLE dbo.order_histories (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.orders(id) ON DELETE CASCADE,
    status NVARCHAR(50) NOT NULL,
    description NVARCHAR(500) NOT NULL,
    created_by NVARCHAR(100),
    created_at DATETIME DEFAULT GETDATE()
);
GO

INSERT INTO dbo.order_histories (order_id, status, description, created_by, created_at)
VALUES
(1, N'PENDING', N'Đơn hàng đã được tạo thành công trên hệ thống', N'Ngọc Mai', '2026-06-15 09:30:00'),
(1, N'CONFIRMED', N'FS Shop đã duyệt đơn hàng và chuẩn bị đóng gói', N'Admin', '2026-06-15 10:00:00'),
(1, N'SHIPPING', N'Đơn hàng đã bàn giao cho đơn vị vận chuyển GHN', N'Admin', '2026-06-15 14:00:00'),
(1, N'DELIVERED', N'Giao hàng thành công tới người nhận', N'Shipper GHN', '2026-06-16 11:30:00'),

(2, N'PENDING', N'Đơn hàng được đặt thành công', N'Nguyễn Hà Đức Trí', '2026-07-10 14:15:00'),
(2, N'CONFIRMED', N'Đã duyệt đơn hàng', N'Admin', '2026-07-10 15:00:00'),
(2, N'SHIPPING', N'Đang giao hàng', N'Admin', '2026-07-10 17:00:00'),
(2, N'DELIVERED', N'Khách hàng đã nhận hàng', N'Shipper', '2026-07-11 16:20:00'),

(3, N'PENDING', N'Đơn hàng được đặt thành công', N'Phạm Gia Hưng', '2026-08-05 16:45:00'),
(3, N'CONFIRMED', N'Admin đã xác nhận đơn hàng', N'Admin', '2026-08-05 17:15:00'),
(3, N'SHIPPING', N'Bàn giao shipper', N'Admin', '2026-08-06 08:30:00'),
(3, N'DELIVERED', N'Giao hàng thành công', N'Shipper', '2026-08-07 10:00:00'),

(4, N'PENDING', N'Đơn hàng chờ xác nhận', N'Ngọc Mai', '2026-08-12 10:00:00'),
(4, N'CONFIRMED', N'Shop đã tiếp nhận và duyệt đơn hàng', N'Admin', '2026-08-12 11:30:00'),

(5, N'PENDING', N'Đơn hàng chờ xác nhận', N'Nguyễn Hà Đức Trí', '2026-08-14 11:30:00'),
(5, N'CONFIRMED', N'Đã duyệt đơn', N'Admin', '2026-08-14 13:00:00'),
(5, N'SHIPPING', N'Đang trên đường giao tới bạn', N'Shipper', '2026-08-14 15:00:00');
GO

-- 7.2 PRODUCT REVIEWS & REPLIES
CREATE TABLE dbo.reviews (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.users(id),
    product_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.products(id) ON DELETE CASCADE,
    order_id BIGINT NULL FOREIGN KEY REFERENCES dbo.orders(id),
    parent_id BIGINT FOREIGN KEY REFERENCES dbo.reviews(id),
    reply_to_user_name NVARCHAR(100),
    rating_stars INT NULL CHECK (rating_stars IS NULL OR (rating_stars >= 1 AND rating_stars <= 5)),
    comment NVARCHAR(1000) NULL,
    created_at DATETIME DEFAULT GETDATE()
);
GO

CREATE TABLE dbo.review_votes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    review_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.reviews(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL FOREIGN KEY REFERENCES dbo.users(id),
    vote_type NVARCHAR(10) NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT UQ_Review_User_Vote UNIQUE(review_id, user_id)
);
GO

INSERT INTO dbo.reviews (user_id, product_id, order_id, parent_id, rating_stars, comment)
VALUES
(3, 1, 1, NULL, 5, N'Áo mặc rất thích, vải cotton dày dặn và mát mẻ! Sẽ tiếp tục ủng hộ shop.'),
(2, 1, NULL, 1, NULL, N'Cảm ơn bạn Ngọc Mai đã tin tưởng và đánh giá 5 sao cho sản phẩm của FS SHOP ạ!'),
(4, 2, 2, NULL, 5, N'Sơ mi vừa vặn, form áo rất chuẩn công sở!'),
(5, 4, 3, NULL, 5, N'Áo khoác bomber kaki đẹp lắm ạ, dày dặn ấm áp!');
GO

INSERT INTO dbo.review_votes (review_id, user_id, vote_type)
VALUES
(1, 3, N'LIKE'),
(1, 2, N'LIKE'),
(3, 4, N'LIKE');
GO
