package com.assistive.library.desktop.ui;

import com.assistive.library.desktop.AppContext;
import com.assistive.library.desktop.api.ApiException;
import com.assistive.library.desktop.api.AuthResponse;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LoginView extends BorderPane {
  public LoginView(SceneRouter router, AppContext context) {
    setPadding(new Insets(32));

    Label title = new Label("EduShelf");
    title.getStyleClass().add("title");

    Label subtitle = new Label("Offline-first library management for schools");
    subtitle.getStyleClass().add("subtitle");

    VBox header = new VBox(6, title, subtitle);
    header.setAlignment(Pos.CENTER_LEFT);

    GridPane form = new GridPane();
    form.setHgap(12);
    form.setVgap(12);

    TextField usernameField = new TextField();
    usernameField.setPromptText("Enter username");

    PasswordField passwordField = new PasswordField();
    passwordField.setPromptText("Enter password");

    ComboBox<String> roleBox = new ComboBox<>();
    roleBox.getItems().addAll("Admin", "Operator", "Viewer");
    roleBox.getSelectionModel().select("Operator");
    roleBox.setDisable(true);

    form.add(new Label("Username"), 0, 0);
    form.add(usernameField, 1, 0);
    form.add(new Label("Password"), 0, 1);
    form.add(passwordField, 1, 1);
    form.add(new Label("Role"), 0, 2);
    form.add(roleBox, 1, 2);

    Label statusLabel = new Label("Internet: Unknown | Last sync: --");
    statusLabel.getStyleClass().add("muted");

    Label errorLabel = new Label();
    errorLabel.getStyleClass().add("error");

    ProgressIndicator progress = new ProgressIndicator();
    progress.setMaxSize(24, 24);
    progress.setVisible(false);

    Button loginButton = new Button("Sign In");
    loginButton.getStyleClass().add("primary-button");
    loginButton.setOnAction(event -> {
      String username = usernameField.getText().trim();
      String password = passwordField.getText();
      if (username.isEmpty() || password.isEmpty()) {
        errorLabel.setText("Enter username and password to continue.");
        return;
      }

      errorLabel.setText("");
      loginButton.setDisable(true);
      progress.setVisible(true);

      Task<AuthResponse> task = new Task<>() {
        @Override
        protected AuthResponse call() throws Exception {
          return context.getAuthApi().login(username, password);
        }
      };

      task.setOnSucceeded(done -> {
        AuthResponse response = task.getValue();
        context.getSessionState().setUsername(response.getUsername());
        context.getSessionState().setRole(response.getRole());
        context.getSessionState().setToken(response.getToken());
        context.getSessionState().setExpiresAt(response.getExpiresAt());
        context.getApiClient().setAuthToken(response.getToken());
        loginButton.setDisable(false);
        progress.setVisible(false);
        router.showMain();
      });

      task.setOnFailed(done -> {
        Throwable error = task.getException();
        if (error instanceof ApiException apiException) {
          errorLabel.setText("Login failed. " + apiException.getResponseBody());
        } else {
          errorLabel.setText("Login failed. Please check the server connection.");
        }
        loginButton.setDisable(false);
        progress.setVisible(false);
      });

      Thread thread = new Thread(task);
      thread.setDaemon(true);
      thread.start();
    });

    HBox actions = new HBox(12, loginButton, progress, statusLabel);
    actions.setAlignment(Pos.CENTER_LEFT);

    VBox content = new VBox(20, form, errorLabel, actions);
    content.getStyleClass().add("panel");
    BorderPane.setMargin(content, new Insets(24, 0, 0, 0));

    setTop(header);
    setCenter(content);
  }
}
