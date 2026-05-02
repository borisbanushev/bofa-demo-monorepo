package com.bofa.auth.controller;

import com.bofa.auth.model.*;
import com.bofa.auth.service.SessionStore;
import com.bofa.auth.service.TokenService;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String DEMO_USERNAME = "bankuser";
    private static final String DEMO_PASSWORD = "secure123";
    private static final String DEMO_MFA_CODE = "123456";

    private final TokenService tokenService;
    private final SessionStore sessionStore;

    public AuthController(TokenService tokenService, SessionStore sessionStore) {
        this.tokenService = tokenService;
        this.sessionStore = sessionStore;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (!DEMO_USERNAME.equals(request.getUsername()) || !DEMO_PASSWORD.equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "CUSTOMER");
        claims.put("customerId", "CUST-001");

        String token = tokenService.generateToken(request.getUsername(), claims);
        String sessionToken = UUID.randomUUID().toString();
        sessionStore.createSession(sessionToken, request.getUsername(), token);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("sessionToken", sessionToken);
        response.put("expiresIn", 3600);
        response.put("tokenType", "Bearer");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestBody TokenValidationRequest request) {
        try {
            Claims claims = tokenService.validateToken(request.getToken());

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("username", claims.getSubject());
            response.put("role", claims.get("role"));
            response.put("customerId", claims.get("customerId"));
            response.put("expiresAt", claims.getExpiration().getTime());

            return ResponseEntity.ok(response);
        } catch (TokenService.TokenExpiredException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Token expired"));
        } catch (TokenService.InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Invalid token"));
        }
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaVerifyRequest request) {
        if (!sessionStore.isSessionActive(request.getSessionToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired session"));
        }

        if (!DEMO_MFA_CODE.equals(request.getOneTimeCode())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid MFA code"));
        }

        sessionStore.markMfaVerified(request.getSessionToken());

        return ResponseEntity.ok(Map.of(
                "verified", true,
                "sessionToken", request.getSessionToken(),
                "message", "MFA verification successful"
        ));
    }

    @PostMapping("/mfa/fallback")
    public ResponseEntity<?> mfaFallback(@RequestBody MfaFallbackRequest request) {
        if (!sessionStore.isSessionActive(request.getSessionToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired session"));
        }

        String fallbackMethod = request.getFallbackMethod();
        if (fallbackMethod == null || (!fallbackMethod.equals("email") && !fallbackMethod.equals("sms"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid fallback method. Use 'email' or 'sms'"));
        }

        String verificationId = UUID.randomUUID().toString();

        return ResponseEntity.ok(Map.of(
                "fallbackInitiated", true,
                "method", fallbackMethod,
                "verificationId", verificationId,
                "message", "Verification code sent via " + fallbackMethod + " to " + maskContact(request.getContactInfo())
        ));
    }

    @PostMapping("/session/invalidate")
    public ResponseEntity<?> invalidateSession(@RequestBody SessionInvalidateRequest request) {
        if (!sessionStore.isSessionActive(request.getSessionToken())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Session not found or already invalidated"));
        }

        sessionStore.invalidateSession(request.getSessionToken());

        return ResponseEntity.ok(Map.of(
                "invalidated", true,
                "message", "Session has been invalidated"
        ));
    }

    private String maskContact(String contact) {
        if (contact == null || contact.length() < 4) {
            return "****";
        }
        return "****" + contact.substring(contact.length() - 4);
    }
}
