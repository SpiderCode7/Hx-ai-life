package com.hxy.hxailife.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hxy.hxailife.auth.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    @Select("SELECT COUNT(1) FROM user_accounts WHERE LOWER(username) = LOWER(#{username})")
    boolean existsByUsernameIgnoreCase(String username);

    @Select("SELECT COUNT(1) FROM user_accounts WHERE LOWER(email) = LOWER(#{email})")
    boolean existsByEmailIgnoreCase(String email);

    @Select("""
            SELECT id, username, email, password_hash, status, created_at, updated_at
            FROM user_accounts
            WHERE LOWER(username) = LOWER(#{identifier})
               OR LOWER(email) = LOWER(#{identifier})
            LIMIT 1
            """)
    Optional<UserAccount> findByUsernameOrEmailIgnoreCase(String identifier);
}
