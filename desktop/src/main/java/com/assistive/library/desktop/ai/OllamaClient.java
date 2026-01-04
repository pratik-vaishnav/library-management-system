package com.assistive.library.desktop.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OllamaClient {
  private final HttpClient httpClient;
  private final ObjectMapper mapper;
  private final String baseUrl;

  public OllamaClient(String baseUrl) {
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(4))
        .build();
    this.mapper = new ObjectMapper();
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  public List<String> listModels() throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/tags"))
        .timeout(Duration.ofSeconds(10))
        .GET()
        .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("Ollama tags request failed with status " + response.statusCode());
    }
    JsonNode root = mapper.readTree(response.body());
    JsonNode models = root.get("models");
    List<String> results = new ArrayList<>();
    if (models != null && models.isArray()) {
      for (JsonNode model : models) {
        JsonNode name = model.get("name");
        if (name != null) {
          results.add(name.asText());
        }
      }
    }
    return results;
  }

  public String generate(String model, String prompt) throws IOException, InterruptedException {
    Map<String, Object> payload = Map.of(
        "model", model,
        "prompt", prompt,
        "stream", false,
        "options", Map.of("temperature", 0.3)
    );
    String body = mapper.writeValueAsString(payload);
    HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/generate"))
        .timeout(Duration.ofSeconds(60))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("Ollama generate failed with status " + response.statusCode());
    }
    JsonNode root = mapper.readTree(response.body());
    JsonNode text = root.get("response");
    return text == null ? "" : text.asText();
  }
}
