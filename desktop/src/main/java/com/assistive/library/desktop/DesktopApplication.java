package com.assistive.library.desktop;

import com.assistive.library.desktop.data.LocalDatabase;
import com.assistive.library.desktop.api.ApiClient;
import com.assistive.library.desktop.api.ServerConfig;
import com.assistive.library.desktop.ui.SceneRouter;
import javafx.application.Application;
import javafx.stage.Stage;

public class DesktopApplication extends Application {
  @Override
  public void start(Stage stage) {
    LocalDatabase.initialize();
    ApiClient apiClient = new ApiClient(ServerConfig.baseUrl());
    AppContext context = new AppContext(apiClient);
    context.getSyncManager().ensureDeviceId();
    SceneRouter router = new SceneRouter(stage, context);
    router.showLogin();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
