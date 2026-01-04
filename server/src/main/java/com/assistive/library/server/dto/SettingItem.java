package com.assistive.library.server.dto;

import jakarta.validation.constraints.NotBlank;

public class SettingItem {
  @NotBlank
  private String key;

  private String value;

  public SettingItem() {
  }

  public SettingItem(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
