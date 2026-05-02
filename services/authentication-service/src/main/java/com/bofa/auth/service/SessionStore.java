package com.bofa.auth.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionStore {

    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    public void createSession(String sessionToken, String username, String jwtToken) {
        SessionInfo info = new SessionInfo(username, jwtToken, Instant.now(), false);
        activeSessions.put(sessionToken, info);
    }

    public SessionInfo getSession(String sessionToken) {
        return activeSessions.get(sessionToken);
    }

    public boolean isSessionActive(String sessionToken) {
        SessionInfo info = activeSessions.get(sessionToken);
        return info != null && !info.isInvalidated();
    }

    public void invalidateSession(String sessionToken) {
        SessionInfo info = activeSessions.get(sessionToken);
        if (info != null) {
            info.setInvalidated(true);
        }
    }

    public void markMfaVerified(String sessionToken) {
        SessionInfo info = activeSessions.get(sessionToken);
        if (info != null) {
            info.setMfaVerified(true);
        }
    }

    public boolean isMfaVerified(String sessionToken) {
        SessionInfo info = activeSessions.get(sessionToken);
        return info != null && info.isMfaVerified();
    }

    public static class SessionInfo {
        private final String username;
        private final String jwtToken;
        private final Instant createdAt;
        private boolean invalidated;
        private boolean mfaVerified;

        public SessionInfo(String username, String jwtToken, Instant createdAt, boolean mfaVerified) {
            this.username = username;
            this.jwtToken = jwtToken;
            this.createdAt = createdAt;
            this.mfaVerified = mfaVerified;
            this.invalidated = false;
        }

        public String getUsername() { return username; }
        public String getJwtToken() { return jwtToken; }
        public Instant getCreatedAt() { return createdAt; }
        public boolean isInvalidated() { return invalidated; }
        public void setInvalidated(boolean invalidated) { this.invalidated = invalidated; }
        public boolean isMfaVerified() { return mfaVerified; }
        public void setMfaVerified(boolean mfaVerified) { this.mfaVerified = mfaVerified; }
    }
}
