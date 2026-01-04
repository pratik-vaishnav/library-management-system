package com.assistive.library.desktop.data;

import com.assistive.library.desktop.model.MemberRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MemberDao {
  public List<MemberRecord> listAll() {
    String sql = "SELECT * FROM members ORDER BY name";
    List<MemberRecord> records = new ArrayList<>();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        records.add(mapRow(rs));
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to load members", ex);
    }
    return records;
  }

  public List<MemberRecord> search(String query, int limit) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    String sql = "SELECT * FROM members WHERE member_id LIKE ? OR name LIKE ? OR class_or_department LIKE ? "
        + "ORDER BY name LIMIT ?";
    List<MemberRecord> records = new ArrayList<>();
    String filter = "%" + query.trim() + "%";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, filter);
      stmt.setString(2, filter);
      stmt.setString(3, filter);
      stmt.setInt(4, limit);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          records.add(mapRow(rs));
        }
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to search members", ex);
    }
    return records;
  }

  public int countAll() {
    String sql = "SELECT COUNT(*) FROM members";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to count members", ex);
    }
    return 0;
  }

  public MemberRecord findByMemberId(String memberId) {
    String sql = "SELECT * FROM members WHERE member_id = ?";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, memberId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return mapRow(rs);
        }
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to find member", ex);
    }
    return null;
  }

  public MemberRecord insert(MemberRecord record) {
    String sql = "INSERT INTO members (member_id, name, type, class_or_department, contact_details, active, "
        + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    Instant now = Instant.now();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, record.getMemberId());
      stmt.setString(2, record.getName());
      stmt.setString(3, record.getType());
      stmt.setString(4, record.getClassOrDepartment());
      stmt.setString(5, record.getContactDetails());
      stmt.setInt(6, record.isActive() ? 1 : 0);
      stmt.setString(7, now.toString());
      stmt.setString(8, now.toString());
      stmt.executeUpdate();
      try (ResultSet keys = stmt.getGeneratedKeys()) {
        if (keys.next()) {
          record.setId(keys.getLong(1));
        }
      }
      record.setUpdatedAt(now);
      return record;
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to insert member", ex);
    }
  }

  public void updateActive(long id, boolean active) {
    String sql = "UPDATE members SET active = ?, updated_at = ? WHERE id = ?";
    Instant now = Instant.now();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, active ? 1 : 0);
      stmt.setString(2, now.toString());
      stmt.setLong(3, id);
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to update member", ex);
    }
  }

  public void upsert(MemberRecord record) {
    MemberRecord existing = findByMemberId(record.getMemberId());
    if (existing == null) {
      insert(record);
      return;
    }
    String sql = "UPDATE members SET name = ?, type = ?, class_or_department = ?, contact_details = ?, "
        + "active = ?, updated_at = ? WHERE member_id = ?";
    Instant now = Instant.now();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, record.getName());
      stmt.setString(2, record.getType());
      stmt.setString(3, record.getClassOrDepartment());
      stmt.setString(4, record.getContactDetails());
      stmt.setInt(5, record.isActive() ? 1 : 0);
      stmt.setString(6, now.toString());
      stmt.setString(7, record.getMemberId());
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to upsert member", ex);
    }
  }

  private MemberRecord mapRow(ResultSet rs) throws SQLException {
    MemberRecord record = new MemberRecord();
    record.setId(rs.getLong("id"));
    record.setMemberId(rs.getString("member_id"));
    record.setName(rs.getString("name"));
    record.setType(rs.getString("type"));
    record.setClassOrDepartment(rs.getString("class_or_department"));
    record.setContactDetails(rs.getString("contact_details"));
    record.setActive(rs.getInt("active") == 1);
    String updated = rs.getString("updated_at");
    if (updated != null) {
      record.setUpdatedAt(Instant.parse(updated));
    }
    return record;
  }
}
