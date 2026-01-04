package com.assistive.library.server.dto;

import java.util.List;

public class SyncPushRequest {
  private String deviceId;
  private List<BookSyncItem> books;
  private List<MemberSyncItem> members;
  private List<LoanSyncItem> loans;

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public List<BookSyncItem> getBooks() {
    return books;
  }

  public void setBooks(List<BookSyncItem> books) {
    this.books = books;
  }

  public List<MemberSyncItem> getMembers() {
    return members;
  }

  public void setMembers(List<MemberSyncItem> members) {
    this.members = members;
  }

  public List<LoanSyncItem> getLoans() {
    return loans;
  }

  public void setLoans(List<LoanSyncItem> loans) {
    this.loans = loans;
  }
}
