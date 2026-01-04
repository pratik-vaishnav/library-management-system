package com.assistive.library.server.dto;

public class SyncPushResponse {
  private int booksAccepted;
  private int membersAccepted;
  private int loansAccepted;
  private java.util.List<SyncItemResult> results;

  public SyncPushResponse(int booksAccepted, int membersAccepted, int loansAccepted, java.util.List<SyncItemResult> results) {
    this.booksAccepted = booksAccepted;
    this.membersAccepted = membersAccepted;
    this.loansAccepted = loansAccepted;
    this.results = results;
  }

  public int getBooksAccepted() {
    return booksAccepted;
  }

  public int getMembersAccepted() {
    return membersAccepted;
  }

  public int getLoansAccepted() {
    return loansAccepted;
  }

  public java.util.List<SyncItemResult> getResults() {
    return results;
  }
}
