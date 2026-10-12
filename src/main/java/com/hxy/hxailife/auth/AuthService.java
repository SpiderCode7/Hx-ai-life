package com.hxy.hxailife.auth;

import com.hxy.hxailife.auth.dto.LoginDTO;
import com.hxy.hxailife.auth.dto.RegisterDTO;
import com.hxy.hxailife.auth.vo.UserAuthVO;
import com.hxy.hxailife.mapper.UserAccountMapper;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final long SESSION_TIMEOUT_SECONDS = 7 * 24 * 60 * 60L;

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccountMapper userAccountMapper,
                       PasswordEncoder passwordEncoder) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAuthVO register(RegisterDTO request) {
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());

        if (userAccountMapper.existsByUsernameIgnoreCase(username)) {
            throw new AuthException("用户名已存在");
        }
        if (email != null && userAccountMapper.existsByEmailIgnoreCase(email)) {
            throw new AuthException("邮箱已被注册");
        }

        UserAccount user = new UserAccount(username, email, passwordEncoder.encode(request.getPassword()));
        try {
            userAccountMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new AuthException("用户名或邮箱已存在");
        }
        return UserAuthVO.registered(user);
    }

    @Transactional
    public UserAuthVO login(LoginDTO request) {
        String identifier = request.getIdentifier().trim();
        UserAccount user = userAccountMapper
                .findByUsernameOrEmailIgnoreCase(identifier)
                .orElseThrow(() -> new AuthException("用户名或密码错误"));

        if (!"ACTIVE".equals(user.getStatus())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("用户名或密码错误");
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(SESSION_TIMEOUT_SECONDS);
        StpUtil.login(user.getId(), SaLoginModel.create().setTimeout(SESSION_TIMEOUT_SECONDS));
        return UserAuthVO.loggedIn(user, StpUtil.getTokenValue(), expiresAt);
    }

    public void logout() {
        StpUtil.logout();
    }

    public UserAuthVO currentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new AuthException("登录用户不存在");
        }
        return UserAuthVO.current(user);
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
