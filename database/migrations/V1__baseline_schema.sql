CREATE DATABASE IF NOT EXISTS hotel_management
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE hotel_management;

CREATE TABLE user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) NULL,
    phone VARCHAR(20) NULL,
    role TINYINT NOT NULL DEFAULT 0 COMMENT '0普通用户 1管理员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE room (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    room_name VARCHAR(100) NOT NULL,
    room_type VARCHAR(50) NULL,
    room_number VARCHAR(20) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    bed_type VARCHAR(50) NULL,
    max_people INT NULL DEFAULT 2,
    description TEXT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1可预订 2已预订 3已入住 4维护中',
    is_promotion TINYINT NOT NULL DEFAULT 0 COMMENT '0否 1是',
    promotion_price DECIMAL(10,2) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_room_number (room_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE booking (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    room_name VARCHAR(100) NULL,
    room_number VARCHAR(20) NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    days INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    contact_name VARCHAR(50) NULL,
    contact_phone VARCHAR(20) NULL,
    guest_name VARCHAR(50) NULL,
    guest_id_card VARCHAR(20) NULL,
    deposit DECIMAL(10,2) NULL DEFAULT 0.00,
    actual_check_in_time DATETIME NULL,
    actual_check_out_time DATETIME NULL,
    deposit_return DECIMAL(10,2) NULL DEFAULT 0.00,
    additional_charges DECIMAL(10,2) NULL DEFAULT 0.00,
    remark TEXT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2已取消 3已入住 4已退房',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_room_id (room_id),
    KEY idx_status (status),
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk_booking_room FOREIGN KEY (room_id) REFERENCES room (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notice (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0隐藏 1显示',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notice_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE feedback (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    type TINYINT NOT NULL DEFAULT 0 COMMENT '0建议 1投诉 2表扬',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理',
    reply TEXT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_feedback_user (user_id),
    KEY idx_feedback_status (status),
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
