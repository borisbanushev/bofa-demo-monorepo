package com.bofa.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compliance-critical tests for SessionStore — session invalidation and MFA state.
 * OCC regulatory examination artifact.
 */
class SessionStoreComplianceTest {

    private SessionStore sessionStore;

    @BeforeEach
    void setUp() {
        sessionStore = new SessionStore();
    }

    @Nested
    @DisplayName("Session Lifecycle")
    class SessionLifecycle {

        @Test
        @DisplayName("newly created session is active")
        void createSession_newSession_isActive() {
            sessionStore.createSession("sess-1", "bankuser", "jwt-token");
            assertTrue(sessionStore.isSessionActive("sess-1"));
        }

        @Test
        @DisplayName("getSession returns the stored session info")
        void getSession_existingSession_returnsInfo() {
            sessionStore.createSession("sess-1", "bankuser", "jwt-token");
            SessionStore.SessionInfo info = sessionStore.getSession("sess-1");

            assertNotNull(info);
            assertEquals("bankuser", info.getUsername());
            assertEquals("jwt-token", info.getJwtToken());
            assertNotNull(info.getCreatedAt());
        }

        @Test
        @DisplayName("getSession returns null for non-existent session")
        void getSession_nonExistent_returnsNull() {
            assertNull(sessionStore.getSession("does-not-exist"));
        }

        @Test
        @DisplayName("isSessionActive returns false for non-existent session")
        void isSessionActive_nonExistent_returnsFalse() {
            assertFalse(sessionStore.isSessionActive("does-not-exist"));
        }
    }

    @Nested
    @DisplayName("Session Invalidation")
    class SessionInvalidation {

        @Test
        @DisplayName("invalidated session is no longer active")
        void invalidateSession_activeSession_becomesInactive() {
            sessionStore.createSession("sess-1", "bankuser", "jwt-token");
            assertTrue(sessionStore.isSessionActive("sess-1"));

            sessionStore.invalidateSession("sess-1");
            assertFalse(sessionStore.isSessionActive("sess-1"));
        }

        @Test
        @DisplayName("session info still exists after invalidation but is flagged")
        void invalidateSession_sessionInfoStillExists() {
            sessionStore.createSession("sess-1", "bankuser", "jwt-token");
            sessionStore.invalidateSession("sess-1");

            SessionStore.SessionInfo info = sessionStore.getSession("sess-1");
            assertNotNull(info);
            assertTrue(info.isInvalidated());
        }

        @Test
        @DisplayName("invalidating a non-existent session does not throw")
        void invalidateSession_nonExistent_noException() {
            assertDoesNotThrow(() -> sessionStore.invalidateSession("does-not-exist"));
        }

        @Test
        @DisplayName("invalidating an already-invalidated session is idempotent")
        void invalidateSession_alreadyInvalidated_remainsInvalidated() {
            sessionStore.createSession("sess-1", "bankuser", "jwt-token");
            sessionStore.invalidateSession("sess-1");
            sessionStore.invalidateSession("sess-1");

            assertFalse(sessionStore.isSessionActive("sess-1"));
            assertTrue(sessionStore.getSession("sess-1").isInvalidated());
        }

        @Test
        @DisplayName("invalidating one session does not affect other sessions")
        void invalidateSession_doesNotAffectOtherSessions() {
            sessionStore.createSession("sess-1", "user1", "jwt-1");
            sessionStore.createSession("sess-2", "user2", "jwt-2");

            sessionStore.invalidateSession("sess-1");

            assertFalse(sessionStore.isSessionActive("sess-1"));
            assertTrue(sessionStore.isSessionActive("sess-2"));
        }
    }

    @Nested
    @DisplayName("MFA Verification State")
    class MfaVerification {

        @Test
        @DisplayName("newly created session is not MFA verified")
        void newSession_isNotMfaVerified() {
            sessionStore.createSession("sess-1", "bankuser", "jwt-token");
            assertFalse(sessionStore.isMfaVerified("sess-1"));
        }

        @Test
        @DisplayName("marking MFA verified sets the correct state")
        void markMfaVerified_setsVerifiedState() {
            sessionStore.createSession("sess-1", "bankuser", "jwt-token");
            sessionStore.markMfaVerified("sess-1");
            assertTrue(sessionStore.isMfaVerified("sess-1"));
        }

        @Test
        @DisplayName("isMfaVerified returns false for non-existent session")
        void isMfaVerified_nonExistent_returnsFalse() {
            assertFalse(sessionStore.isMfaVerified("does-not-exist"));
        }

        @Test
        @DisplayName("marking MFA verified on non-existent session does not throw")
        void markMfaVerified_nonExistent_noException() {
            assertDoesNotThrow(() -> sessionStore.markMfaVerified("does-not-exist"));
        }
    }
}
