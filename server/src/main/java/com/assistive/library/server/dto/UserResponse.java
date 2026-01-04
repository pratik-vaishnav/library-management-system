package com.assistive.library.server.dto;

import com.assistive.library.server.model.Role;

public class UserResponse {
  private Long id;
  private String username;
  private Role role;
  private boolean active;

  public UserResponse(Long id, String username, Role role, boolean active) {
    this.id = id;
    this.username = username;
    this.role = role;
    this.active = active;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public Role getRole() {
    return role;
  }

  public boolean isActive() {
    return active;
  }
}
