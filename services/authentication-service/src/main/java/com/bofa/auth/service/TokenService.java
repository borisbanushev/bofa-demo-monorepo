package com.bofa.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public TokenService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username, Map<String, Object> additionalClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .issuer("bofa-auth-service");

        if (additionalClaims != null) {
            additionalClaims.forEach(builder::claim);
        }

        return builder.signWith(signingKey).compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Token has expired", e);
        } catch (Exception e) {
            throw new InvalidTokenException("Token is invalid", e);
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (TokenExpiredException e) {
            return true;
        } catch (Exception e) {
            throw new InvalidTokenException("Cannot determine token expiry", e);
        }
    }

    public String refreshToken(String token) {
        Claims claims = validateToken(token);
        Map<String, Object> newClaims = new HashMap<>(claims);
        newClaims.remove("sub");
        newClaims.remove("iat");
        newClaims.remove("exp");
        newClaims.remove("iss");
        return generateToken(claims.getSubject(), newClaims);
    }

    public static class TokenExpiredException extends RuntimeException {
        public TokenExpiredException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
