package com.hxy.hxailife.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@TableName("user_accounts")
public class UserAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;

    private String passwordHash;

    private String status = "ACTIVE";

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public UserAccount(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
}
