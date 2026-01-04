package com.assistive.library.server.controller;

import com.assistive.library.server.dto.SettingItem;
import com.assistive.library.server.service.SettingsService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
  private final SettingsService settingsService;

  public SettingsController(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @GetMapping
  public List<SettingItem> listSettings() {
    return settingsService.listAll();
  }

  @PutMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<SettingItem> updateSettings(@Valid @RequestBody List<SettingItem> items) {
    return settingsService.upsert(items);
  }
}
