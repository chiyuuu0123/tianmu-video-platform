package com.shanyangcode.tianmu.service;

import com.shanyangcode.tianmu.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * 管理登录会话。每个 JWT 都有独立的 Redis 会话，避免同一账号在另一个浏览器
 * 登录后把当前浏览器的弹幕认证挤下线。
 */
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private static final String SESSION_KEY_PREFIX = "auth:session:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    public void createSession(String userId, String token) {
        redisTemplate.opsForValue().set(
                sessionKey(token),
                userId,
                jwtUtil.getExpirationHours(),
                TimeUnit.HOURS
        );
    }

    public boolean isValid(String token) {
        return getValidUserId(token) != null;
    }

    public boolean isValid(String token, Long expectedUserId) {
        if (expectedUserId == null) {
            return false;
        }
        String userId = getValidUserId(token);
        return expectedUserId.toString().equals(userId);
    }

    public void revoke(String token) {
        String normalizedToken = normalize(token);
        Claims claims = jwtUtil.parse(normalizedToken);
        if (claims == null) {
            return;
        }

        redisTemplate.delete(sessionKey(normalizedToken));

        // 兼容旧版本的 userId -> token 存储，并且只删除当前 token。
        String userId = claims.getSubject();
        String legacyToken = redisTemplate.opsForValue().get(userId);
        if (normalizedToken.equals(legacyToken)) {
            redisTemplate.delete(userId);
        }
    }

    private String getValidUserId(String token) {
        String normalizedToken = normalize(token);
        Claims claims = jwtUtil.parse(normalizedToken);
        if (claims == null) {
            return null;
        }

        String userId = claims.getSubject();
        String sessionUserId = redisTemplate.opsForValue().get(sessionKey(normalizedToken));
        if (userId.equals(sessionUserId)) {
            return userId;
        }

        // 平滑兼容部署前已经登录的用户，首次验证成功后迁移到新会话格式。
        String legacyToken = redisTemplate.opsForValue().get(userId);
        if (!normalizedToken.equals(legacyToken)) {
            return null;
        }
        migrateLegacySession(userId, normalizedToken, claims);
        return userId;
    }

    private void migrateLegacySession(String userId, String token, Claims claims) {
        long ttlMillis = Duration.between(Instant.now(), claims.getExpiration().toInstant()).toMillis();
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(sessionKey(token), userId, ttlMillis, TimeUnit.MILLISECONDS);
        }
    }

    private String normalize(String token) {
        if (token == null) {
            return null;
        }
        String normalized = token.trim();
        if (normalized.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return normalized.substring(7).trim();
        }
        return normalized;
    }

    private String sessionKey(String token) {
        if (token == null || token.isBlank()) {
            return SESSION_KEY_PREFIX + "invalid";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return SESSION_KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
