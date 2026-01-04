package com.assistive.library.server.dto;

import com.assistive.library.server.model.Role;

public class UpdateUserRequest {
  private Role role;
  private Boolean active;
  private String password;

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
