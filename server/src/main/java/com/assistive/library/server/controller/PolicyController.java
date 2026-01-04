package com.assistive.library.server.controller;

import com.assistive.library.server.dto.PolicyResponse;
import com.assistive.library.server.service.SettingsService;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policy")
public class PolicyController {
  private final SettingsService settingsService;

  public PolicyController(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @GetMapping
  public PolicyResponse getPolicy() {
    int dueDays = settingsService.getDueDays(14);
    BigDecimal finePerDay = settingsService.getFinePerDay(new BigDecimal("1.0"));
    int maxActive = settingsService.getMaxActiveLoans(5);
    return new PolicyResponse(dueDays, finePerDay, maxActive);
  }
}
