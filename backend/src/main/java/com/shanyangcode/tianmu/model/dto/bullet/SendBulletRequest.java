package com.shanyangcode.tianmu.model.dto.bullet;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableField;

import lombok.Data;


@Data
@SuppressWarnings({"all"})
public class SendBulletRequest implements Serializable {

    /**
     * 弹幕 ID
     */

    private Long bulletId;

    /**
     * 视频 ID
     */

    private Long videoId;

    /**
     * 用户 ID
     */

    private Long userId;

    /**
     * WebSocket 登录令牌，仅用于鉴权，不会保存到数据库。
     */
    @TableField(exist = false)
    private String token;

    /**
     * 弹幕内容
     */

    private String content;


    /**
     * 弹幕所在视频的时间点
     */
    private Double playbackTime;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
