package com.shanyangcode.tianmu.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public final class JwtUtil {

    private final Duration expiration;
    private final long expirationHours;
    private final Key signingKey;

    /**
     * JWT 密钥和过期时间由环境配置注入。生产环境必须提供 TIANMU_JWT_SECRET。
     */
    public JwtUtil(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-hours:48}") long expirationHours) {
        if (expirationHours <= 0) {
            throw new IllegalArgumentException("JWT expiration hours must be greater than zero");
        }
        this.expirationHours = expirationHours;
        this.expiration = Duration.ofHours(expirationHours);
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("TIANMU_JWT_SECRET must be a valid Base64-encoded key of at least 32 bytes", e);
        }
    }

    /**
     * 生成JWT
     *
     * @param id 手机
     * @return JWT
     */
    public String generate(String id) {
        // 过期时间
        Date expiryDate = new Date(System.currentTimeMillis() + expiration.toMillis());
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(id) // 将id放进JWT
                .setIssuedAt(new Date()) // 设置JWT签发时间
                .setExpiration(expiryDate)  // 设置过期时间
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    public long getExpirationHours() {
        return expirationHours;
    }

    /**
     * 解析JWT
     *
     * @param token JWT字符串
     * @return 解析成功返回Claims对象，解析失败返回null
     */
    public Claims parse(String token) {
        // 如果是空字符串直接返回null
        if (StringUtils.isEmpty(token)){
            return null;
        }
        // 这个Claims对象包含了许多属性，比如签发时间、过期时间以及存放的数据等
        Claims claims = null;
        // 解析失败了会抛出异常，所以我们要捕捉一下。token过期、token非法都会导致解析失败
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            return null;
        }
        return claims;
    }

    private Key getSignInKey() {
        return signingKey;
    }
}
