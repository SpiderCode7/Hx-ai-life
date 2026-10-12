package com.hxy.hxailife.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须为 3-50 个字符")
    private String username;

    @Email(message = "邮箱格式不正确")
    @Size(max = 120, message = "邮箱长度不能超过 120 个字符")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 72, message = "密码长度必须为 8-72 个字符")
    private String password;

}
