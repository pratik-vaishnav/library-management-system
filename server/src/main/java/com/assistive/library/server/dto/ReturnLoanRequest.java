package com.assistive.library.server.dto;

import jakarta.validation.constraints.NotNull;

public class ReturnLoanRequest {
  @NotNull
  private Long loanId;

  public Long getLoanId() {
    return loanId;
  }

  public void setLoanId(Long loanId) {
    this.loanId = loanId;
  }
}
