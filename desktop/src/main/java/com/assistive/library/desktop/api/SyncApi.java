package com.assistive.library.desktop.api;

import java.io.IOException;

public class SyncApi {
  private final ApiClient apiClient;

  public SyncApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public SyncPullResponse pull() throws IOException, InterruptedException {
    return apiClient.get("/api/sync/pull", SyncPullResponse.class);
  }

  public SyncPushResponse push(SyncPushRequest request) throws IOException, InterruptedException {
    return apiClient.post("/api/sync/push", request, SyncPushResponse.class);
  }
}
