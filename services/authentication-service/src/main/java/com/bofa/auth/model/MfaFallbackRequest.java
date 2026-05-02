package com.bofa.auth.model;

public class MfaFallbackRequest {
    private String sessionToken;
    private String fallbackMethod;
    private String contactInfo;

    public MfaFallbackRequest() {}

    public MfaFallbackRequest(String sessionToken, String fallbackMethod, String contactInfo) {
        this.sessionToken = sessionToken;
        this.fallbackMethod = fallbackMethod;
        this.contactInfo = contactInfo;
    }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public String getFallbackMethod() { return fallbackMethod; }
    public void setFallbackMethod(String fallbackMethod) { this.fallbackMethod = fallbackMethod; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
}
