package com.assistive.library.desktop.ui;

import com.assistive.library.desktop.AppContext;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class SceneRouter {
  private final Stage stage;
  private final AppContext context;

  public SceneRouter(Stage stage, AppContext context) {
    this.stage = stage;
    this.context = context;
    this.stage.setMinWidth(900);
    this.stage.setMinHeight(600);
    this.stage.setTitle("EduShelf Library Manager");
    setIcon();
  }

  public void showLogin() {
    Scene scene = new Scene(new LoginView(this, context), 960, 640);
    applyStyles(scene);
    stage.setScene(scene);
    stage.show();
    stage.setMaximized(true);
  }

  public void showMain() {
    Scene scene = new Scene(new MainView(this, context), 1200, 760);
    applyStyles(scene);
    stage.setScene(scene);
    stage.show();
    fitToScreen();
  }

  private void applyStyles(Scene scene) {
    scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
  }

  private void setIcon() {
    try {
      var stream = getClass().getResourceAsStream("/icon.png");
      if (stream != null) {
        stage.getIcons().add(new Image(stream));
      }
    } catch (Exception ignored) {
    }
  }

  private void fitToScreen() {
    Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
    stage.setX(bounds.getMinX());
    stage.setY(bounds.getMinY());
    stage.setWidth(bounds.getWidth());
    stage.setHeight(bounds.getHeight());
    stage.setMaximized(true);
  }
}
