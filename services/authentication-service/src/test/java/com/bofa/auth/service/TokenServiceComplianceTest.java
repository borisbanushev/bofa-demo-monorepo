package com.bofa.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compliance-critical tests for TokenService — token expiry handling.
 * OCC regulatory examination artifact.
 */
class TokenServiceComplianceTest {

    private static final String SECRET = "ThisIsADemoSecretKeyForBofAAuthenticationServiceDoNotUseInProduction2024";
    private static final long EXPIRATION_MS = 3600000;

    private TokenService tokenService;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(SECRET, EXPIRATION_MS);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGeneration {

        @Test
        @DisplayName("generates token with correct subject")
        void generateToken_setsCorrectSubject() {
            String token = tokenService.generateToken("bankuser", null);
            Claims claims = tokenService.validateToken(token);
            assertEquals("bankuser", claims.getSubject());
        }

        @Test
        @DisplayName("generates token with correct issuer")
        void generateToken_setsCorrectIssuer() {
            String token = tokenService.generateToken("bankuser", null);
            Claims claims = tokenService.validateToken(token);
            assertEquals("bofa-auth-service", claims.getIssuer());
        }

        @Test
        @DisplayName("generates token with additional claims")
        void generateToken_includesAdditionalClaims() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "CUSTOMER");
            claims.put("customerId", "CUST-001");

            String token = tokenService.generateToken("bankuser", claims);
            Claims parsed = tokenService.validateToken(token);

            assertEquals("CUSTOMER", parsed.get("role"));
            assertEquals("CUST-001", parsed.get("customerId"));
        }

        @Test
        @DisplayName("generates token with future expiration date")
        void generateToken_setsFutureExpiration() {
            String token = tokenService.generateToken("bankuser", null);
            Claims claims = tokenService.validateToken(token);
            assertTrue(claims.getExpiration().after(new Date()));
        }

        @Test
        @DisplayName("generates token with null additional claims")
        void generateToken_handlesNullClaims() {
            String token = tokenService.generateToken("bankuser", null);
            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("generates token with empty additional claims map")
        void generateToken_handlesEmptyClaims() {
            String token = tokenService.generateToken("bankuser", new HashMap<>());
            Claims claims = tokenService.validateToken(token);
            assertEquals("bankuser", claims.getSubject());
        }
    }

    @Nested
    @DisplayName("Token Validation — Expiry Handling")
    class TokenExpiryHandling {

        @Test
        @DisplayName("validates a non-expired token successfully")
        void validateToken_validToken_returnsClaims() {
            String token = tokenService.generateToken("bankuser", null);
            Claims claims = tokenService.validateToken(token);

            assertNotNull(claims);
            assertEquals("bankuser", claims.getSubject());
        }

        @Test
        @DisplayName("throws TokenExpiredException for an expired token")
        void validateToken_expiredToken_throwsTokenExpiredException() {
            String expiredToken = buildExpiredToken("bankuser");

            TokenService.TokenExpiredException ex = assertThrows(
                    TokenService.TokenExpiredException.class,
                    () -> tokenService.validateToken(expiredToken)
            );
            assertTrue(ex.getMessage().contains("expired"));
        }

        @Test
        @DisplayName("throws InvalidTokenException for a tampered token")
        void validateToken_tamperedToken_throwsInvalidTokenException() {
            String validToken = tokenService.generateToken("bankuser", null);
            String tampered = validToken.substring(0, validToken.length() - 5) + "XXXXX";

            assertThrows(
                    TokenService.InvalidTokenException.class,
                    () -> tokenService.validateToken(tampered)
            );
        }

        @Test
        @DisplayName("throws InvalidTokenException for a completely invalid token string")
        void validateToken_garbageToken_throwsInvalidTokenException() {
            assertThrows(
                    TokenService.InvalidTokenException.class,
                    () -> tokenService.validateToken("not.a.valid.jwt")
            );
        }

        @Test
        @DisplayName("throws InvalidTokenException for a token signed with different key")
        void validateToken_wrongKeyToken_throwsInvalidTokenException() {
            String wrongKeySecret = "DifferentSecretKeyThatIsLongEnoughForHmacSha256Algorithm!!";
            SecretKey wrongKey = Keys.hmacShaKeyFor(wrongKeySecret.getBytes(StandardCharsets.UTF_8));

            String token = Jwts.builder()
                    .subject("bankuser")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 3600000))
                    .signWith(wrongKey)
                    .compact();

            assertThrows(
                    TokenService.InvalidTokenException.class,
                    () -> tokenService.validateToken(token)
            );
        }
    }

    @Nested
    @DisplayName("isTokenExpired")
    class IsTokenExpired {

        @Test
        @DisplayName("returns false for a valid non-expired token")
        void isTokenExpired_validToken_returnsFalse() {
            String token = tokenService.generateToken("bankuser", null);
            assertFalse(tokenService.isTokenExpired(token));
        }

        @Test
        @DisplayName("returns true for an expired token")
        void isTokenExpired_expiredToken_returnsTrue() {
            String expiredToken = buildExpiredToken("bankuser");
            assertTrue(tokenService.isTokenExpired(expiredToken));
        }

        @Test
        @DisplayName("throws InvalidTokenException for a tampered token")
        void isTokenExpired_tamperedToken_throwsInvalidTokenException() {
            assertThrows(
                    TokenService.InvalidTokenException.class,
                    () -> tokenService.isTokenExpired("not.valid.token")
            );
        }
    }

    @Nested
    @DisplayName("Token Refresh")
    class TokenRefresh {

        @Test
        @DisplayName("refreshed token has the same subject")
        void refreshToken_preservesSubject() {
            String original = tokenService.generateToken("bankuser", null);
            String refreshed = tokenService.refreshToken(original);

            Claims claims = tokenService.validateToken(refreshed);
            assertEquals("bankuser", claims.getSubject());
        }

        @Test
        @DisplayName("refreshed token has a new expiration date")
        void refreshToken_updatesExpiration() {
            String original = tokenService.generateToken("bankuser", null);
            Claims originalClaims = tokenService.validateToken(original);

            String refreshed = tokenService.refreshToken(original);
            Claims refreshedClaims = tokenService.validateToken(refreshed);

            assertTrue(
                    refreshedClaims.getExpiration().getTime() >= originalClaims.getExpiration().getTime()
            );
        }

        @Test
        @DisplayName("refreshed token preserves custom claims")
        void refreshToken_preservesCustomClaims() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "CUSTOMER");
            claims.put("customerId", "CUST-001");

            String original = tokenService.generateToken("bankuser", claims);
            String refreshed = tokenService.refreshToken(original);

            Claims refreshedClaims = tokenService.validateToken(refreshed);
            assertEquals("CUSTOMER", refreshedClaims.get("role"));
            assertEquals("CUST-001", refreshedClaims.get("customerId"));
        }

        @Test
        @DisplayName("throws TokenExpiredException when refreshing an expired token")
        void refreshToken_expiredToken_throwsTokenExpiredException() {
            String expiredToken = buildExpiredToken("bankuser");

            assertThrows(
                    TokenService.TokenExpiredException.class,
                    () -> tokenService.refreshToken(expiredToken)
            );
        }

        @Test
        @DisplayName("throws InvalidTokenException when refreshing an invalid token")
        void refreshToken_invalidToken_throwsInvalidTokenException() {
            assertThrows(
                    TokenService.InvalidTokenException.class,
                    () -> tokenService.refreshToken("garbage.token.value")
            );
        }
    }

    private String buildExpiredToken(String username) {
        Date past = new Date(System.currentTimeMillis() - 10000);
        Date expiredAt = new Date(System.currentTimeMillis() - 5000);
        return Jwts.builder()
                .subject(username)
                .issuedAt(past)
                .expiration(expiredAt)
                .issuer("bofa-auth-service")
                .signWith(signingKey)
                .compact();
    }
}
