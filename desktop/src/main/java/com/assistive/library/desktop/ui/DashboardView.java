package com.assistive.library.desktop.ui;

import com.assistive.library.desktop.AppContext;
import com.assistive.library.desktop.ai.AiSummaryService;
import com.assistive.library.desktop.data.DemoDataSeeder;
import com.assistive.library.desktop.model.BookRecord;
import com.assistive.library.desktop.model.MemberRecord;
import java.io.IOException;
import java.time.Instant;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;
import javafx.util.Duration;

public class DashboardView extends VBox {
  private final Runnable openIssueReturn;
  private final Runnable openBooks;
  private final Runnable openMembers;
  private final Runnable triggerSync;
  private final boolean reduceMotion;

  public DashboardView(AppContext context,
                       String username,
                       Runnable openIssueReturn,
                       Runnable openBooks,
                       Runnable openMembers,
                       Runnable triggerSync) {
    this.openIssueReturn = openIssueReturn;
    this.openBooks = openBooks;
    this.openMembers = openMembers;
    this.triggerSync = triggerSync;
    this.reduceMotion = isReducedMotion(context);

    setPadding(new Insets(16, 0, 0, 0));
    setSpacing(20);

    VBox hero = buildHero(context, username);
    applyEntrance(hero, 0);

    Label heading = new Label("Daily Overview");
    heading.getStyleClass().add("section-title");

    Label greeting = new Label("Here is the latest library snapshot.");
    greeting.getStyleClass().add("muted");

    int issuedToday = context.getLoanDao().countIssuedToday();
    int returnsToday = context.getLoanDao().countReturnedToday();
    int overdue = context.getLoanDao().countOverdue();
    int pendingSync = context.getSyncManager().pendingCount();

    GridPane statsGrid = new GridPane();
    statsGrid.setHgap(16);
    statsGrid.setVgap(16);

    VBox issuedCard = createStatCard("Issued Today", String.valueOf(issuedToday));
    VBox returnedCard = createStatCard("Returns Today", String.valueOf(returnsToday));
    VBox overdueCard = createStatCard("Overdue", String.valueOf(overdue));
    VBox pendingCard = createStatCard("Pending Sync", String.valueOf(pendingSync));

    applyEntrance(issuedCard, 1);
    applyEntrance(returnedCard, 2);
    applyEntrance(overdueCard, 3);
    applyEntrance(pendingCard, 4);

    statsGrid.add(issuedCard, 0, 0);
    statsGrid.add(returnedCard, 1, 0);
    statsGrid.add(overdueCard, 0, 1);
    statsGrid.add(pendingCard, 1, 1);

    VBox summaryList = new VBox(8);
    summaryList.getStyleClass().add("summary-list");
    summaryList.setFillWidth(true);

    Label placeholder = new Label("Click Generate Summary to create a daily overview using Ollama.");
    placeholder.getStyleClass().add("muted");
    placeholder.setWrapText(true);
    summaryList.getChildren().add(placeholder);

    Label summaryTitle = new Label("AI Daily Summary");
    summaryTitle.getStyleClass().add("section-title");

    Label summaryStatus = new Label();
    summaryStatus.getStyleClass().add("muted");

    ProgressIndicator summaryProgress = new ProgressIndicator();
    summaryProgress.setMaxSize(18, 18);
    summaryProgress.setVisible(false);

    Button generateSummary = new Button("Generate Summary");
    generateSummary.getStyleClass().add("primary-button");
    generateSummary.setOnAction(event -> runSummary(context, summaryList, summaryStatus, summaryProgress, generateSummary));

    HBox summaryActions = new HBox(12, generateSummary, summaryProgress, summaryStatus);
    summaryActions.setAlignment(Pos.CENTER_LEFT);

    ScrollPane summaryPane = new ScrollPane(summaryList);
    summaryPane.setFitToWidth(true);
    summaryPane.setPrefViewportHeight(200);
    summaryPane.getStyleClass().add("summary-pane");

    VBox summarySection = new VBox(8, summaryTitle, summaryActions, summaryPane);
    applyEntrance(summarySection, 5);

    Button seedButton = new Button("Seed Demo Data");
    Label seedStatus = new Label();
    seedStatus.getStyleClass().add("muted");
    seedButton.setOnAction(event -> {
      DemoDataSeeder.SeedResult result = new DemoDataSeeder(
          context.getBookDao(),
          context.getMemberDao(),
          context.getSyncManager(),
          context.getSettingsDao()
      ).seed();
      seedStatus.setText(result.getMessage() + " Books: " + result.getBooksAdded()
          + ", Members: " + result.getMembersAdded() + ".");
    });

    HBox seedRow = new HBox(12, seedButton, seedStatus);
    seedRow.setAlignment(Pos.CENTER_LEFT);

    getChildren().addAll(hero, heading, greeting, statsGrid, seedRow, summarySection);
  }

  private VBox createStatCard(String label, String value) {
    Label valueLabel = new Label(value);
    valueLabel.getStyleClass().add("stat-value");

    Label labelLabel = new Label(label);
    labelLabel.getStyleClass().add("stat-label");

    VBox card = new VBox(6, valueLabel, labelLabel);
    card.getStyleClass().add("card");
    card.setPadding(new Insets(16));
    card.setMinWidth(220);
    return card;
  }

  private void runSummary(AppContext context,
                          VBox summaryList,
                          Label status,
                          ProgressIndicator progress,
                          Button generateButton) {
    status.getStyleClass().remove("error");
    status.setText("Generating summary...");
    progress.setVisible(true);
    generateButton.setDisable(true);

    Task<String> task = new Task<>() {
      @Override
      protected String call() throws Exception {
        AiSummaryService service = new AiSummaryService(
            context.getSettingsDao(),
            context.getBookDao(),
            context.getMemberDao(),
            context.getLoanDao(),
            context.getSyncManager()
        );
        return service.generateDailySummary();
      }
    };

    task.setOnSucceeded(event -> {
      updateSummaryList(summaryList, task.getValue());
      status.setText("Summary updated.");
      progress.setVisible(false);
      generateButton.setDisable(false);
    });

    task.setOnFailed(event -> {
      Throwable error = task.getException();
      String message = error instanceof IOException ? error.getMessage() : "Summary failed.";
      updateSummaryList(summaryList, "Unable to generate summary.");
      status.setText(message);
      status.getStyleClass().add("error");
      progress.setVisible(false);
      generateButton.setDisable(false);
    });

    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
  }

  private void updateSummaryList(VBox summaryList, String text) {
    summaryList.getChildren().clear();
    if (text == null || text.isBlank()) {
      Label empty = new Label("No summary available.");
      empty.getStyleClass().add("muted");
      summaryList.getChildren().add(empty);
      applyEntrance(empty, 0);
      return;
    }
    String[] lines = text.split("\\r?\\n");
    boolean added = false;
    int index = 0;
    for (String line : lines) {
      String cleaned = line.trim();
      if (cleaned.isEmpty()) {
        continue;
      }
      if (cleaned.startsWith("-") || cleaned.startsWith("*")) {
        cleaned = cleaned.substring(1).trim();
      }
      Label bullet = new Label("- " + cleaned);
      bullet.getStyleClass().add("summary-bullet");
      bullet.setWrapText(true);
      bullet.setMaxWidth(Double.MAX_VALUE);
      summaryList.getChildren().add(bullet);
      applyEntrance(bullet, index++);
      added = true;
    }
    if (!added) {
      Label single = new Label("- " + text.trim());
      single.getStyleClass().add("summary-bullet");
      single.setWrapText(true);
      single.setMaxWidth(Double.MAX_VALUE);
      summaryList.getChildren().add(single);
      applyEntrance(single, 0);
    }
  }

  private VBox buildHero(AppContext context, String username) {
    Label heroTitle = new Label("Welcome back, " + username);
    heroTitle.getStyleClass().add("hero-title");

    Label heroTagline = new Label("A calm, inclusive space that keeps every reader connected, even offline.");
    heroTagline.getStyleClass().add("hero-tagline");
    heroTagline.setWrapText(true);

    Button issueAction = new Button("Issue / Return");
    issueAction.getStyleClass().add("primary-button");
    issueAction.setOnAction(event -> openIssueReturn.run());

    Button quickAddAction = new Button("Quick Add");
    quickAddAction.getStyleClass().add("hero-button");
    quickAddAction.setOnAction(event -> showQuickAddDialog(context));

    Button addBookAction = new Button("Add Book");
    addBookAction.getStyleClass().add("hero-button");
    addBookAction.setOnAction(event -> openBooks.run());

    Button addMemberAction = new Button("Add Member");
    addMemberAction.getStyleClass().add("hero-button");
    addMemberAction.setOnAction(event -> openMembers.run());

    Button syncAction = new Button("Sync Now");
    syncAction.getStyleClass().add("hero-button");
    syncAction.setOnAction(event -> triggerSync.run());

    HBox actions = new HBox(12, issueAction, quickAddAction, addBookAction, addMemberAction, syncAction);
    actions.setAlignment(Pos.CENTER_LEFT);

    VBox hero = new VBox(10, heroTitle, heroTagline, actions);
    hero.getStyleClass().add("hero");
    return hero;
  }

  private void applyEntrance(Node node, int index) {
    if (reduceMotion) {
      node.setOpacity(1);
      node.setTranslateY(0);
      return;
    }
    node.setOpacity(0);
    node.setTranslateY(6);
    FadeTransition fade = new FadeTransition(Duration.millis(200), node);
    fade.setFromValue(0);
    fade.setToValue(1);

    TranslateTransition translate = new TranslateTransition(Duration.millis(200), node);
    translate.setFromY(6);
    translate.setToY(0);

    ParallelTransition transition = new ParallelTransition(fade, translate);
    transition.setDelay(Duration.millis(70L * index));
    transition.play();
  }

  private boolean isReducedMotion(AppContext context) {
    String value = context.getSettingsDao().getValue("ui.reduceMotion");
    return value != null && value.equalsIgnoreCase("true");
  }

  private void showQuickAddDialog(AppContext context) {
    Dialog<Void> dialog = new Dialog<>();
    dialog.setTitle("Quick Add");
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    ComboBox<String> typeBox = new ComboBox<>();
    typeBox.getItems().addAll("Book", "Member");
    typeBox.getSelectionModel().selectFirst();

    TextField isbnField = new TextField();
    TextField titleField = new TextField();
    TextField authorField = new TextField();
    TextField categoryField = new TextField();
    TextField quantityField = new TextField("1");

    GridPane bookForm = new GridPane();
    bookForm.setHgap(10);
    bookForm.setVgap(10);
    bookForm.add(new Label("ISBN"), 0, 0);
    bookForm.add(isbnField, 1, 0);
    bookForm.add(new Label("Title"), 0, 1);
    bookForm.add(titleField, 1, 1);
    bookForm.add(new Label("Author"), 0, 2);
    bookForm.add(authorField, 1, 2);
    bookForm.add(new Label("Category"), 0, 3);
    bookForm.add(categoryField, 1, 3);
    bookForm.add(new Label("Quantity"), 0, 4);
    bookForm.add(quantityField, 1, 4);

    TextField memberIdField = new TextField();
    TextField memberNameField = new TextField();
    ComboBox<String> memberTypeBox = new ComboBox<>();
    memberTypeBox.getItems().addAll("Student", "Teacher");
    memberTypeBox.getSelectionModel().selectFirst();
    TextField classField = new TextField();
    TextField contactField = new TextField();

    GridPane memberForm = new GridPane();
    memberForm.setHgap(10);
    memberForm.setVgap(10);
    memberForm.add(new Label("Member ID"), 0, 0);
    memberForm.add(memberIdField, 1, 0);
    memberForm.add(new Label("Name"), 0, 1);
    memberForm.add(memberNameField, 1, 1);
    memberForm.add(new Label("Type"), 0, 2);
    memberForm.add(memberTypeBox, 1, 2);
    memberForm.add(new Label("Class/Dept"), 0, 3);
    memberForm.add(classField, 1, 3);
    memberForm.add(new Label("Contact"), 0, 4);
    memberForm.add(contactField, 1, 4);

    Label errorLabel = new Label();
    errorLabel.getStyleClass().add("error");

    VBox content = new VBox(12, new Label("Add Type"), typeBox, bookForm, memberForm, errorLabel);
    dialog.getDialogPane().setContent(content);

    memberForm.setVisible(false);
    memberForm.setManaged(false);

    typeBox.valueProperty().addListener((obs, oldValue, newValue) -> {
      boolean isBook = "Book".equalsIgnoreCase(newValue);
      bookForm.setVisible(isBook);
      bookForm.setManaged(isBook);
      memberForm.setVisible(!isBook);
      memberForm.setManaged(!isBook);
      errorLabel.setText("");
    });

    Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
    okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
      errorLabel.setText("");
      if ("Book".equalsIgnoreCase(typeBox.getValue())) {
        String isbn = isbnField.getText().trim();
        String title = titleField.getText().trim();
        if (isbn.isEmpty() || title.isEmpty()) {
          errorLabel.setText("ISBN and Title are required.");
          event.consume();
          return;
        }
        int qty = parseInt(quantityField.getText().trim(), 1);
        BookRecord record = new BookRecord();
        record.setIsbn(isbn);
        record.setTitle(title);
        record.setAuthor(authorField.getText().trim());
        record.setCategory(categoryField.getText().trim());
        record.setRackLocation("");
        record.setTotalQuantity(qty);
        record.setAvailableQuantity(qty);
        record.setEnabled(true);
        record.setUpdatedAt(Instant.now());
        context.getBookDao().insert(record);
        context.getSyncManager().enqueueBook(record);
      } else {
        String memberId = memberIdField.getText().trim();
        String name = memberNameField.getText().trim();
        if (memberId.isEmpty() || name.isEmpty()) {
          errorLabel.setText("Member ID and Name are required.");
          event.consume();
          return;
        }
        MemberRecord record = new MemberRecord();
        record.setMemberId(memberId);
        record.setName(name);
        record.setType(memberTypeBox.getValue());
        record.setClassOrDepartment(classField.getText().trim());
        record.setContactDetails(contactField.getText().trim());
        record.setActive(true);
        record.setUpdatedAt(Instant.now());
        context.getMemberDao().insert(record);
        context.getSyncManager().enqueueMember(record);
      }
    });

    dialog.showAndWait();
  }

  private int parseInt(String value, int fallback) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }
}
