package com.assistive.library.desktop.api;

import java.io.IOException;

public class AuthApi {
  private final ApiClient apiClient;

  public AuthApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public AuthResponse login(String username, String password) throws IOException, InterruptedException {
    LoginPayload payload = new LoginPayload(username, password);
    return apiClient.post("/api/auth/login", payload, AuthResponse.class);
  }

  private record LoginPayload(String username, String password) {
  }
}
