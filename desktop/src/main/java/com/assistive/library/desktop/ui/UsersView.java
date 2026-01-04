package com.assistive.library.desktop.ui;

import com.assistive.library.desktop.AppContext;
import com.assistive.library.desktop.api.CreateUserRequest;
import com.assistive.library.desktop.api.UpdateUserRequest;
import com.assistive.library.desktop.api.UserResponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class UsersView extends BorderPane {
  private final ObservableList<UserResponse> users = FXCollections.observableArrayList();
  private final Label status = new Label();

  public UsersView(AppContext context) {
    setPadding(new Insets(16, 0, 0, 0));

    Label title = new Label("Users");
    title.getStyleClass().add("section-title");

    Button refreshButton = new Button("Refresh");
    Button addButton = new Button("Add User");
    addButton.getStyleClass().add("primary-button");
    Button updateButton = new Button("Update User");

    HBox controls = new HBox(12, refreshButton, addButton, updateButton, status);
    controls.setAlignment(Pos.CENTER_LEFT);

    TableView<UserResponse> table = new TableView<>();
    table.getColumns().add(column("ID", "id", 80));
    table.getColumns().add(column("Username", "username", 200));
    table.getColumns().add(column("Role", "role", 140));
    table.getColumns().add(column("Active", "active", 100));
    table.setItems(users);

    refreshButton.setOnAction(event -> loadUsers(context));
    addButton.setOnAction(event -> createUser(context));
    updateButton.setOnAction(event -> updateUser(context, table.getSelectionModel().getSelectedItem()));

    VBox content = new VBox(16, title, controls, table);
    setCenter(content);

    loadUsers(context);
  }

  private TableColumn<UserResponse, Object> column(String label, String property, int width) {
    TableColumn<UserResponse, Object> column = new TableColumn<>(label);
    column.setCellValueFactory(new PropertyValueFactory<>(property));
    column.setPrefWidth(width);
    return column;
  }

  private void loadUsers(AppContext context) {
    status.setText("Loading...");
    Task<UserResponse[]> task = new Task<>() {
      @Override
      protected UserResponse[] call() throws Exception {
        return context.getUsersApi().listUsers();
      }
    };

    task.setOnSucceeded(event -> {
      users.setAll(task.getValue());
      status.setText("Loaded " + users.size() + " users");
    });

    task.setOnFailed(event -> status.setText("Failed to load users"));

    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
  }

  private void createUser(AppContext context) {
    Dialog<CreateUserRequest> dialog = new Dialog<>();
    dialog.setTitle("Create User");

    TextField usernameField = new TextField();
    PasswordField passwordField = new PasswordField();
    ComboBox<String> roleBox = new ComboBox<>();
    roleBox.getItems().addAll("ADMIN", "OPERATOR", "VIEWER");
    roleBox.getSelectionModel().select("OPERATOR");
    CheckBox activeBox = new CheckBox("Active");
    activeBox.setSelected(true);

    GridPane form = new GridPane();
    form.setHgap(12);
    form.setVgap(12);
    form.add(new Label("Username"), 0, 0);
    form.add(usernameField, 1, 0);
    form.add(new Label("Password"), 0, 1);
    form.add(passwordField, 1, 1);
    form.add(new Label("Role"), 0, 2);
    form.add(roleBox, 1, 2);
    form.add(activeBox, 1, 3);

    dialog.getDialogPane().setContent(form);
    dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK,
        javafx.scene.control.ButtonType.CANCEL);

    dialog.setResultConverter(button -> {
      if (button == javafx.scene.control.ButtonType.OK) {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(usernameField.getText().trim());
        request.setPassword(passwordField.getText());
        request.setRole(roleBox.getValue());
        request.setActive(activeBox.isSelected());
        return request;
      }
      return null;
    });

    dialog.showAndWait().ifPresent(request -> {
      Task<UserResponse> task = new Task<>() {
        @Override
        protected UserResponse call() throws Exception {
          return context.getUsersApi().createUser(request);
        }
      };
      task.setOnSucceeded(event -> {
        users.add(task.getValue());
        status.setText("User created");
      });
      task.setOnFailed(event -> status.setText("Failed to create user"));
      Thread thread = new Thread(task);
      thread.setDaemon(true);
      thread.start();
    });
  }

  private void updateUser(AppContext context, UserResponse selected) {
    if (selected == null) {
      status.setText("Select a user to update");
      return;
    }

    Dialog<UpdateUserRequest> dialog = new Dialog<>();
    dialog.setTitle("Update User");

    ComboBox<String> roleBox = new ComboBox<>();
    roleBox.getItems().addAll("ADMIN", "OPERATOR", "VIEWER");
    roleBox.getSelectionModel().select(selected.getRole());
    CheckBox activeBox = new CheckBox("Active");
    activeBox.setSelected(selected.isActive());
    PasswordField passwordField = new PasswordField();
    passwordField.setPromptText("Leave blank to keep current");

    GridPane form = new GridPane();
    form.setHgap(12);
    form.setVgap(12);
    form.add(new Label("Role"), 0, 0);
    form.add(roleBox, 1, 0);
    form.add(activeBox, 1, 1);
    form.add(new Label("New Password"), 0, 2);
    form.add(passwordField, 1, 2);

    dialog.getDialogPane().setContent(form);
    dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK,
        javafx.scene.control.ButtonType.CANCEL);

    dialog.setResultConverter(button -> {
      if (button == javafx.scene.control.ButtonType.OK) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole(roleBox.getValue());
        request.setActive(activeBox.isSelected());
        if (!passwordField.getText().isBlank()) {
          request.setPassword(passwordField.getText());
        }
        return request;
      }
      return null;
    });

    dialog.showAndWait().ifPresent(request -> {
      Task<UserResponse> task = new Task<>() {
        @Override
        protected UserResponse call() throws Exception {
          return context.getUsersApi().updateUser(selected.getId(), request);
        }
      };
      task.setOnSucceeded(event -> {
        UserResponse updated = task.getValue();
        users.remove(selected);
        users.add(updated);
        status.setText("User updated");
      });
      task.setOnFailed(event -> status.setText("Failed to update user"));
      Thread thread = new Thread(task);
      thread.setDaemon(true);
      thread.start();
    });
  }
}
