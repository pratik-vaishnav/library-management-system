package com.assistive.library.desktop.api;

import java.time.Instant;

public class SyncPullResponse {
  private Instant serverTime;
  private java.util.List<BookSyncItem> books;
  private java.util.List<MemberSyncItem> members;
  private java.util.List<LoanSyncItem> loans;

  public Instant getServerTime() {
    return serverTime;
  }

  public void setServerTime(Instant serverTime) {
    this.serverTime = serverTime;
  }

  public java.util.List<BookSyncItem> getBooks() {
    return books;
  }

  public void setBooks(java.util.List<BookSyncItem> books) {
    this.books = books;
  }

  public java.util.List<MemberSyncItem> getMembers() {
    return members;
  }

  public void setMembers(java.util.List<MemberSyncItem> members) {
    this.members = members;
  }

  public java.util.List<LoanSyncItem> getLoans() {
    return loans;
  }

  public void setLoans(java.util.List<LoanSyncItem> loans) {
    this.loans = loans;
  }
}
