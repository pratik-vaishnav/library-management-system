package com.assistive.library.desktop.api;

import java.io.IOException;

public class UsersApi {
  private final ApiClient apiClient;

  public UsersApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public UserResponse[] listUsers() throws IOException, InterruptedException {
    return apiClient.get("/api/admin/users", UserResponse[].class);
  }

  public UserResponse createUser(CreateUserRequest request) throws IOException, InterruptedException {
    return apiClient.post("/api/admin/users", request, UserResponse.class);
  }

  public UserResponse updateUser(long id, UpdateUserRequest request) throws IOException, InterruptedException {
    return apiClient.put("/api/admin/users/" + id, request, UserResponse.class);
  }
}
