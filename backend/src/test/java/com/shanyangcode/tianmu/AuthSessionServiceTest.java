package com.shanyangcode.tianmu;

import com.shanyangcode.tianmu.service.AuthSessionService;
import com.shanyangcode.tianmu.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthSessionServiceTest {

    private final Map<String, String> redisValues = new ConcurrentHashMap<>();
    private AuthSessionService authSessionService;
    private JwtUtil jwtUtil;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenAnswer(invocation -> redisValues.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            redisValues.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        when(redisTemplate.delete(anyString())).thenAnswer(invocation -> redisValues.remove(invocation.getArgument(0)) != null);

        String secret = Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)
        );
        jwtUtil = new JwtUtil(secret, 48);
        authSessionService = new AuthSessionService(redisTemplate, jwtUtil);
    }

    @Test
    void shouldKeepMultipleSessionsForTheSameUser() {
        String firstToken = jwtUtil.generate("1001");
        String secondToken = jwtUtil.generate("1001");
        assertNotEquals(firstToken, secondToken);

        authSessionService.createSession("1001", firstToken);
        authSessionService.createSession("1001", secondToken);

        assertTrue(authSessionService.isValid(firstToken, 1001L));
        assertTrue(authSessionService.isValid(secondToken, 1001L));
    }

    @Test
    void shouldOnlyRevokeTheCurrentSession() {
        String firstToken = jwtUtil.generate("1001");
        String secondToken = jwtUtil.generate("1001");
        authSessionService.createSession("1001", firstToken);
        authSessionService.createSession("1001", secondToken);

        authSessionService.revoke(firstToken);

        assertFalse(authSessionService.isValid(firstToken));
        assertTrue(authSessionService.isValid(secondToken));
    }
}
