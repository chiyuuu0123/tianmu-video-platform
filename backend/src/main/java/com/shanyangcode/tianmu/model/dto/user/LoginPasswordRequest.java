package com.shanyangcode.tianmu.model.dto.user;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;


/**
 * 用户密码登录 DTO
 */
@Data
public class LoginPasswordRequest {


    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    private String account;



    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
