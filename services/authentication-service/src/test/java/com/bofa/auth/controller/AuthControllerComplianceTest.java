package com.bofa.auth.controller;

import com.bofa.auth.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Compliance-critical integration tests for AuthController endpoints:
 * token validation/expiry, MFA fallback, and session invalidation.
 * OCC regulatory examination artifact.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerComplianceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken() throws Exception {
        LoginRequest login = new LoginRequest("bankuser", "secure123");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {});
        return (String) body.get("token");
    }

    private String loginAndGetSessionToken() throws Exception {
        LoginRequest login = new LoginRequest("bankuser", "secure123");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {});
        return (String) body.get("sessionToken");
    }

    @Nested
    @DisplayName("Token Validation Endpoint — Expiry Handling")
    class TokenValidationEndpoint {

        @Test
        @DisplayName("returns valid=true for a fresh token")
        void validate_validToken_returnsValid() throws Exception {
            String token = loginAndGetToken();

            mockMvc.perform(post("/auth/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TokenValidationRequest(token))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.username").value("bankuser"))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.customerId").value("CUST-001"))
                    .andExpect(jsonPath("$.expiresAt").isNumber());
        }

        @Test
        @DisplayName("returns 401 with 'Token expired' for an expired token")
        void validate_expiredToken_returns401() throws Exception {
            mockMvc.perform(post("/auth/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\": \"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJiYW5rdXNlciIsImlhdCI6MTcwMDAwMDAwMCwiZXhwIjoxNzAwMDAwMDAxLCJpc3MiOiJib2ZhLWF1dGgtc2VydmljZSJ9.invalid\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.valid").value(false));
        }

        @Test
        @DisplayName("returns 401 with 'Invalid token' for a garbage token")
        void validate_garbageToken_returns401() throws Exception {
            mockMvc.perform(post("/auth/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\": \"not.a.real.jwt\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.error").value("Invalid token"));
        }
    }

    @Nested
    @DisplayName("MFA Fallback Endpoint")
    class MfaFallbackEndpoint {

        @Test
        @DisplayName("initiates email fallback for active session")
        void mfaFallback_emailMethod_returnsSuccess() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            MfaFallbackRequest request = new MfaFallbackRequest(sessionToken, "email", "user@bank.com");
            mockMvc.perform(post("/auth/mfa/fallback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fallbackInitiated").value(true))
                    .andExpect(jsonPath("$.method").value("email"))
                    .andExpect(jsonPath("$.verificationId").exists())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("initiates SMS fallback for active session")
        void mfaFallback_smsMethod_returnsSuccess() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            MfaFallbackRequest request = new MfaFallbackRequest(sessionToken, "sms", "+15551234567");
            mockMvc.perform(post("/auth/mfa/fallback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fallbackInitiated").value(true))
                    .andExpect(jsonPath("$.method").value("sms"))
                    .andExpect(jsonPath("$.verificationId").exists());
        }

        @Test
        @DisplayName("returns 401 for inactive/non-existent session")
        void mfaFallback_invalidSession_returns401() throws Exception {
            MfaFallbackRequest request = new MfaFallbackRequest("invalid-session", "email", "user@bank.com");
            mockMvc.perform(post("/auth/mfa/fallback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid or expired session"));
        }

        @Test
        @DisplayName("returns 400 for invalid fallback method")
        void mfaFallback_invalidMethod_returns400() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            MfaFallbackRequest request = new MfaFallbackRequest(sessionToken, "pigeon", "user@bank.com");
            mockMvc.perform(post("/auth/mfa/fallback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid fallback method. Use 'email' or 'sms'"));
        }

        @Test
        @DisplayName("returns 400 when fallback method is null")
        void mfaFallback_nullMethod_returns400() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            MfaFallbackRequest request = new MfaFallbackRequest(sessionToken, null, "user@bank.com");
            mockMvc.perform(post("/auth/mfa/fallback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid fallback method. Use 'email' or 'sms'"));
        }

        @Test
        @DisplayName("masks contact info in response message")
        void mfaFallback_masksContactInfo() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            MfaFallbackRequest request = new MfaFallbackRequest(sessionToken, "email", "user@bank.com");
            mockMvc.perform(post("/auth/mfa/fallback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Verification code sent via email to ****.com"));
        }

        @Test
        @DisplayName("masks short contact info")
        void mfaFallback_masksShortContact() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            MfaFallbackRequest request = new MfaFallbackRequest(sessionToken, "sms", "12");
            mockMvc.perform(post("/auth/mfa/fallback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Verification code sent via sms to ****"));
        }

        @Test
        @DisplayName("returns 401 for invalidated session")
        void mfaFallback_invalidatedSession_returns401() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            // Invalidate the session first
            mockMvc.perform(post("/auth/session/invalidate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SessionInvalidateRequest(sessionToken))))
                    .andExpect(status().isOk());

            // Now try MFA fallback on the invalidated session
            MfaFallbackRequest request = new MfaFallbackRequest(sessionToken, "email", "user@bank.com");
            mockMvc.perform(post("/auth/mfa/fallback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid or expired session"));
        }
    }

    @Nested
    @DisplayName("MFA Verify Endpoint")
    class MfaVerifyEndpoint {

        @Test
        @DisplayName("verifies MFA with correct code")
        void mfaVerify_correctCode_returnsSuccess() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            MfaVerifyRequest request = new MfaVerifyRequest(sessionToken, "123456");
            mockMvc.perform(post("/auth/mfa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verified").value(true))
                    .andExpect(jsonPath("$.sessionToken").value(sessionToken))
                    .andExpect(jsonPath("$.message").value("MFA verification successful"));
        }

        @Test
        @DisplayName("rejects MFA with incorrect code")
        void mfaVerify_incorrectCode_returns401() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            MfaVerifyRequest request = new MfaVerifyRequest(sessionToken, "000000");
            mockMvc.perform(post("/auth/mfa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid MFA code"));
        }

        @Test
        @DisplayName("rejects MFA for non-existent session")
        void mfaVerify_invalidSession_returns401() throws Exception {
            MfaVerifyRequest request = new MfaVerifyRequest("non-existent-session", "123456");
            mockMvc.perform(post("/auth/mfa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid or expired session"));
        }

        @Test
        @DisplayName("rejects MFA for invalidated session")
        void mfaVerify_invalidatedSession_returns401() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            // Invalidate the session
            mockMvc.perform(post("/auth/session/invalidate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SessionInvalidateRequest(sessionToken))))
                    .andExpect(status().isOk());

            // Try MFA verify on invalidated session
            MfaVerifyRequest request = new MfaVerifyRequest(sessionToken, "123456");
            mockMvc.perform(post("/auth/mfa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid or expired session"));
        }
    }

    @Nested
    @DisplayName("Session Invalidation Endpoint")
    class SessionInvalidationEndpoint {

        @Test
        @DisplayName("invalidates an active session")
        void invalidateSession_activeSession_returnsSuccess() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            mockMvc.perform(post("/auth/session/invalidate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SessionInvalidateRequest(sessionToken))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.invalidated").value(true))
                    .andExpect(jsonPath("$.message").value("Session has been invalidated"));
        }

        @Test
        @DisplayName("returns 404 for non-existent session")
        void invalidateSession_nonExistent_returns404() throws Exception {
            mockMvc.perform(post("/auth/session/invalidate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SessionInvalidateRequest("does-not-exist"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Session not found or already invalidated"));
        }

        @Test
        @DisplayName("returns 404 for already-invalidated session")
        void invalidateSession_alreadyInvalidated_returns404() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            // First invalidation succeeds
            mockMvc.perform(post("/auth/session/invalidate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SessionInvalidateRequest(sessionToken))))
                    .andExpect(status().isOk());

            // Second invalidation returns 404
            mockMvc.perform(post("/auth/session/invalidate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SessionInvalidateRequest(sessionToken))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Session not found or already invalidated"));
        }

        @Test
        @DisplayName("invalidated session blocks subsequent MFA verification")
        void invalidateSession_blocksMfaVerify() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            // Invalidate session
            mockMvc.perform(post("/auth/session/invalidate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SessionInvalidateRequest(sessionToken))))
                    .andExpect(status().isOk());

            // MFA verify must fail
            MfaVerifyRequest mfaRequest = new MfaVerifyRequest(sessionToken, "123456");
            mockMvc.perform(post("/auth/mfa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(mfaRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("invalidated session blocks subsequent MFA fallback")
        void invalidateSession_blocksMfaFallback() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            // Invalidate session
            mockMvc.perform(post("/auth/session/invalidate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SessionInvalidateRequest(sessionToken))))
                    .andExpect(status().isOk());

            // MFA fallback must fail
            MfaFallbackRequest fallbackRequest = new MfaFallbackRequest(sessionToken, "email", "user@bank.com");
            mockMvc.perform(post("/auth/mfa/fallback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(fallbackRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Suspended Account Mid-Session — OCC Edge Case")
    class SuspendedAccountMidSession {

        @Test
        @DisplayName("valid token still passes /validate after session is invalidated (suspended)")
        void suspendedAccount_tokenStillValidates() throws Exception {
            String token = loginAndGetToken();
            String sessionToken = loginAndGetSessionToken();

            // Suspend the account by invalidating the session mid-flight
            mockMvc.perform(post("/auth/session/invalidate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SessionInvalidateRequest(sessionToken))))
                    .andExpect(status().isOk());

            // The JWT itself is still cryptographically valid and not expired
            mockMvc.perform(post("/auth/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TokenValidationRequest(token))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));
        }

        @Test
        @DisplayName("suspended account blocks MFA verify even with correct code")
        void suspendedAccount_blocksMfaVerify() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            // Suspend the account mid-session
            mockMvc.perform(post("/auth/session/invalidate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SessionInvalidateRequest(sessionToken))))
                    .andExpect(status().isOk());

            // MFA verify with correct code must still be rejected
            MfaVerifyRequest mfaRequest = new MfaVerifyRequest(sessionToken, "123456");
            mockMvc.perform(post("/auth/mfa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(mfaRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid or expired session"));
        }

        @Test
        @DisplayName("suspended account blocks MFA fallback initiation")
        void suspendedAccount_blocksMfaFallback() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            // Suspend the account mid-session
            mockMvc.perform(post("/auth/session/invalidate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SessionInvalidateRequest(sessionToken))))
                    .andExpect(status().isOk());

            // MFA fallback must be rejected for suspended account
            MfaFallbackRequest fallbackRequest = new MfaFallbackRequest(sessionToken, "email", "user@bank.com");
            mockMvc.perform(post("/auth/mfa/fallback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(fallbackRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid or expired session"));
        }

        @Test
        @DisplayName("suspended account cannot be re-invalidated (returns 404)")
        void suspendedAccount_cannotBeReInvalidated() throws Exception {
            String sessionToken = loginAndGetSessionToken();

            // First suspension
            mockMvc.perform(post("/auth/session/invalidate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SessionInvalidateRequest(sessionToken))))
                    .andExpect(status().isOk());

            // Attempting to suspend again returns 404
            mockMvc.perform(post("/auth/session/invalidate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SessionInvalidateRequest(sessionToken))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Session not found or already invalidated"));
        }
    }
}
