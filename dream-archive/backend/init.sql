-- Dream Archive database initialization

CREATE DATABASE IF NOT EXISTS dream_archive DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dream_archive;

-- 1. User table
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'User ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Username',
    `password` VARCHAR(100) NOT NULL COMMENT 'Password (hashed)',
    `email` VARCHAR(100) COMMENT 'Email (optional)',
    `avatar` VARCHAR(255) DEFAULT '/default-avatar.png' COMMENT 'Avatar',
    `role` VARCHAR(20) DEFAULT 'USER' COMMENT 'Role: USER/ADMIN',
    `status` TINYINT DEFAULT 1 COMMENT 'Status: 1 active, 0 disabled',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    INDEX idx_username (`username`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User table';

-- 2. Dream category table
CREATE TABLE IF NOT EXISTS `dream_category` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Category ID',
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Category name',
    `description` VARCHAR(200) COMMENT 'Description',
    `icon` VARCHAR(100) COMMENT 'Icon',
    `color` VARCHAR(20) COMMENT 'Color',
    `sort_order` INT DEFAULT 0 COMMENT 'Sort order',
    `status` TINYINT DEFAULT 1 COMMENT 'Status: 1 active, 0 disabled',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dream category table';

-- 3. Dream table
CREATE TABLE IF NOT EXISTS `dream` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Dream ID',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `category_id` INT NOT NULL COMMENT 'Category ID',
    `title` VARCHAR(200) NOT NULL COMMENT 'Dream title',
    `content` TEXT NOT NULL COMMENT 'Dream content',
    `tags` VARCHAR(500) COMMENT 'Tags (comma separated)',
    `images` TEXT COMMENT 'Image URLs',
    `mood_score` TINYINT COMMENT 'Mood score 1-5',
    `is_public` TINYINT DEFAULT 1 COMMENT 'Public: 1 yes, 0 no',
    `view_count` INT DEFAULT 0 COMMENT 'Views',
    `like_count` INT DEFAULT 0 COMMENT 'Likes',
    `comment_count` INT DEFAULT 0 COMMENT 'Comments',
    `status` TINYINT DEFAULT 1 COMMENT 'Status: 1 normal, 0 deleted, 2 pending',
    `dream_date` DATE COMMENT 'Dream date',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    `analysis_label` INT COMMENT 'Analysis label id',
    `analysis_label_name` VARCHAR(50) COMMENT 'Analysis label name',
    `analysis_confidence` DECIMAL(6,4) COMMENT 'Analysis confidence',
    `analysis_intensity` VARCHAR(30) COMMENT 'Analysis intensity',
    `analysis_feedback` TEXT COMMENT 'Analysis feedback',
    `analysis_updated_at` DATETIME COMMENT 'Analysis updated time',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`category_id`) REFERENCES `dream_category`(`id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_category_id (`category_id`),
    INDEX idx_create_time (`create_time`),
    INDEX idx_is_public (`is_public`),
    FULLTEXT INDEX idx_title_content (`title`, `content`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dream table';

-- 4. Comment table
CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Comment ID',
    `dream_id` BIGINT NOT NULL COMMENT 'Dream ID',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `parent_id` BIGINT DEFAULT 0 COMMENT 'Parent comment ID',
    `content` TEXT NOT NULL COMMENT 'Content',
    `like_count` INT DEFAULT 0 COMMENT 'Likes',
    `status` TINYINT DEFAULT 1 COMMENT 'Status: 1 normal, 0 deleted',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    FOREIGN KEY (`dream_id`) REFERENCES `dream`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX idx_dream_id (`dream_id`),
    INDEX idx_user_id (`user_id`),
    INDEX idx_parent_id (`parent_id`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Comment table';

-- 5. Like record table
CREATE TABLE IF NOT EXISTS `like_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Like ID',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `target_id` BIGINT NOT NULL COMMENT 'Target ID (dream or comment)',
    `target_type` TINYINT NOT NULL COMMENT 'Target type: 1 dream, 2 comment',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    UNIQUE KEY uk_user_target (`user_id`, `target_id`, `target_type`),
    INDEX idx_target (`target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Like record table';

-- 6. Questionnaire table
CREATE TABLE IF NOT EXISTS `questionnaire` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Questionnaire ID',
    `user_id` BIGINT NOT NULL COMMENT 'User ID',
    `score` INT NOT NULL COMMENT 'Score',
    `result` VARCHAR(50) COMMENT 'Result level',
    `suggestions` TEXT COMMENT 'Suggestions',
    `answers` JSON COMMENT 'Answer details (JSON)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX idx_user_id (`user_id`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Questionnaire table';

-- 7. System config table
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Config ID',
    `config_key` VARCHAR(100) NOT NULL UNIQUE COMMENT 'Config key',
    `config_value` TEXT COMMENT 'Config value',
    `description` VARCHAR(200) COMMENT 'Description',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System config table';

-- Seed dream categories
INSERT INTO `dream_category` (`name`, `description`, `icon`, `color`, `sort_order`) VALUES
('快乐梦', '充满欢笑与愉快的美好梦境', '😊', '#52c41a', 1),
('恐怖梦', '令人不安的恐惧体验', '😱', '#f5222d', 2),
('焦虑梦', '反映压力与担忧的梦境', '😥', '#fa8c16', 3),
('奇幻梦', '充满想象力的奇妙梦境', '✨', '#722ed1', 4),
('日常梦', '日常生活场景的再现', '📘', '#1890ff', 5),
('预知梦', '似乎预示未来的梦境', '🔮', '#eb2f96', 6),
('噩梦', '极度不适的恐怖梦境', '🕳️', '#000000', 7),
('其他', '难以归类的特殊梦境', '❓', '#8c8c8c', 8);

-- Seed admin user (password: admin123)
INSERT INTO `user` (`username`, `password`, `role`, `email`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN', 'admin@dreamarchive.com');

-- Seed system config
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('site_name', '梦境档案馆', '网站名称'),
('site_description', '记录梦境，分享心情，探索内心世界', '网站描述'),
('allow_register', '1', '是否允许注册：1允许 0关闭'),
('default_avatar', '/uploads/default-avatar.png', '默认头像'),
('max_upload_size', '10485760', '最大上传文件大小（字节）');
