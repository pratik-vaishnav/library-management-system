package com.assistive.library.desktop.ui;

import com.assistive.library.desktop.AppContext;
import com.assistive.library.desktop.data.LoanDao.BookCount;
import com.assistive.library.desktop.data.LoanDao.MonthlyCount;
import com.assistive.library.desktop.report.ReportExporter;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class ReportsView extends VBox {
  private final ReportExporter exporter = new ReportExporter();

  public ReportsView(AppContext context) {
    setPadding(new Insets(16, 0, 0, 0));
    setSpacing(16);

    Label title = new Label("Reports and Analytics");
    title.getStyleClass().add("section-title");

    HBox actions = new HBox(12);
    actions.setAlignment(Pos.CENTER_LEFT);

    Button exportPdf = new Button("Export Books PDF");
    exportPdf.getStyleClass().add("primary-button");
    applyButtonIcon(exportPdf, "PDF", "action-icon-pdf");
    exportPdf.setOnAction(event -> exportBooksPdf(context));

    Button exportExcel = new Button("Export Loans Excel");
    applyButtonIcon(exportExcel, "XLS", "action-icon-excel");
    exportExcel.setOnAction(event -> exportLoansExcel(context));

    actions.getChildren().addAll(exportPdf, exportExcel);

    GridPane grid = new GridPane();
    grid.setHgap(16);
    grid.setVgap(16);

    grid.add(createMonthlyIssuesChart(context), 0, 0);
    grid.add(createLoanStatusChart(context), 1, 0);
    grid.add(createTopBooksChart(context), 0, 1, 2, 1);

    getChildren().addAll(title, actions, grid);
  }

  private VBox createMonthlyIssuesChart(AppContext context) {
    CategoryAxis xAxis = new CategoryAxis();
    NumberAxis yAxis = new NumberAxis();
    BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
    chart.setLegendVisible(false);
    chart.setTitle("Monthly Issues");

    List<MonthlyCount> counts = context.getLoanDao().countIssuedByMonth(6);
    Collections.reverse(counts);

    XYChart.Series<String, Number> series = new XYChart.Series<>();
    for (MonthlyCount count : counts) {
      series.getData().add(new XYChart.Data<>(count.month(), count.total()));
    }
    chart.getData().add(series);

    VBox box = new VBox(chart);
    box.getStyleClass().add("card");
    box.setPadding(new Insets(12));
    return box;
  }

  private VBox createLoanStatusChart(AppContext context) {
    int overdue = context.getLoanDao().countOverdue();
    int issued = context.getLoanDao().countIssuedToday();
    int returned = context.getLoanDao().countReturnedToday();

    PieChart chart = new PieChart();
    chart.setTitle("Today Snapshot");
    chart.getData().add(new PieChart.Data("Issued", issued));
    chart.getData().add(new PieChart.Data("Returned", returned));
    chart.getData().add(new PieChart.Data("Overdue", overdue));

    VBox box = new VBox(chart);
    box.getStyleClass().add("card");
    box.setPadding(new Insets(12));
    return box;
  }

  private VBox createTopBooksChart(AppContext context) {
    CategoryAxis xAxis = new CategoryAxis();
    NumberAxis yAxis = new NumberAxis();
    BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
    chart.setLegendVisible(false);
    chart.setTitle("Top Books (Usage)");

    List<BookCount> counts = context.getLoanDao().topBooks(8);
    XYChart.Series<String, Number> series = new XYChart.Series<>();
    for (BookCount count : counts) {
      series.getData().add(new XYChart.Data<>(count.title(), count.total()));
    }
    chart.getData().add(series);

    VBox box = new VBox(chart);
    box.getStyleClass().add("card");
    box.setPadding(new Insets(12));
    return box;
  }

  private void exportBooksPdf(AppContext context) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export Books PDF");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
    File file = chooser.showSaveDialog(getScene().getWindow());
    if (file == null) {
      return;
    }
    Path path = file.toPath();
    try {
      exporter.exportBooksPdf(context.getBookDao().listAll(), path, getSchoolName(context),
          getLogoPath(context), getFooterText(context));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private void exportLoansExcel(AppContext context) {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Export Loans Excel");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
    File file = chooser.showSaveDialog(getScene().getWindow());
    if (file == null) {
      return;
    }
    Path path = file.toPath();
    try {
      exporter.exportLoansExcel(context.getLoanDao().listAll(), path, getSchoolName(context));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private String getSchoolName(AppContext context) {
    String name = context.getSettingsDao().getValue("school.name");
    return name == null || name.isBlank() ? "School Library" : name.trim();
  }

  private String getLogoPath(AppContext context) {
    String path = context.getSettingsDao().getValue("school.logoPath");
    return path == null ? "" : path.trim();
  }

  private String getFooterText(AppContext context) {
    String footer = context.getSettingsDao().getValue("school.footer");
    return footer == null || footer.isBlank() ? "Generated by EduShelf" : footer.trim();
  }

  private void applyButtonIcon(Button button, String label, String style) {
    Label icon = new Label(label);
    icon.getStyleClass().add("action-icon");
    icon.getStyleClass().add(style);
    button.setGraphic(icon);
    button.setContentDisplay(ContentDisplay.LEFT);
    button.setGraphicTextGap(8);
  }
}
