package com.assistive.library.server.dto;

import com.assistive.library.server.model.LoanStatus;
import java.math.BigDecimal;
import java.time.Instant;

public class LoanSyncItem {
  private String clientRef;
  private boolean force;
  private String externalRef;
  private String sourceDeviceId;
  private String bookIsbn;
  private String memberId;
  private Instant issuedAt;
  private Instant dueAt;
  private Instant returnedAt;
  private LoanStatus status;
  private BigDecimal fineAmount;
  private Instant updatedAt;

  public String getClientRef() {
    return clientRef;
  }

  public void setClientRef(String clientRef) {
    this.clientRef = clientRef;
  }

  public boolean isForce() {
    return force;
  }

  public void setForce(boolean force) {
    this.force = force;
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

  public String getBookIsbn() {
    return bookIsbn;
  }

  public void setBookIsbn(String bookIsbn) {
    this.bookIsbn = bookIsbn;
  }

  public String getMemberId() {
    return memberId;
  }

  public void setMemberId(String memberId) {
    this.memberId = memberId;
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

  public LoanStatus getStatus() {
    return status;
  }

  public void setStatus(LoanStatus status) {
    this.status = status;
  }

  public BigDecimal getFineAmount() {
    return fineAmount;
  }

  public void setFineAmount(BigDecimal fineAmount) {
    this.fineAmount = fineAmount;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
