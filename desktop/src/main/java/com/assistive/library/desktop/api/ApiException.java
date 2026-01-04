package com.assistive.library.desktop.api;

public class ApiException extends RuntimeException {
  private final int statusCode;
  private final String responseBody;

  public ApiException(int statusCode, String responseBody) {
    super("Request failed with status " + statusCode);
    this.statusCode = statusCode;
    this.responseBody = responseBody;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getResponseBody() {
    return responseBody;
  }
}
