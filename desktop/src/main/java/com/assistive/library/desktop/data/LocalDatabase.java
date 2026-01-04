package com.assistive.library.desktop.data;

import com.assistive.library.desktop.util.AppPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class LocalDatabase {
  private static final String JDBC_PREFIX = "jdbc:sqlite:";

  private LocalDatabase() {
  }

  public static void initialize() {
    try {
      Files.createDirectories(AppPaths.dataDir());
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to create data directory", ex);
    }

    try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA foreign_keys = ON");
      statement.execute("CREATE TABLE IF NOT EXISTS books ("
          + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
          + "isbn TEXT UNIQUE NOT NULL, "
          + "title TEXT NOT NULL, "
          + "author TEXT, "
          + "category TEXT, "
          + "rack_location TEXT, "
          + "total_quantity INTEGER NOT NULL, "
          + "available_quantity INTEGER NOT NULL, "
          + "enabled INTEGER NOT NULL DEFAULT 1, "
          + "created_at TEXT NOT NULL, "
          + "updated_at TEXT NOT NULL"
          + ")");
      statement.execute("CREATE TABLE IF NOT EXISTS members ("
          + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
          + "member_id TEXT UNIQUE NOT NULL, "
          + "name TEXT NOT NULL, "
          + "type TEXT NOT NULL, "
          + "class_or_department TEXT, "
          + "contact_details TEXT, "
          + "active INTEGER NOT NULL DEFAULT 1, "
          + "created_at TEXT NOT NULL, "
          + "updated_at TEXT NOT NULL"
          + ")");
      statement.execute("CREATE TABLE IF NOT EXISTS loans ("
          + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
          + "book_id INTEGER NOT NULL, "
          + "member_id INTEGER NOT NULL, "
          + "issued_at TEXT NOT NULL, "
          + "due_at TEXT NOT NULL, "
          + "returned_at TEXT, "
          + "status TEXT NOT NULL, "
          + "fine_amount REAL NOT NULL DEFAULT 0, "
          + "external_ref TEXT, "
          + "source_device_id TEXT, "
          + "created_at TEXT NOT NULL, "
          + "updated_at TEXT NOT NULL, "
          + "FOREIGN KEY(book_id) REFERENCES books(id), "
          + "FOREIGN KEY(member_id) REFERENCES members(id)"
          + ")");
      statement.execute("CREATE TABLE IF NOT EXISTS settings ("
          + "key TEXT PRIMARY KEY, "
          + "value TEXT NOT NULL"
          + ")");
      statement.execute("CREATE TABLE IF NOT EXISTS sync_queue ("
          + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
          + "client_ref TEXT, "
          + "entity_type TEXT NOT NULL, "
          + "entity_id TEXT NOT NULL, "
          + "action TEXT NOT NULL, "
          + "payload TEXT NOT NULL, "
          + "created_at TEXT NOT NULL, "
          + "status TEXT NOT NULL, "
          + "last_attempt_at TEXT, "
          + "error_message TEXT"
          + ")");

      ensureColumnExists(connection, "loans", "external_ref", "TEXT");
      ensureColumnExists(connection, "loans", "source_device_id", "TEXT");
      ensureColumnExists(connection, "sync_queue", "client_ref", "TEXT");
      ensureColumnExists(connection, "sync_queue", "last_attempt_at", "TEXT");
      ensureColumnExists(connection, "sync_queue", "error_message", "TEXT");
    } catch (SQLException ex) {
      throw new IllegalStateException("Failed to initialize local database", ex);
    }
  }

  public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(JDBC_PREFIX + AppPaths.databasePath());
  }

  private static void ensureColumnExists(Connection connection, String table, String column, String type)
      throws SQLException {
    try (Statement statement = connection.createStatement();
         var rs = statement.executeQuery("PRAGMA table_info('" + table + "')")) {
      while (rs.next()) {
        if (column.equalsIgnoreCase(rs.getString("name"))) {
          return;
        }
      }
    }
    try (Statement statement = connection.createStatement()) {
      statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
    }
  }
}
