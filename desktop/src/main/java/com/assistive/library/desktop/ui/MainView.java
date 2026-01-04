package com.assistive.library.desktop.ui;

import com.assistive.library.desktop.AppContext;
import com.assistive.library.desktop.state.SessionState;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MainView extends BorderPane {
  private final AppContext context;
  private final Label syncStatus;
  private final Label syncNotice;
  private final Button syncButton;
  private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
  private ScheduledExecutorService scheduler;
  private String lastSyncError;
  private Button activeNavButton;
  private Button dashboardButton;
  private Button booksButton;
  private Button membersButton;
  private Button issueButton;
  private Button reportsButton;
  private Button syncQueueButton;
  private Button usersButton;
  private Button settingsButton;

  public MainView(SceneRouter router, AppContext context) {
    this.context = context;

    SessionState session = context.getSessionState();
    setPadding(new Insets(24));

    Label title = new Label("EduShelf");
    title.getStyleClass().add("title");

    Label userInfo = new Label("Signed in as " + session.getUsername() + " (" + session.getRole() + ")");
    userInfo.getStyleClass().add("muted");

    syncStatus = new Label(buildSyncStatus());
    syncStatus.getStyleClass().add("muted");
    syncStatus.getStyleClass().add("status-pill");

    syncNotice = new Label();
    syncNotice.getStyleClass().add("muted");

    syncButton = new Button("Sync Now");
    syncButton.getStyleClass().add("primary-button");
    syncButton.setOnAction(event -> runSync(syncButton));

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox header = new HBox(16, title, userInfo, spacer, syncStatus, syncNotice, syncButton);
    header.setAlignment(Pos.CENTER_LEFT);
    header.getStyleClass().add("app-header");

    VBox nav = buildNavigation(router, session);
    nav.setPadding(new Insets(8));

    setTop(header);
    BorderPane.setMargin(header, new Insets(0, 0, 16, 0));
    setLeft(nav);
    BorderPane.setMargin(nav, new Insets(0, 16, 0, 0));
    setCenter(buildDashboard(session));

    updateSyncIndicators();
    startBackgroundSync();
  }

  private VBox buildNavigation(SceneRouter router, SessionState session) {
    dashboardButton = createNavButton("Dashboard", "D", "nav-icon-dashboard");
    dashboardButton.setOnAction(event -> {
      setCenter(buildDashboard(session));
      setActiveNav(dashboardButton);
    });

    booksButton = createNavButton("Books", "B", "nav-icon-books");
    booksButton.setOnAction(event -> {
      setCenter(new BooksView(context));
      setActiveNav(booksButton);
    });

    membersButton = createNavButton("Members", "M", "nav-icon-members");
    membersButton.setOnAction(event -> {
      setCenter(new MembersView(context));
      setActiveNav(membersButton);
    });

    issueButton = createNavButton("Issue / Return", "I", "nav-icon-issue");
    issueButton.setOnAction(event -> {
      setCenter(new IssueReturnView(context));
      setActiveNav(issueButton);
    });

    reportsButton = createNavButton("Reports", "R", "nav-icon-reports");
    reportsButton.setOnAction(event -> {
      setCenter(new ReportsView(context));
      setActiveNav(reportsButton);
    });

    syncQueueButton = createNavButton("Sync Queue", "Q", "nav-icon-sync");
    syncQueueButton.setOnAction(event -> {
      setCenter(new SyncQueueView(context));
      setActiveNav(syncQueueButton);
    });

    usersButton = createNavButton("Users", "U", "nav-icon-users");
    usersButton.setOnAction(event -> {
      setCenter(new UsersView(context));
      setActiveNav(usersButton);
    });

    settingsButton = createNavButton("Settings", "S", "nav-icon-settings");
    settingsButton.setOnAction(event -> {
      setCenter(new SettingsView(context));
      setActiveNav(settingsButton);
    });

    boolean adminOnly = !"ADMIN".equalsIgnoreCase(session.getRole());
    usersButton.setDisable(adminOnly);
    settingsButton.setDisable(adminOnly);

    Button logoutButton = createNavButton("Logout", "L", "nav-icon-logout");
    logoutButton.getStyleClass().add("danger-button");
    logoutButton.setOnAction(event -> {
      stopBackgroundSync();
      session.setToken(null);
      session.setUsername(null);
      session.setRole(null);
      session.setExpiresAt(null);
      context.getApiClient().setAuthToken(null);
      router.showLogin();
    });

    VBox nav = new VBox(12, dashboardButton, booksButton, membersButton, issueButton,
        reportsButton, syncQueueButton, usersButton, settingsButton, logoutButton);
    nav.getStyleClass().add("nav");
    setActiveNav(dashboardButton);
    return nav;
  }

  private Button createNavButton(String label, String iconText, String iconStyle) {
    Label icon = new Label(iconText);
    icon.getStyleClass().add("nav-icon");
    icon.getStyleClass().add(iconStyle);

    Button button = new Button(label, icon);
    button.setMaxWidth(Double.MAX_VALUE);
    button.getStyleClass().add("nav-button");
    button.setContentDisplay(ContentDisplay.LEFT);
    button.setGraphicTextGap(10);
    return button;
  }

  private void setActiveNav(Button button) {
    if (activeNavButton != null) {
      activeNavButton.getStyleClass().remove("nav-button-active");
    }
    activeNavButton = button;
    if (activeNavButton != null && !activeNavButton.getStyleClass().contains("nav-button-active")) {
      activeNavButton.getStyleClass().add("nav-button-active");
    }
  }

  private DashboardView buildDashboard(SessionState session) {
    return new DashboardView(
        context,
        session.getUsername(),
        this::showIssueReturn,
        this::showBooks,
        this::showMembers,
        () -> syncButton.fire()
    );
  }

  private void showIssueReturn() {
    setCenter(new IssueReturnView(context));
    setActiveNav(issueButton);
  }

  private void showBooks() {
    setCenter(new BooksView(context));
    setActiveNav(booksButton);
  }

  private void showMembers() {
    setCenter(new MembersView(context));
    setActiveNav(membersButton);
  }

  private void runSync(Button syncButton) {
    if (!syncInProgress.compareAndSet(false, true)) {
      syncNotice.setText("Sync already running.");
      syncNotice.getStyleClass().add("warning");
      return;
    }
    syncStatus.setText("Sync: In progress...");
    syncButton.setDisable(true);
    lastSyncError = null;
    syncNotice.setText("");

    javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
      @Override
      protected Void call() throws Exception {
        context.getSyncManager().sync();
        return null;
      }
    };

    task.setOnSucceeded(done -> {
      syncInProgress.set(false);
      syncStatus.setText(buildSyncStatus());
      updateSyncIndicators();
      syncButton.setDisable(false);
    });

    task.setOnFailed(done -> {
      syncInProgress.set(false);
      lastSyncError = "Sync failed. Check server connection.";
      syncStatus.setText("Sync: Failed | Check server connection");
      updateSyncIndicators();
      syncButton.setDisable(false);
    });

    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
  }

  private String buildSyncStatus() {
    String lastSync = context.getSettingsDao().getValue(com.assistive.library.desktop.sync.SyncManager.LAST_SYNC_KEY);
    if (lastSync == null) {
      lastSync = "--";
    }
    int pending = context.getSyncManager().pendingCount();
    int failed = context.getSyncQueueDao().countFailed();
    int conflicts = context.getSyncQueueDao().countConflicts();
    return "Sync: " + pending + " pending, " + conflicts + " conflicts, " + failed + " failed | Last sync: " + lastSync;
  }

  private void startBackgroundSync() {
    scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable);
      thread.setDaemon(true);
      thread.setName("background-sync");
      return thread;
    });

    scheduler.scheduleWithFixedDelay(() -> {
      if (context.getSessionState().getToken() == null) {
        return;
      }
      if (!syncInProgress.compareAndSet(false, true)) {
        return;
      }
      try {
        context.getSyncManager().sync();
        lastSyncError = null;
      } catch (Exception ex) {
        lastSyncError = "Background sync failed.";
      } finally {
        syncInProgress.set(false);
        Platform.runLater(this::updateSyncIndicators);
      }
    }, 120, 300, TimeUnit.SECONDS);
  }

  private void stopBackgroundSync() {
    if (scheduler != null) {
      scheduler.shutdownNow();
      scheduler = null;
    }
  }

  private void updateSyncIndicators() {
    syncStatus.setText(buildSyncStatus());
    int conflicts = context.getSyncQueueDao().countConflicts();
    syncNotice.getStyleClass().removeAll("warning", "error");
    if (conflicts > 0) {
      syncNotice.setText("Conflicts: " + conflicts + " (resolve in Sync Queue)");
      syncNotice.getStyleClass().add("warning");
    } else if (lastSyncError != null) {
      syncNotice.setText(lastSyncError);
      syncNotice.getStyleClass().add("error");
    } else {
      syncNotice.setText("");
    }
  }
}
