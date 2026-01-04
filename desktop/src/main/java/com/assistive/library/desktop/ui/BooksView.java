package com.assistive.library.desktop.ui;

import com.assistive.library.desktop.AppContext;
import com.assistive.library.desktop.model.BookRecord;
import java.time.Instant;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
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

public class BooksView extends BorderPane {
  private final ObservableList<BookRecord> books;

  public BooksView(AppContext context) {
    setPadding(new Insets(16, 0, 0, 0));

    Label title = new Label("Books");
    title.getStyleClass().add("section-title");

    TextField searchField = new TextField();
    searchField.setPromptText("Search by title, author, ISBN");

    Button addButton = new Button("Add Book");
    addButton.getStyleClass().add("primary-button");

    Button disableButton = new Button("Disable");

    HBox controls = new HBox(12, searchField, addButton, disableButton);
    controls.setAlignment(Pos.CENTER_LEFT);

    TableView<BookRecord> table = new TableView<>();
    table.getColumns().add(column("ISBN", "isbn", 140));
    table.getColumns().add(column("Title", "title", 220));
    table.getColumns().add(column("Author", "author", 160));
    table.getColumns().add(column("Category", "category", 140));
    table.getColumns().add(column("Available", "availableQuantity", 100));
    table.getColumns().add(column("Enabled", "enabled", 90));

    books = FXCollections.observableArrayList(context.getBookDao().listAll());
    FilteredList<BookRecord> filtered = new FilteredList<>(books, item -> true);
    table.setItems(filtered);

    searchField.textProperty().addListener((obs, oldValue, newValue) -> {
      String filter = newValue == null ? "" : newValue.toLowerCase();
      filtered.setPredicate(record -> {
        if (filter.isBlank()) {
          return true;
        }
        return contains(record.getTitle(), filter)
            || contains(record.getAuthor(), filter)
            || contains(record.getIsbn(), filter);
      });
    });

    addButton.setOnAction(event -> {
      BookRecord record = showBookDialog();
      if (record != null) {
        record.setAvailableQuantity(record.getTotalQuantity());
        record.setEnabled(true);
        context.getBookDao().insert(record);
        record.setUpdatedAt(Instant.now());
        context.getSyncManager().enqueueBook(record);
        books.setAll(context.getBookDao().listAll());
      }
    });

    disableButton.setOnAction(event -> {
      BookRecord selected = table.getSelectionModel().getSelectedItem();
      if (selected == null) {
        return;
      }
      context.getBookDao().updateEnabled(selected.getId(), false);
      selected.setEnabled(false);
      selected.setUpdatedAt(Instant.now());
      context.getSyncManager().enqueueBook(selected);
      books.setAll(context.getBookDao().listAll());
    });

    VBox content = new VBox(16, title, controls, table);
    setCenter(content);
  }

  private TableColumn<BookRecord, Object> column(String label, String property, int width) {
    TableColumn<BookRecord, Object> column = new TableColumn<>(label);
    column.setCellValueFactory(new PropertyValueFactory<>(property));
    column.setPrefWidth(width);
    return column;
  }

  private BookRecord showBookDialog() {
    Dialog<BookRecord> dialog = new Dialog<>();
    dialog.setTitle("Add Book");

    TextField isbnField = new TextField();
    TextField titleField = new TextField();
    TextField authorField = new TextField();
    TextField categoryField = new TextField();
    TextField rackField = new TextField();
    TextField quantityField = new TextField("1");

    GridPane form = new GridPane();
    form.setHgap(12);
    form.setVgap(12);
    form.add(new Label("ISBN"), 0, 0);
    form.add(isbnField, 1, 0);
    form.add(new Label("Title"), 0, 1);
    form.add(titleField, 1, 1);
    form.add(new Label("Author"), 0, 2);
    form.add(authorField, 1, 2);
    form.add(new Label("Category"), 0, 3);
    form.add(categoryField, 1, 3);
    form.add(new Label("Rack Location"), 0, 4);
    form.add(rackField, 1, 4);
    form.add(new Label("Total Quantity"), 0, 5);
    form.add(quantityField, 1, 5);

    dialog.getDialogPane().setContent(form);
    dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK,
        javafx.scene.control.ButtonType.CANCEL);

    dialog.setResultConverter(button -> {
      if (button == javafx.scene.control.ButtonType.OK) {
        BookRecord record = new BookRecord();
        record.setIsbn(isbnField.getText().trim());
        record.setTitle(titleField.getText().trim());
        record.setAuthor(authorField.getText().trim());
        record.setCategory(categoryField.getText().trim());
        record.setRackLocation(rackField.getText().trim());
        record.setTotalQuantity(parseInt(quantityField.getText(), 1));
        return record;
      }
      return null;
    });

    return dialog.showAndWait().orElse(null);
  }

  private int parseInt(String value, int fallback) {
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  private boolean contains(String value, String filter) {
    return value != null && value.toLowerCase().contains(filter);
  }
}
