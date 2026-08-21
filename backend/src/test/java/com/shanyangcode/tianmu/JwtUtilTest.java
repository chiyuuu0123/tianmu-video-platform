package com.shanyangcode.tianmu;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.shanyangcode.tianmu.utils.JwtUtil;

class JwtUtilTest {

    @Test
    void shouldGenerateAndParseTokenWithConfiguredSecret() {
        String secret = Base64.getEncoder()
                .encodeToString("0123456789abcdef0123456789abcdef".getBytes(UTF_8));
        JwtUtil jwtUtil = new JwtUtil(secret, 48);

        String token = jwtUtil.generate("test-user-1");

        assertNotNull(token);
        assertEquals("test-user-1", jwtUtil.parse(token).getSubject());
        assertEquals(48, jwtUtil.getExpirationHours());
        assertNull(jwtUtil.parse("invalid-token"));
    }
}
