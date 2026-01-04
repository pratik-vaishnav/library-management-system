package com.assistive.library.server.dto;

import java.time.Instant;
import java.util.List;

public class SyncPullResponse {
  private Instant serverTime;
  private List<BookSyncItem> books;
  private List<MemberSyncItem> members;
  private List<LoanSyncItem> loans;

  public SyncPullResponse(Instant serverTime,
                          List<BookSyncItem> books,
                          List<MemberSyncItem> members,
                          List<LoanSyncItem> loans) {
    this.serverTime = serverTime;
    this.books = books;
    this.members = members;
    this.loans = loans;
  }

  public Instant getServerTime() {
    return serverTime;
  }

  public List<BookSyncItem> getBooks() {
    return books;
  }

  public List<MemberSyncItem> getMembers() {
    return members;
  }

  public List<LoanSyncItem> getLoans() {
    return loans;
  }
}
