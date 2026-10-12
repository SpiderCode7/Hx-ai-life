package com.hxy.hxailife.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.hxy.hxailife.auth.UserAccount;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthVO {
    private Long userId;

    private String username;

    private String email;

    private String token;

    private String tokenType;

    private LocalDateTime expiresAt;


    public static UserAuthVO registered(UserAccount user) {
        return new UserAuthVO(user.getId(), user.getUsername(), user.getEmail(), null, null, null);
    }

    public static UserAuthVO loggedIn(UserAccount user, String token, LocalDateTime expiresAt) {
        return new UserAuthVO(user.getId(), user.getUsername(), user.getEmail(), token, "Bearer", expiresAt);
    }

    public static UserAuthVO current(UserAccount user) {
        return new UserAuthVO(user.getId(), user.getUsername(), user.getEmail(), null, null, null);
    }
}
