package com.cluj1.eventapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.cluj1.eventapp.model.User;
import com.cluj1.eventapp.model.enums.Role;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private User user;

    private static final String SECRET = "SuperSecretKeyThatIsAtLeast32BytesLongForHS256Algorithm";

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 3_600_000L);

        user = User.builder()
                .id(UUID.randomUUID())
                .email("john.doe@msg.group")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    void generateToken_returnsNonEmptyJwt_whenGivenValidUser() {
        String token = provider.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void validateToken_returnsTrue_forFreshlyGeneratedToken() {
        String token = provider.generateToken(user);

        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_returnsFalse_forMalformedToken() {
        assertThat(provider.validateToken("not-a-token")).isFalse();
    }

    @Test
    void validateToken_returnsFalse_forNullToken() {
        assertThat(provider.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_returnsFalse_forEmptyToken() {
        assertThat(provider.validateToken("")).isFalse();
    }

    @Test
    void validateToken_returnsFalse_forTokenSignedWithDifferentSecret() {
        JwtTokenProvider other = new JwtTokenProvider();
        ReflectionTestUtils.setField(other, "jwtSecret",
                "DifferentSecretKeyThatIsAtLeast32BytesLongForHS256!");
        ReflectionTestUtils.setField(other, "jwtExpirationMs", 3_600_000L);
        String token = other.generateToken(user);

        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_returnsFalse_forExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 1L);
        String token = provider.generateToken(user);
        Thread.sleep(50);

        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void getUserIdFromJwt_returnsSubject_matchingUserId() {
        String token = provider.generateToken(user);

        String userId = provider.getUserIdFromJwt(token);

        assertThat(userId).isEqualTo(user.getId().toString());
    }
}

