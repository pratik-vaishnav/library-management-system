package com.assistive.library.desktop.api;

import java.io.IOException;

public class SettingsApi {
  private final ApiClient apiClient;

  public SettingsApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public SettingItem[] listSettings() throws IOException, InterruptedException {
    return apiClient.get("/api/settings", SettingItem[].class);
  }

  public SettingItem[] updateSettings(SettingItem[] items) throws IOException, InterruptedException {
    return apiClient.put("/api/settings", items, SettingItem[].class);
  }
}
