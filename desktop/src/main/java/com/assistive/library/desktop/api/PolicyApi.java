package com.assistive.library.desktop.api;

import java.io.IOException;

public class PolicyApi {
  private final ApiClient apiClient;

  public PolicyApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public PolicyResponse getPolicy() throws IOException, InterruptedException {
    return apiClient.get("/api/policy", PolicyResponse.class);
  }
}
