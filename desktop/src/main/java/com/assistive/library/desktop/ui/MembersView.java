package com.assistive.library.desktop.ui;

import com.assistive.library.desktop.AppContext;
import com.assistive.library.desktop.model.MemberRecord;
import java.time.Instant;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MembersView extends BorderPane {
  private final ObservableList<MemberRecord> members;

  public MembersView(AppContext context) {
    setPadding(new Insets(16, 0, 0, 0));

    Label title = new Label("Members");
    title.getStyleClass().add("section-title");

    TextField searchField = new TextField();
    searchField.setPromptText("Search by name, member ID");

    Button addButton = new Button("Add Member");
    addButton.getStyleClass().add("primary-button");

    HBox controls = new HBox(12, searchField, addButton);
    controls.setAlignment(Pos.CENTER_LEFT);

    TableView<MemberRecord> table = new TableView<>();
    table.getColumns().add(column("Member ID", "memberId", 140));
    table.getColumns().add(column("Name", "name", 200));
    table.getColumns().add(column("Type", "type", 120));
    table.getColumns().add(column("Class/Dept", "classOrDepartment", 160));
    table.getColumns().add(column("Active", "active", 90));

    members = FXCollections.observableArrayList(context.getMemberDao().listAll());
    FilteredList<MemberRecord> filtered = new FilteredList<>(members, item -> true);
    table.setItems(filtered);

    searchField.textProperty().addListener((obs, oldValue, newValue) -> {
      String filter = newValue == null ? "" : newValue.toLowerCase();
      filtered.setPredicate(record -> {
        if (filter.isBlank()) {
          return true;
        }
        return contains(record.getName(), filter) || contains(record.getMemberId(), filter);
      });
    });

    addButton.setOnAction(event -> {
      MemberRecord record = showMemberDialog();
      if (record != null) {
        record.setActive(true);
        context.getMemberDao().insert(record);
        record.setUpdatedAt(Instant.now());
        context.getSyncManager().enqueueMember(record);
        members.setAll(context.getMemberDao().listAll());
      }
    });

    VBox content = new VBox(16, title, controls, table);
    setCenter(content);
  }

  private TableColumn<MemberRecord, Object> column(String label, String property, int width) {
    TableColumn<MemberRecord, Object> column = new TableColumn<>(label);
    column.setCellValueFactory(new PropertyValueFactory<>(property));
    column.setPrefWidth(width);
    return column;
  }

  private MemberRecord showMemberDialog() {
    Dialog<MemberRecord> dialog = new Dialog<>();
    dialog.setTitle("Add Member");

    TextField idField = new TextField();
    TextField nameField = new TextField();
    ComboBox<String> typeBox = new ComboBox<>();
    typeBox.getItems().addAll("STUDENT", "TEACHER");
    typeBox.getSelectionModel().selectFirst();
    TextField classField = new TextField();
    TextField contactField = new TextField();

    GridPane form = new GridPane();
    form.setHgap(12);
    form.setVgap(12);
    form.add(new Label("Member ID"), 0, 0);
    form.add(idField, 1, 0);
    form.add(new Label("Name"), 0, 1);
    form.add(nameField, 1, 1);
    form.add(new Label("Type"), 0, 2);
    form.add(typeBox, 1, 2);
    form.add(new Label("Class/Dept"), 0, 3);
    form.add(classField, 1, 3);
    form.add(new Label("Contact"), 0, 4);
    form.add(contactField, 1, 4);

    dialog.getDialogPane().setContent(form);
    dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK,
        javafx.scene.control.ButtonType.CANCEL);

    dialog.setResultConverter(button -> {
      if (button == javafx.scene.control.ButtonType.OK) {
        MemberRecord record = new MemberRecord();
        record.setMemberId(idField.getText().trim());
        record.setName(nameField.getText().trim());
        record.setType(typeBox.getValue());
        record.setClassOrDepartment(classField.getText().trim());
        record.setContactDetails(contactField.getText().trim());
        return record;
      }
      return null;
    });

    return dialog.showAndWait().orElse(null);
  }

  private boolean contains(String value, String filter) {
    return value != null && value.toLowerCase().contains(filter);
  }
}
