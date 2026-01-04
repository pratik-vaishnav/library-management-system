package com.assistive.library.desktop.ui;

import com.assistive.library.desktop.AppContext;
import com.assistive.library.desktop.model.BookRecord;
import com.assistive.library.desktop.model.LoanRecord;
import com.assistive.library.desktop.model.MemberRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class IssueReturnView extends BorderPane {
  private final AppContext context;
  private TextField returnLoanField;

  public IssueReturnView(AppContext context) {
    this.context = context;
    setPadding(new Insets(16, 0, 0, 0));

    Label title = new Label("Issue and Return");
    title.getStyleClass().add("section-title");

    TabPane tabs = new TabPane();
    tabs.getTabs().add(createIssueTab());
    tabs.getTabs().add(createReturnTab());
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

    VBox content = new VBox(16, title, tabs);
    setCenter(content);
  }

  private Tab createIssueTab() {
    Tab tab = new Tab("Issue Book");

    GridPane form = new GridPane();
    form.setHgap(12);
    form.setVgap(12);

    TextField bookField = new TextField();
    bookField.setPromptText("Book ISBN");

    TextField memberField = new TextField();
    memberField.setPromptText("Member ID");

    TextField dueDaysField = new TextField(String.valueOf(getDueDays()));

    form.add(new Label("Book"), 0, 0);
    form.add(bookField, 1, 0);
    form.add(new Label("Member"), 0, 1);
    form.add(memberField, 1, 1);
    form.add(new Label("Due Days"), 0, 2);
    form.add(dueDaysField, 1, 2);

    AutoCompleteSupport.bind(bookField, () -> bookSuggestions(bookField.getText()), true, 1);
    AutoCompleteSupport.bind(memberField, () -> memberSuggestions(memberField.getText()), true, 1);

    Label status = new Label("Ready to issue.");
    status.getStyleClass().add("muted");

    Button issueButton = new Button("Confirm Issue");
    issueButton.getStyleClass().add("primary-button");
    issueButton.setOnAction(event -> {
      String isbn = bookField.getText().trim();
      String memberId = memberField.getText().trim();
      if (isbn.isEmpty() || memberId.isEmpty()) {
        status.setText("Enter book ISBN and member ID.");
        return;
      }
      BookRecord book = context.getBookDao().findByIsbn(isbn);
      if (book == null) {
        BookRecord quickBook = promptQuickAddBook(isbn);
        if (quickBook == null) {
          status.setText("Book not found. Quick add cancelled.");
          return;
        }
        quickBook.setAvailableQuantity(quickBook.getTotalQuantity());
        quickBook.setEnabled(true);
        book = context.getBookDao().insert(quickBook);
        context.getSyncManager().enqueueBook(book);
      }
      MemberRecord member = context.getMemberDao().findByMemberId(memberId);
      if (member == null) {
        MemberRecord quickMember = promptQuickAddMember(memberId);
        if (quickMember == null) {
          status.setText("Member not found. Quick add cancelled.");
          return;
        }
        quickMember.setActive(true);
        member = context.getMemberDao().insert(quickMember);
        context.getSyncManager().enqueueMember(member);
      }
      if (book == null || member == null) {
        status.setText("Book or member not found in local database.");
        return;
      }
      if (!book.isEnabled() || book.getAvailableQuantity() <= 0) {
        status.setText("Book not available.");
        return;
      }
      int maxActive = getMaxActiveLoans();
      int activeLoans = context.getLoanDao().countActiveLoansForMember(member.getId());
      if (activeLoans >= maxActive) {
        status.setText("Member has reached the active loan limit.");
        return;
      }
      int policyDueDays = getDueDays();
      int dueDays = parseInt(dueDaysField.getText(), policyDueDays);
      if (dueDays > policyDueDays) {
        status.setText("Due days exceed the policy limit.");
        return;
      }
      String externalRef = UUID.randomUUID().toString();
      String deviceId = context.getSyncManager().ensureDeviceId();
      LoanRecord loan = context.getLoanDao().issueLoan(book.getId(), member.getId(), externalRef, deviceId, dueDays);
      context.getBookDao().updateAvailability(book.getId(), book.getAvailableQuantity() - 1);
      book.setAvailableQuantity(book.getAvailableQuantity() - 1);
      book.setUpdatedAt(Instant.now());
      context.getSyncManager().enqueueBook(book);
      context.getSyncManager().enqueueLoan(externalRef, deviceId, book.getIsbn(), member.getMemberId(),
          loan.getIssuedAt(), loan.getDueAt(), loan.getReturnedAt(), loan.getStatus(), loan.getFineAmount(),
          loan.getUpdatedAt());
      status.setText("Issue recorded locally. Loan ID: " + loan.getId() + ".");
      if (returnLoanField != null) {
        returnLoanField.setText(String.valueOf(loan.getId()));
      }
      bookField.clear();
      memberField.clear();
    });

    HBox actions = new HBox(12, issueButton, status);
    actions.setAlignment(Pos.CENTER_LEFT);

    VBox layout = new VBox(16, form, actions);
    layout.setPadding(new Insets(12));

    tab.setContent(layout);
    return tab;
  }

  private Tab createReturnTab() {
    Tab tab = new Tab("Return Book");

    GridPane form = new GridPane();
    form.setHgap(12);
    form.setVgap(12);

    returnLoanField = new TextField();
    returnLoanField.setPromptText("Loan ID");

    TextField fineField = new TextField("0.00");

    form.add(new Label("Loan"), 0, 0);
    form.add(returnLoanField, 1, 0);
    form.add(new Label("Fine"), 0, 1);
    form.add(fineField, 1, 1);

    AutoCompleteSupport.bind(returnLoanField, () -> loanSuggestions(returnLoanField.getText()), false, 1);

    Label status = new Label("Ready to accept returns.");
    status.getStyleClass().add("muted");

    Button returnButton = new Button("Confirm Return");
    returnButton.getStyleClass().add("primary-button");
    returnButton.setOnAction(event -> {
      long loanId = parseLong(returnLoanField.getText(), -1);
      if (loanId <= 0) {
        status.setText("Enter a valid loan ID.");
        return;
      }
      LoanRecord loan = context.getLoanDao().findById(loanId);
      if (loan == null) {
        status.setText("Loan not found.");
        return;
      }
      BigDecimal fineAmount = parseDecimal(fineField.getText(), calculateFine(loan));
      LoanRecord updated = context.getLoanDao().returnLoan(loanId, fineAmount);
      BookRecord book = context.getBookDao().findByIsbn(loan.getBookIsbn());
      if (book != null) {
        int updatedAvailable = Math.min(book.getAvailableQuantity() + 1, book.getTotalQuantity());
        context.getBookDao().updateAvailability(book.getId(), updatedAvailable);
        book.setAvailableQuantity(updatedAvailable);
        book.setUpdatedAt(Instant.now());
        context.getSyncManager().enqueueBook(book);
      }
      context.getSyncManager().enqueueLoan(updated.getExternalRef(), updated.getSourceDeviceId(),
          loan.getBookIsbn(), loan.getMemberIdentifier(), updated.getIssuedAt(), updated.getDueAt(),
          updated.getReturnedAt(), updated.getStatus(), updated.getFineAmount(), updated.getUpdatedAt());
      status.setText("Return recorded locally and queued for sync.");
      returnLoanField.clear();
    });

    HBox actions = new HBox(12, returnButton, status);
    actions.setAlignment(Pos.CENTER_LEFT);

    VBox layout = new VBox(16, form, actions);
    layout.setPadding(new Insets(12));

    tab.setContent(layout);
    return tab;
  }

  private int getDueDays() {
    String value = context.getSettingsDao().getValue("loan.dueDays");
    return parseInt(value, 14);
  }

  private BigDecimal getFinePerDay() {
    String value = context.getSettingsDao().getValue("loan.finePerDay");
    if (value == null || value.isBlank()) {
      return new BigDecimal("1.0");
    }
    return new BigDecimal(value);
  }

  private int getMaxActiveLoans() {
    String value = context.getSettingsDao().getValue("loan.maxActive");
    return parseInt(value, 5);
  }

  private BigDecimal calculateFine(LoanRecord loan) {
    if (loan.getDueAt() == null) {
      return BigDecimal.ZERO;
    }
    long daysLate = ChronoUnit.DAYS.between(loan.getDueAt(), Instant.now());
    if (daysLate <= 0) {
      return BigDecimal.ZERO;
    }
    return getFinePerDay().multiply(BigDecimal.valueOf(daysLate));
  }

  private int parseInt(String value, int fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  private long parseLong(String value, long fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  private BigDecimal parseDecimal(String value, BigDecimal fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return new BigDecimal(value.trim());
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  private List<AutoCompleteSupport.Item> bookSuggestions(String query) {
    return context.getBookDao().search(query, 8).stream()
        .map(book -> new AutoCompleteSupport.Item(
            book.getIsbn() + " - " + book.getTitle(),
            book.getIsbn()))
        .toList();
  }

  private List<AutoCompleteSupport.Item> memberSuggestions(String query) {
    return context.getMemberDao().search(query, 8).stream()
        .map(member -> new AutoCompleteSupport.Item(
            member.getMemberId() + " - " + member.getName(),
            member.getMemberId()))
        .toList();
  }

  private List<AutoCompleteSupport.Item> loanSuggestions(String query) {
    return context.getLoanDao().searchIssued(query, 8).stream()
        .map(loan -> new AutoCompleteSupport.Item(
            buildLoanDisplay(loan),
            String.valueOf(loan.getId())))
        .toList();
  }

  private String buildLoanDisplay(LoanRecord loan) {
    String memberName = loan.getMemberName() == null || loan.getMemberName().isBlank()
        ? loan.getMemberIdentifier()
        : loan.getMemberName();
    String bookTitle = loan.getBookTitle() == null || loan.getBookTitle().isBlank()
        ? loan.getBookIsbn()
        : loan.getBookTitle();
    return loan.getId() + " - " + memberName + " (" + loan.getMemberIdentifier() + ") | "
        + bookTitle + " (" + loan.getBookIsbn() + ")";
  }

  private BookRecord promptQuickAddBook(String isbn) {
    Dialog<BookRecord> dialog = new Dialog<>();
    dialog.setTitle("Quick Add Book");

    TextField isbnField = new TextField(isbn);
    TextField titleField = new TextField();
    TextField authorField = new TextField();
    TextField categoryField = new TextField();
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
    form.add(new Label("Total Quantity"), 0, 4);
    form.add(quantityField, 1, 4);

    dialog.getDialogPane().setContent(form);
    dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK,
        javafx.scene.control.ButtonType.CANCEL);

    dialog.setResultConverter(button -> {
      if (button == javafx.scene.control.ButtonType.OK) {
        String isbnValue = isbnField.getText().trim();
        String titleValue = titleField.getText().trim();
        if (isbnValue.isEmpty() || titleValue.isEmpty()) {
          return null;
        }
        BookRecord record = new BookRecord();
        record.setIsbn(isbnValue);
        record.setTitle(titleValue);
        record.setAuthor(authorField.getText().trim());
        record.setCategory(categoryField.getText().trim());
        record.setRackLocation("");
        record.setTotalQuantity(parseInt(quantityField.getText(), 1));
        return record;
      }
      return null;
    });

    return dialog.showAndWait().orElse(null);
  }

  private MemberRecord promptQuickAddMember(String memberId) {
    Dialog<MemberRecord> dialog = new Dialog<>();
    dialog.setTitle("Quick Add Member");

    TextField idField = new TextField(memberId);
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
        String idValue = idField.getText().trim();
        String nameValue = nameField.getText().trim();
        if (idValue.isEmpty() || nameValue.isEmpty()) {
          return null;
        }
        MemberRecord record = new MemberRecord();
        record.setMemberId(idValue);
        record.setName(nameValue);
        record.setType(typeBox.getValue());
        record.setClassOrDepartment(classField.getText().trim());
        record.setContactDetails(contactField.getText().trim());
        return record;
      }
      return null;
    });

    return dialog.showAndWait().orElse(null);
  }
}
