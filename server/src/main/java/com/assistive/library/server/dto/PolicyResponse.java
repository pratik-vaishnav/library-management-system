package com.assistive.library.server.dto;

import java.math.BigDecimal;

public class PolicyResponse {
  private int dueDays;
  private BigDecimal finePerDay;
  private int maxActiveLoans;

  public PolicyResponse(int dueDays, BigDecimal finePerDay, int maxActiveLoans) {
    this.dueDays = dueDays;
    this.finePerDay = finePerDay;
    this.maxActiveLoans = maxActiveLoans;
  }

  public int getDueDays() {
    return dueDays;
  }

  public BigDecimal getFinePerDay() {
    return finePerDay;
  }

  public int getMaxActiveLoans() {
    return maxActiveLoans;
  }
}
