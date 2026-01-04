package com.assistive.library.server.config;

import com.assistive.library.server.model.Setting;
import com.assistive.library.server.repository.SettingRepository;
import com.assistive.library.server.service.SettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DefaultSettingsInitializer implements CommandLineRunner {
  private final SettingRepository settingRepository;
  private final String defaultDueDays;
  private final String defaultFinePerDay;
  private final String defaultMaxActive;

  public DefaultSettingsInitializer(SettingRepository settingRepository,
                                   @Value("${library.loans.due-days:14}") String defaultDueDays,
                                   @Value("${library.loans.fine-per-day:1.0}") String defaultFinePerDay,
                                   @Value("${library.loans.max-active:5}") String defaultMaxActive) {
    this.settingRepository = settingRepository;
    this.defaultDueDays = defaultDueDays;
    this.defaultFinePerDay = defaultFinePerDay;
    this.defaultMaxActive = defaultMaxActive;
  }

  @Override
  public void run(String... args) {
    settingRepository.findById(SettingsService.LOAN_DUE_DAYS)
        .orElseGet(() -> settingRepository.save(new Setting(SettingsService.LOAN_DUE_DAYS, defaultDueDays)));
    settingRepository.findById(SettingsService.LOAN_FINE_PER_DAY)
        .orElseGet(() -> settingRepository.save(new Setting(SettingsService.LOAN_FINE_PER_DAY, defaultFinePerDay)));
    settingRepository.findById(SettingsService.LOAN_MAX_ACTIVE)
        .orElseGet(() -> settingRepository.save(new Setting(SettingsService.LOAN_MAX_ACTIVE, defaultMaxActive)));
  }
}
