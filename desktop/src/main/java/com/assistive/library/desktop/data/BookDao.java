package com.assistive.library.desktop.data;

import com.assistive.library.desktop.model.BookRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BookDao {
  public List<BookRecord> listAll() {
    String sql = "SELECT * FROM books ORDER BY title";
    List<BookRecord> records = new ArrayList<>();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        records.add(mapRow(rs));
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to load books", ex);
    }
    return records;
  }

  public List<BookRecord> search(String query, int limit) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    String sql = "SELECT * FROM books WHERE isbn LIKE ? OR title LIKE ? OR author LIKE ? "
        + "ORDER BY title LIMIT ?";
    List<BookRecord> records = new ArrayList<>();
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
      throw new IllegalStateException("Failed to search books", ex);
    }
    return records;
  }

  public int countAll() {
    String sql = "SELECT COUNT(*) FROM books";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to count books", ex);
    }
    return 0;
  }

  public BookRecord findByIsbn(String isbn) {
    String sql = "SELECT * FROM books WHERE isbn = ?";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, isbn);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return mapRow(rs);
        }
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to find book", ex);
    }
    return null;
  }

  public BookRecord insert(BookRecord record) {
    String sql = "INSERT INTO books (isbn, title, author, category, rack_location, total_quantity, "
        + "available_quantity, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    Instant now = Instant.now();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
      stmt.setString(1, record.getIsbn());
      stmt.setString(2, record.getTitle());
      stmt.setString(3, record.getAuthor());
      stmt.setString(4, record.getCategory());
      stmt.setString(5, record.getRackLocation());
      stmt.setInt(6, record.getTotalQuantity());
      stmt.setInt(7, record.getAvailableQuantity());
      stmt.setInt(8, record.isEnabled() ? 1 : 0);
      stmt.setString(9, now.toString());
      stmt.setString(10, now.toString());
      stmt.executeUpdate();
      try (ResultSet keys = stmt.getGeneratedKeys()) {
        if (keys.next()) {
          record.setId(keys.getLong(1));
        }
      }
      record.setUpdatedAt(now);
      return record;
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to insert book", ex);
    }
  }

  public void updateAvailability(long id, int available) {
    String sql = "UPDATE books SET available_quantity = ?, updated_at = ? WHERE id = ?";
    Instant now = Instant.now();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, available);
      stmt.setString(2, now.toString());
      stmt.setLong(3, id);
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to update availability", ex);
    }
  }

  public void updateEnabled(long id, boolean enabled) {
    String sql = "UPDATE books SET enabled = ?, updated_at = ? WHERE id = ?";
    Instant now = Instant.now();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, enabled ? 1 : 0);
      stmt.setString(2, now.toString());
      stmt.setLong(3, id);
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to update book", ex);
    }
  }

  public void upsert(BookRecord record) {
    BookRecord existing = findByIsbn(record.getIsbn());
    if (existing == null) {
      insert(record);
      return;
    }
    String sql = "UPDATE books SET title = ?, author = ?, category = ?, rack_location = ?, total_quantity = ?, "
        + "available_quantity = ?, enabled = ?, updated_at = ? WHERE isbn = ?";
    Instant now = Instant.now();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, record.getTitle());
      stmt.setString(2, record.getAuthor());
      stmt.setString(3, record.getCategory());
      stmt.setString(4, record.getRackLocation());
      stmt.setInt(5, record.getTotalQuantity());
      stmt.setInt(6, record.getAvailableQuantity());
      stmt.setInt(7, record.isEnabled() ? 1 : 0);
      stmt.setString(8, now.toString());
      stmt.setString(9, record.getIsbn());
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to upsert book", ex);
    }
  }

  private BookRecord mapRow(ResultSet rs) throws SQLException {
    BookRecord record = new BookRecord();
    record.setId(rs.getLong("id"));
    record.setIsbn(rs.getString("isbn"));
    record.setTitle(rs.getString("title"));
    record.setAuthor(rs.getString("author"));
    record.setCategory(rs.getString("category"));
    record.setRackLocation(rs.getString("rack_location"));
    record.setTotalQuantity(rs.getInt("total_quantity"));
    record.setAvailableQuantity(rs.getInt("available_quantity"));
    record.setEnabled(rs.getInt("enabled") == 1);
    String updated = rs.getString("updated_at");
    if (updated != null) {
      record.setUpdatedAt(Instant.parse(updated));
    }
    return record;
  }
}
