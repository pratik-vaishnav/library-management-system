package com.assistive.library.desktop.state;

import java.time.Instant;

public class SessionState {
  private String username;
  private String role;
  private String token;
  private Instant expiresAt;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public boolean isAuthenticated() {
    return token != null && !token.isBlank();
  }
}
