package com.bofa.auth.model;

public class TokenResponse {
    private String token;
    private long expiresIn;
    private String tokenType;

    public TokenResponse() {}

    public TokenResponse(String token, long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.tokenType = "Bearer";
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
}
