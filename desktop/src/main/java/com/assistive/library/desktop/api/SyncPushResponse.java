package com.assistive.library.desktop.api;

public class SyncPushResponse {
  private int booksAccepted;
  private int membersAccepted;
  private int loansAccepted;
  private java.util.List<SyncItemResult> results;

  public int getBooksAccepted() {
    return booksAccepted;
  }

  public void setBooksAccepted(int booksAccepted) {
    this.booksAccepted = booksAccepted;
  }

  public int getMembersAccepted() {
    return membersAccepted;
  }

  public void setMembersAccepted(int membersAccepted) {
    this.membersAccepted = membersAccepted;
  }

  public int getLoansAccepted() {
    return loansAccepted;
  }

  public void setLoansAccepted(int loansAccepted) {
    this.loansAccepted = loansAccepted;
  }

  public java.util.List<SyncItemResult> getResults() {
    return results;
  }

  public void setResults(java.util.List<SyncItemResult> results) {
    this.results = results;
  }
}
