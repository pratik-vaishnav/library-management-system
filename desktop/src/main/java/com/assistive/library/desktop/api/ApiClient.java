package com.assistive.library.desktop.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiClient {
  private final HttpClient httpClient;
  private final ObjectMapper mapper;
  private final String baseUrl;
  private String authToken;

  public ApiClient(String baseUrl) {
    this.httpClient = HttpClient.newBuilder().build();
    this.mapper = new ObjectMapper();
    this.mapper.registerModule(new JavaTimeModule());
    this.baseUrl = baseUrl;
  }

  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public <T> T post(String path, Object body, Class<T> responseType) throws IOException, InterruptedException {
    String payload = mapper.writeValueAsString(body);
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(payload));
    applyAuth(builder);
    HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    return handleResponse(response, responseType);
  }

  public <T> T get(String path, Class<T> responseType) throws IOException, InterruptedException {
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).GET();
    applyAuth(builder);
    HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    return handleResponse(response, responseType);
  }

  public <T> T put(String path, Object body, Class<T> responseType) throws IOException, InterruptedException {
    String payload = mapper.writeValueAsString(body);
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
        .header("Content-Type", "application/json")
        .PUT(HttpRequest.BodyPublishers.ofString(payload));
    applyAuth(builder);
    HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    return handleResponse(response, responseType);
  }

  private void applyAuth(HttpRequest.Builder builder) {
    if (authToken != null && !authToken.isBlank()) {
      builder.header("X-Auth-Token", authToken);
    }
  }

  private <T> T handleResponse(HttpResponse<String> response, Class<T> responseType) throws IOException {
    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      if (responseType == Void.class) {
        return null;
      }
      return mapper.readValue(response.body(), responseType);
    }
    throw new ApiException(response.statusCode(), response.body());
  }
}
