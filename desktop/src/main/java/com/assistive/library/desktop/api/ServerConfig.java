package com.assistive.library.desktop.api;

public final class ServerConfig {
  private static final String DEFAULT_URL = "http://localhost:8080";

  private ServerConfig() {
  }

  public static String baseUrl() {
    String env = System.getenv("ASSISTIVE_SERVER_URL");
    if (env != null && !env.isBlank()) {
      return env.trim();
    }
    return DEFAULT_URL;
  }
}
