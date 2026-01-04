package com.assistive.library.desktop.util;

import java.nio.file.Path;

public final class AppPaths {
  private static final String APP_DIR = ".assistive-library";

  private AppPaths() {
  }

  public static Path dataDir() {
    return Path.of(System.getProperty("user.home"), APP_DIR);
  }

  public static Path databasePath() {
    return dataDir().resolve("library.db");
  }
}
