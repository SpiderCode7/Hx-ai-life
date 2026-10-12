package com.hxy.hxailife.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {
    @NotBlank(message = "用户名或邮箱不能为空")
    private String identifier;

    @NotBlank(message = "密码不能为空")
    private String password;

}
