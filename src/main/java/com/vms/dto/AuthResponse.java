package com.vms.dto;

import java.util.List;

/**
 * Standardized authentication response containing JWT access token and user identity metadata.
 * Passwords are never included in authentication responses.
 */
public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private long expiresIn;
    private String username;
    private List<String> roles;

    public AuthResponse() {
    }

    public AuthResponse(String token, String type, long expiresIn, String username, List<String> roles) {
        this.token = token;
        this.type = type;
        this.expiresIn = expiresIn;
        this.username = username;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "username='" + username + '\'' +
                ", type='" + type + '\'' +
                ", expiresIn=" + expiresIn +
                ", roles=" + roles +
                '}';
    }
}
