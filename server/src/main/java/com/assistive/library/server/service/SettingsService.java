package com.assistive.library.server.service;

import com.assistive.library.server.dto.SettingItem;
import com.assistive.library.server.model.Setting;
import com.assistive.library.server.repository.SettingRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {
  public static final String LOAN_DUE_DAYS = "loan.dueDays";
  public static final String LOAN_FINE_PER_DAY = "loan.finePerDay";
  public static final String LOAN_MAX_ACTIVE = "loan.maxActive";

  private final SettingRepository settingRepository;

  public SettingsService(SettingRepository settingRepository) {
    this.settingRepository = settingRepository;
  }

  public List<SettingItem> listAll() {
    return settingRepository.findAll().stream()
        .map(setting -> new SettingItem(setting.getKey(), setting.getValue()))
        .collect(Collectors.toList());
  }

  @Transactional
  public List<SettingItem> upsert(List<SettingItem> items) {
    for (SettingItem item : items) {
      if (item.getKey() == null || item.getKey().isBlank()) {
        continue;
      }
      validateSetting(item);
      Setting setting = settingRepository.findById(item.getKey())
          .orElseGet(() -> new Setting(item.getKey(), null));
      setting.setValue(item.getValue());
      settingRepository.save(setting);
    }
    return listAll();
  }

  public int getDueDays(int defaultValue) {
    return getInt(LOAN_DUE_DAYS).orElse(defaultValue);
  }

  public BigDecimal getFinePerDay(BigDecimal defaultValue) {
    return getDecimal(LOAN_FINE_PER_DAY).orElse(defaultValue);
  }

  public int getMaxActiveLoans(int defaultValue) {
    return getInt(LOAN_MAX_ACTIVE).orElse(defaultValue);
  }

  private Optional<Integer> getInt(String key) {
    return settingRepository.findById(key)
        .map(Setting::getValue)
        .filter(value -> value != null && !value.isBlank())
        .flatMap(value -> {
          try {
            return Optional.of(Integer.parseInt(value));
          } catch (NumberFormatException ex) {
            return Optional.empty();
          }
        });
  }

  private Optional<BigDecimal> getDecimal(String key) {
    return settingRepository.findById(key)
        .map(Setting::getValue)
        .filter(value -> value != null && !value.isBlank())
        .flatMap(value -> {
          try {
            return Optional.of(new BigDecimal(value));
          } catch (NumberFormatException ex) {
            return Optional.empty();
          }
        });
  }

  private void validateSetting(SettingItem item) {
    if (LOAN_DUE_DAYS.equals(item.getKey())) {
      int value = parseInt(item.getValue());
      if (value <= 0 || value > 60) {
        throw new IllegalArgumentException("Due days must be between 1 and 60");
      }
    }
    if (LOAN_FINE_PER_DAY.equals(item.getKey())) {
      BigDecimal value = parseDecimal(item.getValue());
      if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("1000")) > 0) {
        throw new IllegalArgumentException("Fine per day must be between 0 and 1000");
      }
    }
    if (LOAN_MAX_ACTIVE.equals(item.getKey())) {
      int value = parseInt(item.getValue());
      if (value <= 0 || value > 20) {
        throw new IllegalArgumentException("Max active loans must be between 1 and 20");
      }
    }
  }

  private int parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Invalid numeric value");
    }
  }

  private BigDecimal parseDecimal(String value) {
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Invalid decimal value");
    }
  }
}
