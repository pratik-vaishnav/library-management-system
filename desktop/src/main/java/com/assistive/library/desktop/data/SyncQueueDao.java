package com.assistive.library.desktop.data;

import com.assistive.library.desktop.model.SyncQueueItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SyncQueueDao {
  public void enqueue(String clientRef, String entityType, String entityId, String action, String payload) {
    String sql = "INSERT INTO sync_queue (client_ref, entity_type, entity_id, action, payload, created_at, status) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, clientRef);
      stmt.setString(2, entityType);
      stmt.setString(3, entityId);
      stmt.setString(4, action);
      stmt.setString(5, payload);
      stmt.setString(6, Instant.now().toString());
      stmt.setString(7, "PENDING");
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to enqueue sync item", ex);
    }
  }

  public List<SyncQueueItem> listPending() {
    return listByStatus("PENDING");
  }

  public List<SyncQueueItem> listFailed() {
    return listByStatus("FAILED");
  }

  public List<SyncQueueItem> listAll() {
    String sql = "SELECT * FROM sync_queue ORDER BY id DESC";
    return listWithQuery(sql);
  }

  public int countPending() {
    return countByStatus("PENDING");
  }

  public int countFailed() {
    return countByStatus("FAILED");
  }

  public int countConflicts() {
    return countByStatus("CONFLICT");
  }

  public void markSyncedByClientRefs(List<String> clientRefs) {
    updateStatusByClientRefs(clientRefs, "SYNCED", null);
  }

  public void markFailedByClientRef(String clientRef, String errorMessage) {
    updateStatusByClientRefs(List.of(clientRef), "FAILED", errorMessage);
  }

  public void markStatusByClientRef(String clientRef, String status, String errorMessage) {
    updateStatusByClientRefs(List.of(clientRef), status, errorMessage);
  }

  public void updateClientRef(long id, String clientRef) {
    String sql = "UPDATE sync_queue SET client_ref = ? WHERE id = ?";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, clientRef);
      stmt.setLong(2, id);
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to update client ref", ex);
    }
  }

  public void updatePayloadAndStatus(String clientRef, String payload, String status) {
    String sql = "UPDATE sync_queue SET payload = ?, status = ?, error_message = NULL, last_attempt_at = ? "
        + "WHERE client_ref = ?";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, payload);
      stmt.setString(2, status);
      stmt.setString(3, Instant.now().toString());
      stmt.setString(4, clientRef);
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to update sync payload", ex);
    }
  }

  public void retryFailed() {
    String sql = "UPDATE sync_queue SET status = 'PENDING', error_message = NULL WHERE status = 'FAILED'";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to retry failed items", ex);
    }
  }

  private int countByStatus(String status) {
    String sql = "SELECT COUNT(*) FROM sync_queue WHERE status = ?";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, status);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to count sync queue", ex);
    }
    return 0;
  }

  private void updateStatusByClientRefs(List<String> clientRefs, String status, String errorMessage) {
    if (clientRefs.isEmpty()) {
      return;
    }
    String placeholders = String.join(",", clientRefs.stream().map(ref -> "?").toList());
    String sql = "UPDATE sync_queue SET status = ?, error_message = ?, last_attempt_at = ? WHERE client_ref IN ("
        + placeholders + ")";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      int index = 1;
      stmt.setString(index++, status);
      stmt.setString(index++, errorMessage);
      stmt.setString(index++, Instant.now().toString());
      for (String ref : clientRefs) {
        stmt.setString(index++, ref);
      }
      stmt.executeUpdate();
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to update sync queue", ex);
    }
  }

  private List<SyncQueueItem> listByStatus(String status) {
    String sql = "SELECT * FROM sync_queue WHERE status = ? ORDER BY id DESC";
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, status);
      return listWithStatement(stmt);
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to load sync queue", ex);
    }
  }

  private List<SyncQueueItem> listWithQuery(String sql) {
    try (Connection conn = LocalDatabase.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
      return listWithStatement(stmt);
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to load sync queue", ex);
    }
  }

  private List<SyncQueueItem> listWithStatement(PreparedStatement stmt) throws SQLException {
    List<SyncQueueItem> items = new ArrayList<>();
    try (ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        SyncQueueItem item = new SyncQueueItem();
        item.setId(rs.getLong("id"));
        item.setClientRef(rs.getString("client_ref"));
        item.setEntityType(rs.getString("entity_type"));
        item.setEntityId(rs.getString("entity_id"));
        item.setAction(rs.getString("action"));
        item.setPayload(rs.getString("payload"));
        item.setStatus(rs.getString("status"));
        item.setErrorMessage(rs.getString("error_message"));
        String created = rs.getString("created_at");
        if (created != null) {
          item.setCreatedAt(Instant.parse(created));
        }
        String attempted = rs.getString("last_attempt_at");
        if (attempted != null) {
          item.setLastAttemptAt(Instant.parse(attempted));
        }
        items.add(item);
      }
    }
    return items;
  }
}
