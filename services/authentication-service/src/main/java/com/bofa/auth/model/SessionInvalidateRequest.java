package com.bofa.auth.model;

public class SessionInvalidateRequest {
    private String sessionToken;

    public SessionInvalidateRequest() {}

    public SessionInvalidateRequest(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}
