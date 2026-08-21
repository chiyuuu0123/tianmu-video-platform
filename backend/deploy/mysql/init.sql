-- Tianmu local database initialization.
-- WARNING: this script recreates all Tianmu business tables.

CREATE DATABASE IF NOT EXISTS `tianmu`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `tianmu`;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `comment`;
DROP TABLE IF EXISTS `follow`;
DROP TABLE IF EXISTS `favorite`;
DROP TABLE IF EXISTS `coin`;
DROP TABLE IF EXISTS `like`;
DROP TABLE IF EXISTS `bullet`;
DROP TABLE IF EXISTS `video_stats`;
DROP TABLE IF EXISTS `video`;
DROP TABLE IF EXISTS `file`;
DROP TABLE IF EXISTS `user_stats`;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `category`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `user` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `email` VARCHAR(256) DEFAULT NULL COMMENT '用户邮箱',
    `password` VARCHAR(256) NOT NULL COMMENT '用户密码',
    `nickname` VARCHAR(128) NOT NULL COMMENT '用户昵称',
    `avatar` VARCHAR(512) NOT NULL DEFAULT '/api/default-avatar.jpg' COMMENT '用户头像URL',
    `gender` TINYINT NOT NULL DEFAULT 2 COMMENT '性别 0女 1男 2未知',
    `description` TEXT DEFAULT NULL COMMENT '个性签名',
    `state` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0正常 1禁言 2封禁 3注销',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色 0普通用户 1管理员 2超级管理员',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `idx_email` (`email`),
    KEY `idx_state` (`state`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `user_stats` (
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `followers` INT DEFAULT 0 COMMENT '粉丝数',
    `following` INT DEFAULT 0 COMMENT '关注数',
    `video_count` INT DEFAULT 0 COMMENT '视频数',
    `coin_count` INT DEFAULT 20 COMMENT '硬币数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_user_stats_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户统计信息表';

CREATE TABLE `file` (
    `file_id` BIGINT NOT NULL COMMENT '文件ID',
    `file_hash` VARCHAR(512) NOT NULL COMMENT '文件哈希值',
    `file_url` VARCHAR(512) NOT NULL COMMENT '文件URL',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`file_id`),
    UNIQUE KEY `idx_file_hash` (`file_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件表';

CREATE TABLE `category` (
    `category_id` INT NOT NULL AUTO_INCREMENT COMMENT '分区ID',
    `category_name` VARCHAR(64) NOT NULL COMMENT '分区名',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`category_id`),
    UNIQUE KEY `uk_category_name` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分区表';

CREATE TABLE `video` (
    `video_id` BIGINT NOT NULL COMMENT '视频ID',
    `file_url` VARCHAR(512) NOT NULL COMMENT '文件URL',
    `cover_url` VARCHAR(512) NOT NULL COMMENT '封面URL',
    `user_id` BIGINT NOT NULL COMMENT '投稿用户ID',
    `title` VARCHAR(512) NOT NULL COMMENT '标题',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型 1自制 2转载',
    `duration` DOUBLE NOT NULL DEFAULT 0 COMMENT '播放时长（秒）',
    `category_id` INT NOT NULL COMMENT '分类ID',
    `tags` VARCHAR(512) DEFAULT NULL COMMENT '标签',
    `description` TEXT DEFAULT NULL COMMENT '简介',
    `status` TINYINT DEFAULT 2 COMMENT '状态 0下架 1审核中 2已发布',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`video_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    CONSTRAINT `fk_video_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_video_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频表';

CREATE TABLE `video_stats` (
    `video_id` BIGINT NOT NULL COMMENT '视频ID',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '播放量',
    `bullet_count` INT NOT NULL DEFAULT 0 COMMENT '弹幕数',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `coin_count` INT NOT NULL DEFAULT 0 COMMENT '投币数',
    `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '收藏数',
    `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论量',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`video_id`),
    CONSTRAINT `fk_video_stats_video` FOREIGN KEY (`video_id`) REFERENCES `video` (`video_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频数据统计表';

CREATE TABLE `bullet` (
    `bullet_id` BIGINT NOT NULL COMMENT '弹幕ID',
    `video_id` BIGINT NOT NULL COMMENT '视频ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `content` VARCHAR(128) NOT NULL COMMENT '弹幕内容',
    `color` VARCHAR(7) NOT NULL DEFAULT '#FFFFFF' COMMENT '弹幕颜色',
    `playback_time` DOUBLE NOT NULL COMMENT '弹幕所在视频的时间点',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`bullet_id`),
    KEY `idx_bullet_video` (`video_id`),
    CONSTRAINT `fk_bullet_video` FOREIGN KEY (`video_id`) REFERENCES `video` (`video_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_bullet_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='弹幕表';

CREATE TABLE `like` (
    `like_id` BIGINT NOT NULL COMMENT '点赞ID',
    `video_id` BIGINT NOT NULL COMMENT '视频ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`like_id`),
    UNIQUE KEY `uk_like_video_user` (`video_id`, `user_id`),
    CONSTRAINT `fk_like_video` FOREIGN KEY (`video_id`) REFERENCES `video` (`video_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

CREATE TABLE `coin` (
    `coin_id` BIGINT NOT NULL COMMENT '投币ID',
    `video_id` BIGINT NOT NULL COMMENT '视频ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`coin_id`),
    UNIQUE KEY `uk_coin_video_user` (`video_id`, `user_id`),
    CONSTRAINT `fk_coin_video` FOREIGN KEY (`video_id`) REFERENCES `video` (`video_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_coin_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投币表';

CREATE TABLE `favorite` (
    `favorite_id` BIGINT NOT NULL COMMENT '收藏ID',
    `video_id` BIGINT NOT NULL COMMENT '视频ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`favorite_id`),
    UNIQUE KEY `uk_favorite_video_user` (`video_id`, `user_id`),
    CONSTRAINT `fk_favorite_video` FOREIGN KEY (`video_id`) REFERENCES `video` (`video_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

CREATE TABLE `follow` (
    `follow_id` BIGINT NOT NULL COMMENT '关注ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `creator_id` BIGINT NOT NULL COMMENT '被关注用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`follow_id`),
    UNIQUE KEY `uk_follow_user_creator` (`user_id`, `creator_id`),
    CONSTRAINT `fk_follow_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_follow_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注表';

CREATE TABLE `comment` (
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `video_id` BIGINT NOT NULL COMMENT '视频ID',
    `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
    `parent_comment_id` BIGINT DEFAULT NULL COMMENT '父评论ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`comment_id`),
    KEY `idx_comment_video` (`video_id`),
    KEY `idx_comment_user` (`user_id`),
    KEY `idx_parent_comment` (`parent_comment_id`),
    CONSTRAINT `fk_comment_video` FOREIGN KEY (`video_id`) REFERENCES `video` (`video_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_parent` FOREIGN KEY (`parent_comment_id`) REFERENCES `comment` (`comment_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

INSERT INTO `category` (`category_name`) VALUES
    ('番剧'), ('国创'), ('综艺'), ('动画'), ('鬼畜'), ('舞蹈'), ('娱乐'),
    ('美食'), ('汽车'), ('体育'), ('电影'), ('电视剧'), ('游戏'), ('音乐'),
    ('动物'), ('情感'), ('户外'), ('时尚'), ('绘画'), ('健身'), ('风景'),
    ('亲情'), ('生活'), ('手工'), ('健康'), ('小剧场'), ('纪录片'),
    ('家装房产'), ('公益'), ('二次元');
