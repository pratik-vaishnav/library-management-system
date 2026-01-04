package com.assistive.library.server.dto;

import com.assistive.library.server.model.Role;
import java.time.Instant;

public class LoginResponse {
  private String token;
  private String username;
  private Role role;
  private Instant expiresAt;

  public LoginResponse(String token, String username, Role role, Instant expiresAt) {
    this.token = token;
    this.username = username;
    this.role = role;
    this.expiresAt = expiresAt;
  }

  public String getToken() {
    return token;
  }

  public String getUsername() {
    return username;
  }

  public Role getRole() {
    return role;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }
}
