package com.assistive.library.desktop.ui;

import com.assistive.library.desktop.AppContext;
import com.assistive.library.desktop.api.ApiException;
import com.assistive.library.desktop.api.BookLookupResponse;
import com.assistive.library.desktop.api.BookSyncItem;
import com.assistive.library.desktop.api.LoanLookupResponse;
import com.assistive.library.desktop.api.LoanSyncItem;
import com.assistive.library.desktop.api.LookupApi;
import com.assistive.library.desktop.api.MemberLookupResponse;
import com.assistive.library.desktop.api.MemberSyncItem;
import com.assistive.library.desktop.model.SyncQueueItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TableRow;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SyncQueueView extends BorderPane {
  private final ObservableList<SyncQueueItem> items = FXCollections.observableArrayList();
  private final Label status = new Label();
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private final TableView<DiffRow> diffTable = new TableView<>();
  private final Label detailsMeta = new Label();
  private final Label detailsStatus = new Label();

  public SyncQueueView(AppContext context) {
    setPadding(new Insets(16, 0, 0, 0));

    Label title = new Label("Sync Queue");
    title.getStyleClass().add("section-title");

    Button refreshButton = new Button("Refresh");
    Button retryButton = new Button("Retry Failed");
    retryButton.getStyleClass().add("primary-button");
    Button acceptButton = new Button("Accept Server");
    Button overwriteButton = new Button("Overwrite Server");

    HBox controls = new HBox(12, refreshButton, retryButton, acceptButton, overwriteButton, status);
    controls.setAlignment(Pos.CENTER_LEFT);

    TableView<SyncQueueItem> table = new TableView<>();
    table.getColumns().add(column("Type", "entityType", 100));
    table.getColumns().add(column("Entity", "entityId", 180));
    table.getColumns().add(column("Status", "status", 100));
    table.getColumns().add(column("Last Attempt", "lastAttemptAt", 160));
    table.getColumns().add(column("Error", "errorMessage", 320));
    table.setItems(items);
    table.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> loadDetails(context, newItem));

    refreshButton.setOnAction(event -> load(context));
    retryButton.setOnAction(event -> {
      context.getSyncQueueDao().retryFailed();
      load(context);
    });
    acceptButton.setOnAction(event -> acceptServer(context, table.getSelectionModel().getSelectedItem()));
    overwriteButton.setOnAction(event -> overwriteServer(context, table.getSelectionModel().getSelectedItem()));

    VBox content = new VBox(16, title, controls, table);
    setCenter(content);

    Label detailsTitle = new Label("Conflict Details");
    detailsTitle.getStyleClass().add("section-title");
    detailsMeta.getStyleClass().add("muted");
    detailsMeta.setText("Select a conflict item to view details.");
    detailsStatus.getStyleClass().add("muted");

    diffTable.getColumns().add(column("Field", "field", 140));
    diffTable.getColumns().add(column("Local", "localValue", 200));
    diffTable.getColumns().add(column("Server", "serverValue", 200));
    diffTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    diffTable.setRowFactory(view -> new TableRow<>() {
      @Override
      protected void updateItem(DiffRow item, boolean empty) {
        super.updateItem(item, empty);
        getStyleClass().remove("diff-row");
        if (!empty && item != null && item.isDifferent()) {
          getStyleClass().add("diff-row");
        }
      }
    });

    VBox drawer = new VBox(12, detailsTitle, detailsMeta, diffTable, detailsStatus);
    drawer.getStyleClass().add("drawer");
    drawer.setPadding(new Insets(12));
    drawer.setPrefWidth(360);
    setRight(drawer);

    load(context);
  }

  private <T> TableColumn<T, Object> column(String label, String property, int width) {
    TableColumn<T, Object> column = new TableColumn<>(label);
    column.setCellValueFactory(new PropertyValueFactory<>(property));
    column.setPrefWidth(width);
    return column;
  }

  private void load(AppContext context) {
    items.setAll(context.getSyncQueueDao().listAll());
    int failed = context.getSyncQueueDao().countFailed();
    int pending = context.getSyncQueueDao().countPending();
    int conflicts = context.getSyncQueueDao().countConflicts();
    status.setText("Pending: " + pending + " | Conflicts: " + conflicts + " | Failed: " + failed);
  }

  private void acceptServer(AppContext context, SyncQueueItem item) {
    if (item == null || item.getClientRef() == null) {
      status.setText("Select a conflict item to accept.");
      return;
    }
    context.getSyncQueueDao().markSyncedByClientRefs(java.util.List.of(item.getClientRef()));
    runSync(context, "Server version accepted.");
  }

  private void overwriteServer(AppContext context, SyncQueueItem item) {
    if (item == null || item.getClientRef() == null) {
      status.setText("Select a conflict item to overwrite.");
      return;
    }
    try {
      String payload = item.getPayload();
      switch (item.getEntityType()) {
        case "BOOK" -> {
          BookSyncItem book = mapper.readValue(payload, BookSyncItem.class);
          book.setForce(true);
          payload = mapper.writeValueAsString(book);
        }
        case "MEMBER" -> {
          MemberSyncItem member = mapper.readValue(payload, MemberSyncItem.class);
          member.setForce(true);
          payload = mapper.writeValueAsString(member);
        }
        case "LOAN" -> {
          LoanSyncItem loan = mapper.readValue(payload, LoanSyncItem.class);
          loan.setForce(true);
          payload = mapper.writeValueAsString(loan);
        }
        default -> {
          status.setText("Unsupported entity type.");
          return;
        }
      }
      context.getSyncQueueDao().updatePayloadAndStatus(item.getClientRef(), payload, "PENDING");
      runSync(context, "Overwrite queued for sync.");
    } catch (Exception ex) {
      status.setText("Failed to prepare overwrite.");
    }
  }

  private void runSync(AppContext context, String message) {
    status.setText(message + " Syncing...");
    Task<Void> task = new Task<>() {
      @Override
      protected Void call() throws Exception {
        context.getSyncManager().sync();
        return null;
      }
    };
    task.setOnSucceeded(event -> {
      load(context);
      status.setText(message);
    });
    task.setOnFailed(event -> status.setText("Sync failed."));
    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
  }

  private void loadDetails(AppContext context, SyncQueueItem item) {
    diffTable.getItems().clear();
    if (item == null || !"CONFLICT".equalsIgnoreCase(item.getStatus())) {
      detailsMeta.setText("Select a conflict item to view details.");
      detailsStatus.setText("");
      return;
    }

    detailsMeta.setText(item.getEntityType() + " - " + item.getEntityId());
    detailsStatus.setText("Loading server version...");

    Task<DiffPayload> task = new Task<>() {
      @Override
      protected DiffPayload call() throws Exception {
        return buildDiffPayload(context.getLookupApi(), item);
      }
    };

    task.setOnSucceeded(event -> {
      DiffPayload payload = task.getValue();
      diffTable.getItems().setAll(payload.rows());
      detailsStatus.setText(payload.statusMessage());
    });

    task.setOnFailed(event -> {
      detailsStatus.setText("Unable to load conflict details.");
    });

    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
  }

  private DiffPayload buildDiffPayload(LookupApi lookupApi, SyncQueueItem item) throws Exception {
    Map<String, String> localValues = new LinkedHashMap<>();
    Map<String, String> serverValues = new LinkedHashMap<>();
    String statusMessage = "Server version loaded.";

    try {
      switch (item.getEntityType()) {
        case "BOOK" -> {
          BookSyncItem local = mapper.readValue(item.getPayload(), BookSyncItem.class);
          localValues = toBookMap(local);
          BookLookupResponse server = lookupApi.getBookByIsbn(local.getIsbn());
          serverValues = toBookMap(server);
        }
        case "MEMBER" -> {
          MemberSyncItem local = mapper.readValue(item.getPayload(), MemberSyncItem.class);
          localValues = toMemberMap(local);
          MemberLookupResponse server = lookupApi.getMemberById(local.getMemberId());
          serverValues = toMemberMap(server);
        }
        case "LOAN" -> {
          LoanSyncItem local = mapper.readValue(item.getPayload(), LoanSyncItem.class);
          localValues = toLoanMap(local);
          LoanLookupResponse server = lookupApi.getLoanByExternalRef(local.getExternalRef());
          serverValues = toLoanMap(server);
        }
        default -> statusMessage = "Unsupported entity type.";
      }
    } catch (ApiException ex) {
      if (ex.getStatusCode() == 404) {
        statusMessage = "Server record not found.";
      } else {
        throw ex;
      }
    }

    List<DiffRow> rows = new ArrayList<>();
    for (Map.Entry<String, String> entry : localValues.entrySet()) {
      String field = entry.getKey();
      String local = entry.getValue();
      String server = serverValues.getOrDefault(field, "--");
      rows.add(new DiffRow(field, local, server, !Objects.equals(local, server)));
    }

    return new DiffPayload(rows, statusMessage);
  }

  private Map<String, String> toBookMap(BookSyncItem item) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("ISBN", value(item.getIsbn()));
    map.put("Title", value(item.getTitle()));
    map.put("Author", value(item.getAuthor()));
    map.put("Category", value(item.getCategory()));
    map.put("Rack", value(item.getRackLocation()));
    map.put("Total Qty", String.valueOf(item.getTotalQuantity()));
    map.put("Available Qty", String.valueOf(item.getAvailableQuantity()));
    map.put("Enabled", formatBoolean(item.isEnabled()));
    map.put("Updated At", formatInstant(item.getUpdatedAt()));
    return map;
  }

  private Map<String, String> toBookMap(BookLookupResponse item) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("ISBN", value(item.getIsbn()));
    map.put("Title", value(item.getTitle()));
    map.put("Author", value(item.getAuthor()));
    map.put("Category", value(item.getCategory()));
    map.put("Rack", value(item.getRackLocation()));
    map.put("Total Qty", String.valueOf(item.getTotalQuantity()));
    map.put("Available Qty", String.valueOf(item.getAvailableQuantity()));
    map.put("Enabled", formatBoolean(item.isEnabled()));
    map.put("Updated At", formatInstant(item.getUpdatedAt()));
    return map;
  }

  private Map<String, String> toMemberMap(MemberSyncItem item) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("Member ID", value(item.getMemberId()));
    map.put("Name", value(item.getName()));
    map.put("Type", value(item.getType()));
    map.put("Class/Dept", value(item.getClassOrDepartment()));
    map.put("Contact", value(item.getContactDetails()));
    map.put("Active", formatBoolean(item.isActive()));
    map.put("Updated At", formatInstant(item.getUpdatedAt()));
    return map;
  }

  private Map<String, String> toMemberMap(MemberLookupResponse item) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("Member ID", value(item.getMemberId()));
    map.put("Name", value(item.getName()));
    map.put("Type", value(item.getType()));
    map.put("Class/Dept", value(item.getClassOrDepartment()));
    map.put("Contact", value(item.getContactDetails()));
    map.put("Active", formatBoolean(item.isActive()));
    map.put("Updated At", formatInstant(item.getUpdatedAt()));
    return map;
  }

  private Map<String, String> toLoanMap(LoanSyncItem item) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("External Ref", value(item.getExternalRef()));
    map.put("Book ISBN", value(item.getBookIsbn()));
    map.put("Member ID", value(item.getMemberId()));
    map.put("Issued At", formatInstant(item.getIssuedAt()));
    map.put("Due At", formatInstant(item.getDueAt()));
    map.put("Returned At", formatInstant(item.getReturnedAt()));
    map.put("Status", value(item.getStatus()));
    map.put("Fine", value(item.getFineAmount()));
    map.put("Updated At", formatInstant(item.getUpdatedAt()));
    return map;
  }

  private Map<String, String> toLoanMap(LoanLookupResponse item) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("External Ref", value(item.getExternalRef()));
    map.put("Book ISBN", value(item.getBookIsbn()));
    map.put("Member ID", value(item.getMemberId()));
    map.put("Issued At", formatInstant(item.getIssuedAt()));
    map.put("Due At", formatInstant(item.getDueAt()));
    map.put("Returned At", formatInstant(item.getReturnedAt()));
    map.put("Status", value(item.getStatus()));
    map.put("Fine", value(item.getFineAmount()));
    map.put("Updated At", formatInstant(item.getUpdatedAt()));
    return map;
  }

  private String value(Object value) {
    return value == null ? "--" : value.toString();
  }

  private String formatInstant(Instant value) {
    return value == null ? "--" : value.toString();
  }

  private String formatBoolean(boolean value) {
    return value ? "Yes" : "No";
  }

  private record DiffPayload(List<DiffRow> rows, String statusMessage) {
  }

  public static class DiffRow {
    private final String field;
    private final String localValue;
    private final String serverValue;
    private final boolean different;

    public DiffRow(String field, String localValue, String serverValue, boolean different) {
      this.field = field;
      this.localValue = localValue;
      this.serverValue = serverValue;
      this.different = different;
    }

    public String getField() {
      return field;
    }

    public String getLocalValue() {
      return localValue;
    }

    public String getServerValue() {
      return serverValue;
    }

    public boolean isDifferent() {
      return different;
    }
  }
}
