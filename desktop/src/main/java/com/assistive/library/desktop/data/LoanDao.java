package com.assistive.library.desktop.data;

import com.assistive.library.desktop.model.LoanRecord;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class LoanDao {
  public List<LoanRecord> listAll() {
    String sql = "SELECT l.*, b.isbn AS book_isbn, b.title AS book_title, "
        + "m.member_id AS member_identifier, m.name AS member_name "
        + "FROM loans l JOIN books b ON l.book_id = b.id "
        + "JOIN members m ON l.member_id = m.id ORDER BY l.issued_at DESC";
    List<LoanRecord> records = new ArrayList<>();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        records.add(mapRow(rs));
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to load loans", ex);
    }
    return records;
  }

  public List<LoanRecord> searchIssued(String query, int limit) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    String sql = "SELECT l.*, b.isbn AS book_isbn, b.title AS book_title, "
        + "m.member_id AS member_identifier, m.name AS member_name "
        + "FROM loans l JOIN books b ON l.book_id = b.id "
        + "JOIN members m ON l.member_id = m.id "
        + "WHERE l.status = 'ISSUED' AND ("
        + "CAST(l.id AS TEXT) LIKE ? OR m.member_id LIKE ? OR m.name LIKE ? "
        + "OR b.isbn LIKE ? OR b.title LIKE ?) "
        + "ORDER BY l.issued_at DESC LIMIT ?";
    List<LoanRecord> records = new ArrayList<>();
    String filter = "%" + query.trim() + "%";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, filter);
      stmt.setString(2, filter);
      stmt.setString(3, filter);
      stmt.setString(4, filter);
      stmt.setString(5, filter);
      stmt.setInt(6, limit);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          records.add(mapRow(rs));
        }
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to search loans", ex);
    }
    return records;
  }

  public LoanRecord issueLoan(long bookId, long memberId, String externalRef, String deviceId, int dueDays) {
    String sql = "INSERT INTO loans (book_id, member_id, issued_at, due_at, status, fine_amount, "
        + "external_ref, source_device_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    Instant issuedAt = Instant.now();
    Instant dueAt = issuedAt.plus(dueDays, ChronoUnit.DAYS);
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
      stmt.setLong(1, bookId);
      stmt.setLong(2, memberId);
      stmt.setString(3, issuedAt.toString());
      stmt.setString(4, dueAt.toString());
      stmt.setString(5, "ISSUED");
      stmt.setBigDecimal(6, BigDecimal.ZERO);
      stmt.setString(7, externalRef);
      stmt.setString(8, deviceId);
      stmt.setString(9, issuedAt.toString());
      stmt.setString(10, issuedAt.toString());
      stmt.executeUpdate();
      long id = -1;
      try (ResultSet keys = stmt.getGeneratedKeys()) {
        if (keys.next()) {
          id = keys.getLong(1);
        }
      }
      LoanRecord record = new LoanRecord();
      record.setId(id);
      record.setBookId(bookId);
      record.setMemberId(memberId);
      record.setIssuedAt(issuedAt);
      record.setDueAt(dueAt);
      record.setStatus("ISSUED");
      record.setFineAmount(BigDecimal.ZERO);
      record.setExternalRef(externalRef);
      record.setSourceDeviceId(deviceId);
      record.setUpdatedAt(issuedAt);
      return record;
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to issue loan", ex);
    }
  }

  public void upsertFromSync(long bookId,
                             long memberId,
                             String externalRef,
                             String deviceId,
                             Instant issuedAt,
                             Instant dueAt,
                             Instant returnedAt,
                             String status,
                             BigDecimal fineAmount) {
    LoanRecord existing = findByExternalRef(externalRef);
    if (existing == null) {
      insertFromSync(bookId, memberId, externalRef, deviceId, issuedAt, dueAt, returnedAt, status, fineAmount);
      return;
    }

    String sql = "UPDATE loans SET issued_at = ?, due_at = ?, returned_at = ?, status = ?, fine_amount = ?, "
        + "updated_at = ? WHERE external_ref = ?";
    Instant now = Instant.now();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, issuedAt == null ? now.toString() : issuedAt.toString());
      stmt.setString(2, dueAt == null ? now.toString() : dueAt.toString());
      stmt.setString(3, returnedAt == null ? null : returnedAt.toString());
      stmt.setString(4, status);
      stmt.setBigDecimal(5, fineAmount == null ? BigDecimal.ZERO : fineAmount);
      stmt.setString(6, now.toString());
      stmt.setString(7, externalRef);
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to update loan", ex);
    }
  }

  public LoanRecord findByExternalRef(String externalRef) {
    String sql = "SELECT l.*, b.isbn AS book_isbn, b.title AS book_title, "
        + "m.member_id AS member_identifier, m.name AS member_name "
        + "FROM loans l JOIN books b ON l.book_id = b.id "
        + "JOIN members m ON l.member_id = m.id WHERE l.external_ref = ?";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, externalRef);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return mapRow(rs);
        }
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to load loan", ex);
    }
    return null;
  }

  public LoanRecord returnLoan(long loanId, BigDecimal fineAmount) {
    String sql = "UPDATE loans SET returned_at = ?, status = ?, fine_amount = ?, updated_at = ? WHERE id = ?";
    Instant now = Instant.now();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, now.toString());
      stmt.setString(2, "RETURNED");
      stmt.setBigDecimal(3, fineAmount);
      stmt.setString(4, now.toString());
      stmt.setLong(5, loanId);
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to return loan", ex);
    }
    return findById(loanId);
  }

  public LoanRecord findById(long loanId) {
    String sql = "SELECT l.*, b.isbn AS book_isbn, b.title AS book_title, "
        + "m.member_id AS member_identifier, m.name AS member_name "
        + "FROM loans l JOIN books b ON l.book_id = b.id "
        + "JOIN members m ON l.member_id = m.id WHERE l.id = ?";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, loanId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return mapRow(rs);
        }
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to load loan", ex);
    }
    return null;
  }

  public int countIssuedToday() {
    String sql = "SELECT COUNT(*) FROM loans WHERE date(issued_at) = date('now')";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to count issued", ex);
    }
    return 0;
  }

  public int countReturnedToday() {
    String sql = "SELECT COUNT(*) FROM loans WHERE date(returned_at) = date('now')";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to count returns", ex);
    }
    return 0;
  }

  public int countOverdue() {
    String sql = "SELECT COUNT(*) FROM loans WHERE status = 'ISSUED' AND due_at < datetime('now')";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to count overdue", ex);
    }
    return 0;
  }

  public int countActiveLoansForMember(long memberId) {
    String sql = "SELECT COUNT(*) FROM loans WHERE member_id = ? AND status = 'ISSUED'";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, memberId);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to count member loans", ex);
    }
    return 0;
  }

  public List<MonthlyCount> countIssuedByMonth(int months) {
    String sql = "SELECT strftime('%Y-%m', issued_at) AS month, COUNT(*) AS total "
        + "FROM loans GROUP BY month ORDER BY month DESC LIMIT ?";
    List<MonthlyCount> results = new ArrayList<>();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, months);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          results.add(new MonthlyCount(rs.getString("month"), rs.getInt("total")));
        }
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to load monthly counts", ex);
    }
    return results;
  }

  public List<BookCount> topBooks(int limit) {
    String sql = "SELECT b.title AS title, COUNT(*) AS total "
        + "FROM loans l JOIN books b ON l.book_id = b.id "
        + "GROUP BY b.title ORDER BY total DESC LIMIT ?";
    List<BookCount> results = new ArrayList<>();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, limit);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          results.add(new BookCount(rs.getString("title"), rs.getInt("total")));
        }
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to load top books", ex);
    }
    return results;
  }

  public record MonthlyCount(String month, int total) {
  }

  public record BookCount(String title, int total) {
  }

  private void insertFromSync(long bookId,
                              long memberId,
                              String externalRef,
                              String deviceId,
                              Instant issuedAt,
                              Instant dueAt,
                              Instant returnedAt,
                              String status,
                              BigDecimal fineAmount) {
    String sql = "INSERT INTO loans (book_id, member_id, issued_at, due_at, returned_at, status, fine_amount, "
        + "external_ref, source_device_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    Instant now = Instant.now();
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, bookId);
      stmt.setLong(2, memberId);
      stmt.setString(3, issuedAt == null ? now.toString() : issuedAt.toString());
      stmt.setString(4, dueAt == null ? now.toString() : dueAt.toString());
      stmt.setString(5, returnedAt == null ? null : returnedAt.toString());
      stmt.setString(6, status);
      stmt.setBigDecimal(7, fineAmount == null ? BigDecimal.ZERO : fineAmount);
      stmt.setString(8, externalRef);
      stmt.setString(9, deviceId);
      stmt.setString(10, now.toString());
      stmt.setString(11, now.toString());
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to insert loan", ex);
    }
  }

  private LoanRecord mapRow(ResultSet rs) throws SQLException {
    LoanRecord record = new LoanRecord();
    record.setId(rs.getLong("id"));
    record.setBookId(rs.getLong("book_id"));
    record.setMemberId(rs.getLong("member_id"));
    record.setBookIsbn(rs.getString("book_isbn"));
    record.setBookTitle(rs.getString("book_title"));
    record.setMemberIdentifier(rs.getString("member_identifier"));
    record.setMemberName(rs.getString("member_name"));
    record.setIssuedAt(Instant.parse(rs.getString("issued_at")));
    record.setDueAt(Instant.parse(rs.getString("due_at")));
    String returned = rs.getString("returned_at");
    if (returned != null) {
      record.setReturnedAt(Instant.parse(returned));
    }
    record.setStatus(rs.getString("status"));
    record.setFineAmount(rs.getBigDecimal("fine_amount"));
    record.setExternalRef(rs.getString("external_ref"));
    record.setSourceDeviceId(rs.getString("source_device_id"));
    String updated = rs.getString("updated_at");
    if (updated != null) {
      record.setUpdatedAt(Instant.parse(updated));
    }
    return record;
  }
}
