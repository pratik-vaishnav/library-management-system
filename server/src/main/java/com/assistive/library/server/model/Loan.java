package com.assistive.library.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "loans")
public class Loan {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "book_id")
  private Book book;

  @ManyToOne(optional = false)
  @JoinColumn(name = "member_id")
  private Member member;

  @Column(nullable = false)
  private Instant issuedAt;

  @Column(nullable = false)
  private Instant dueAt;

  private Instant returnedAt;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private LoanStatus status = LoanStatus.ISSUED;

  @Column(nullable = false)
  private BigDecimal fineAmount = BigDecimal.ZERO;

  @Column(unique = true)
  private String externalRef;

  private String sourceDeviceId;

  private Instant createdAt;
  private Instant updatedAt;

  public Loan() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Book getBook() {
    return book;
  }

  public void setBook(Book book) {
    this.book = book;
  }

  public Member getMember() {
    return member;
  }

  public void setMember(Member member) {
    this.member = member;
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
