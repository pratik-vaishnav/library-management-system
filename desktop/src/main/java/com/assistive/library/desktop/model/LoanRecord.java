package com.assistive.library.desktop.model;

import java.math.BigDecimal;
import java.time.Instant;

public class LoanRecord {
  private long id;
  private long bookId;
  private long memberId;
  private String bookIsbn;
  private String bookTitle;
  private String memberIdentifier;
  private String memberName;
  private Instant issuedAt;
  private Instant dueAt;
  private Instant returnedAt;
  private String status;
  private BigDecimal fineAmount;
  private String externalRef;
  private String sourceDeviceId;
  private Instant updatedAt;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getBookId() {
    return bookId;
  }

  public void setBookId(long bookId) {
    this.bookId = bookId;
  }

  public long getMemberId() {
    return memberId;
  }

  public void setMemberId(long memberId) {
    this.memberId = memberId;
  }

  public String getBookIsbn() {
    return bookIsbn;
  }

  public void setBookIsbn(String bookIsbn) {
    this.bookIsbn = bookIsbn;
  }

  public String getBookTitle() {
    return bookTitle;
  }

  public void setBookTitle(String bookTitle) {
    this.bookTitle = bookTitle;
  }

  public String getMemberIdentifier() {
    return memberIdentifier;
  }

  public void setMemberIdentifier(String memberIdentifier) {
    this.memberIdentifier = memberIdentifier;
  }

  public String getMemberName() {
    return memberName;
  }

  public void setMemberName(String memberName) {
    this.memberName = memberName;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }

  public void setIssuedAt(Instant issuedAt) {
    this.issuedAt = issuedAt;
  }

  public Instant getDueAt() {
    return dueAt;
  }

  public void setDueAt(Instant dueAt) {
    this.dueAt = dueAt;
  }

  public Instant getReturnedAt() {
    return returnedAt;
  }

  public void setReturnedAt(Instant returnedAt) {
    this.returnedAt = returnedAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public BigDecimal getFineAmount() {
    return fineAmount;
  }

  public void setFineAmount(BigDecimal fineAmount) {
    this.fineAmount = fineAmount;
  }

  public String getExternalRef() {
    return externalRef;
  }

  public void setExternalRef(String externalRef) {
    this.externalRef = externalRef;
  }

  public String getSourceDeviceId() {
    return sourceDeviceId;
  }

  public void setSourceDeviceId(String sourceDeviceId) {
    this.sourceDeviceId = sourceDeviceId;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
