package com.bofa.auth.model;

public class MfaVerifyRequest {
    private String sessionToken;
    private String oneTimeCode;

    public MfaVerifyRequest() {}

    public MfaVerifyRequest(String sessionToken, String oneTimeCode) {
        this.sessionToken = sessionToken;
        this.oneTimeCode = oneTimeCode;
    }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public String getOneTimeCode() { return oneTimeCode; }
    public void setOneTimeCode(String oneTimeCode) { this.oneTimeCode = oneTimeCode; }
}
