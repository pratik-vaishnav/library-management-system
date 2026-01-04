package com.assistive.library.desktop.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SettingsDao {
  public String getValue(String key) {
    String sql = "SELECT value FROM settings WHERE key = ?";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, key);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString("value");
        }
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to read setting", ex);
    }
    return null;
  }

  public void setValue(String key, String value) {
    String sql = "INSERT INTO settings (key, value) VALUES (?, ?) "
        + "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, key);
      stmt.setString(2, value);
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to write setting", ex);
    }
  }

  public Map<String, String> listAll() {
    String sql = "SELECT key, value FROM settings";
    Map<String, String> values = new HashMap<>();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        values.put(rs.getString("key"), rs.getString("value"));
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to load settings", ex);
    }
    return values;
  }
}
