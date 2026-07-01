-- ======================================================
-- 空运代理智能管理系统 - MySQL 初始化脚本
-- ======================================================

-- 创建数据库（如未创建）
-- CREATE DATABASE IF NOT EXISTS airfreight CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- JPA 会自动建表（ddl-auto: update），以下为参考结构
-- 如需手动建表，请执行下方语句

USE airfreight;

-- 运单主表
CREATE TABLE IF NOT EXISTS shipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_no VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) DEFAULT 'QUOTATION',

    goods_name VARCHAR(200) NOT NULL,
    goods_category VARCHAR(50),
    piece_count INT,
    gross_weight DECIMAL(12,2),
    volume_weight DECIMAL(12,2),
    chargeable_weight DECIMAL(12,2),
    length_cm DECIMAL(10,2),
    width_cm DECIMAL(10,2),
    height_cm DECIMAL(10,2),

    origin_airport VARCHAR(10) NOT NULL,
    dest_airport VARCHAR(10) NOT NULL,
    incoterm VARCHAR(20),

    shipper_name VARCHAR(200) NOT NULL,
    shipper_address VARCHAR(500),
    shipper_phone VARCHAR(50),
    consignee_name VARCHAR(200) NOT NULL,
    consignee_address VARCHAR(500),
    consignee_phone VARCHAR(50),

    awb_type VARCHAR(20),
    mawb_number VARCHAR(50),
    haw_number VARCHAR(50),
    airline_code VARCHAR(50),
    flight_no VARCHAR(50),
    etd DATETIME,
    eta DATETIME,
    actual_departure DATETIME,
    actual_arrival DATETIME,

    hs_code VARCHAR(30),
    customs_cleared TINYINT(1) DEFAULT 0,
    customs_clearance_time DATETIME,

    freight_charge DECIMAL(12,2),
    fuel_surcharge DECIMAL(12,2),
    security_surcharge DECIMAL(12,2),
    ground_handling_fee DECIMAL(12,2),
    customs_broker_fee DECIMAL(12,2),
    customs_duty DECIMAL(12,2),
    vat_amount DECIMAL(12,2),
    storage_fee DECIMAL(12,2),
    insurance_fee DECIMAL(12,2),
    total_charge DECIMAL(12,2),
    currency VARCHAR(10) DEFAULT 'CNY',
    paid TINYINT(1) DEFAULT 0,
    paid_time DATETIME,

    created_at DATETIME,
    updated_at DATETIME,

    INDEX idx_shipment_no (shipment_no),
    INDEX idx_status (status),
    INDEX idx_origin_dest (origin_airport, dest_airport)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订舱表
CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_no VARCHAR(50) NOT NULL UNIQUE,
    shipment_id BIGINT,
    booking_status VARCHAR(20) DEFAULT 'SUBMITTED',
    booking_note TEXT,
    warehouse_receipt VARCHAR(200),
    cut_off_time DATETIME,
    confirmed_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    INDEX idx_booking_no (booking_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 财务表
CREATE TABLE IF NOT EXISTS financials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_no VARCHAR(50) NOT NULL UNIQUE,
    shipment_id BIGINT,
    fee_type VARCHAR(50),
    amount DECIMAL(12,2),
    exchange_rate DECIMAL(10,4),
    currency VARCHAR(10) DEFAULT 'CNY',
    payment_status VARCHAR(20) DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    payment_ref VARCHAR(200),
    due_date DATETIME,
    paid_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    INDEX idx_invoice_no (invoice_no),
    INDEX idx_payment_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 客户表
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20),
    contact_person VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(200),
    address VARCHAR(500),
    country VARCHAR(100),
    tax_id VARCHAR(50),
    bank_account VARCHAR(50),
    bank_name VARCHAR(200),
    swift_code VARCHAR(20),
    remark VARCHAR(200),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_name (name),
    INDEX idx_country (country)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 报关记录表
CREATE TABLE IF NOT EXISTS customs_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    declaration_no VARCHAR(50) NOT NULL UNIQUE,
    shipment_id BIGINT,
    customs_type VARCHAR(20),
    hs_code VARCHAR(30),
    status VARCHAR(20) DEFAULT 'SUBMITTED',
    declaration_data TEXT,
    inspector_name VARCHAR(200),
    submitted_at DATETIME,
    cleared_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    INDEX idx_declaration_no (declaration_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 跟踪事件表
CREATE TABLE IF NOT EXISTS tracking_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT,
    event_type VARCHAR(20),
    location VARCHAR(100),
    description TEXT,
    status VARCHAR(20) DEFAULT 'COMPLETED',
    event_time DATETIME,
    created_at DATETIME,
    FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    INDEX idx_shipment_event (shipment_id, event_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通知推送表
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT,
    channel VARCHAR(20),
    recipient VARCHAR(50),
    subject VARCHAR(100),
    content TEXT,
    status VARCHAR(20) DEFAULT 'SENT',
    notify_type VARCHAR(20),
    sent_at DATETIME,
    created_at DATETIME,
    FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    INDEX idx_notify_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 邮箱账号表
CREATE TABLE IF NOT EXISTS mail_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_name VARCHAR(100) NOT NULL,
    email_address VARCHAR(200) NOT NULL,
    password VARCHAR(200) NOT NULL,
    smtp_host VARCHAR(200) NOT NULL,
    smtp_port INT,
    smtp_ssl BIT(1),
    receive_protocol VARCHAR(10) DEFAULT 'IMAP',
    pop3_host VARCHAR(200),
    pop3_port INT,
    pop3_ssl BIT(1),
    imap_host VARCHAR(200),
    imap_port INT,
    imap_ssl BIT(1),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_check_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_email_address (email_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 邮件记录表
CREATE TABLE IF NOT EXISTS mail_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT,
    message_id VARCHAR(255),
    direction VARCHAR(20),
    from_address VARCHAR(200),
    from_personal VARCHAR(200),
    to_address VARCHAR(500),
    cc_address VARCHAR(500),
    bcc_address VARCHAR(500),
    subject VARCHAR(500),
    content TEXT,
    content_html TEXT,
    has_attachments BIT(1),
    attachment_names TEXT,
    is_read BIT(1),
    is_starred BIT(1),
    priority VARCHAR(20),
    sent_date DATETIME,
    received_date DATETIME,
    category VARCHAR(50),
    tags VARCHAR(200),
    classification_method VARCHAR(50),
    classification_confidence DECIMAL(4,3),
    classification_reason TEXT,
    is_classified BIT(1),
    classified_at DATETIME,
    created_at DATETIME,
    FOREIGN KEY (account_id) REFERENCES mail_accounts(id),
    INDEX idx_account_id (account_id),
    INDEX idx_direction (direction),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- LLM配置表
CREATE TABLE IF NOT EXISTS llm_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    api_key VARCHAR(500),
    base_url VARCHAR(500),
    model VARCHAR(100),
    temperature DOUBLE,
    max_tokens INT,
    api_version VARCHAR(20),
    is_active BIT(1) DEFAULT 0,
    classification_prompt TEXT,
    remark VARCHAR(500),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_provider (provider),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分类规则表
CREATE TABLE IF NOT EXISTS classification_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    field VARCHAR(20) NOT NULL,
    keywords VARCHAR(500),
    category VARCHAR(50),
    tags VARCHAR(200),
    confidence DECIMAL(4,3),
    is_active BIT(1) DEFAULT 1,
    priority INT DEFAULT 0,
    description VARCHAR(500),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_is_active (is_active),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据
INSERT INTO customers (name, type, contact_person, phone, email, country, swift_code, created_at, updated_at)
VALUES 
('上海电子科技有限公司', 'SHIPPER', '张经理', '13800138001', 'zhang@sh-elec.com', 'China', 'ICBKCNBJ', NOW(), NOW()),
('New York Electronics Inc', 'CONSIGNEE', 'John Smith', '+1-212-555-0101', 'john@ny-elec.com', 'USA', 'BOFAUS3N', NOW(), NOW()),
('深圳跨境电商有限公司', 'SHIPPER', '李总', '13900139001', 'li@sz-cross.com', 'China', 'BKCHCNBJ', NOW(), NOW()),
('London Trading Co Ltd', 'CONSIGNEE', 'Sarah Wilson', '+44-20-7946-0958', 'sarah@london-trade.co.uk', 'UK', 'BARCGB22', NOW(), NOW());