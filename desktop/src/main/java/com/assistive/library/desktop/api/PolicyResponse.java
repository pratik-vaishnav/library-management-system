package com.assistive.library.desktop.api;

import java.math.BigDecimal;

public class PolicyResponse {
  private int dueDays;
  private BigDecimal finePerDay;
  private int maxActiveLoans;

  public int getDueDays() {
    return dueDays;
  }

  public void setDueDays(int dueDays) {
    this.dueDays = dueDays;
  }

  public BigDecimal getFinePerDay() {
    return finePerDay;
  }

  public void setFinePerDay(BigDecimal finePerDay) {
    this.finePerDay = finePerDay;
  }

  public int getMaxActiveLoans() {
    return maxActiveLoans;
  }

  public void setMaxActiveLoans(int maxActiveLoans) {
    this.maxActiveLoans = maxActiveLoans;
  }
}
