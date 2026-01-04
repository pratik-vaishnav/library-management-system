package com.assistive.library.desktop.api;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class LookupApi {
  private final ApiClient apiClient;

  public LookupApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  public BookLookupResponse getBookByIsbn(String isbn) throws IOException, InterruptedException {
    String encoded = URLEncoder.encode(isbn, StandardCharsets.UTF_8);
    return apiClient.get("/api/books/isbn/" + encoded, BookLookupResponse.class);
  }

  public MemberLookupResponse getMemberById(String memberId) throws IOException, InterruptedException {
    String encoded = URLEncoder.encode(memberId, StandardCharsets.UTF_8);
    return apiClient.get("/api/members/member-id/" + encoded, MemberLookupResponse.class);
  }

  public LoanLookupResponse getLoanByExternalRef(String externalRef) throws IOException, InterruptedException {
    String encoded = URLEncoder.encode(externalRef, StandardCharsets.UTF_8);
    return apiClient.get("/api/loans/external/" + encoded, LoanLookupResponse.class);
  }
}
