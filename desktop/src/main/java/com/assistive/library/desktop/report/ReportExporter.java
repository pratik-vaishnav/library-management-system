package com.assistive.library.desktop.report;

import com.assistive.library.desktop.model.BookRecord;
import com.assistive.library.desktop.model.LoanRecord;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ReportExporter {
  public void exportBooksPdf(List<BookRecord> books,
                             Path outputPath,
                             String schoolName,
                             String logoPath,
                             String footerText) throws IOException {
    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage(PDRectangle.LETTER);
      document.addPage(page);

      float startY = 750;
      float margin = 50;
      float y = startY;

      PDPageContentStream content = new PDPageContentStream(document, page);
      y = renderHeader(document, content, y, schoolName, "Books Report", logoPath);
      y = renderTableHeader(content, y, margin);

      content.setFont(PDType1Font.HELVETICA, 11);
      for (BookRecord book : books) {
        if (y < margin) {
          content.close();
          page = new PDPage(PDRectangle.LETTER);
          document.addPage(page);
          content = new PDPageContentStream(document, page);
          y = startY;
          y = renderHeader(document, content, y, schoolName, "Books Report", logoPath);
          y = renderTableHeader(content, y, margin);
          content.setFont(PDType1Font.HELVETICA, 11);
        }
        float x = margin;
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(truncate(book.getIsbn(), 15));
        content.endText();

        content.beginText();
        content.newLineAtOffset(x + 100, y);
        content.showText(truncate(book.getTitle(), 28));
        content.endText();

        content.beginText();
        content.newLineAtOffset(x + 320, y);
        content.showText(truncate(safe(book.getAuthor()), 18));
        content.endText();

        content.beginText();
        content.newLineAtOffset(x + 470, y);
        content.showText(book.getAvailableQuantity() + "/" + book.getTotalQuantity());
        content.endText();
        y -= 18;
      }

      content.close();
      addFooters(document, footerText);
      document.save(outputPath.toFile());
    }
  }

  public void exportLoansExcel(List<LoanRecord> loans, Path outputPath, String schoolName) throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Loans");

      Font boldFont = workbook.createFont();
      boldFont.setBold(true);

      CellStyle titleStyle = workbook.createCellStyle();
      titleStyle.setAlignment(HorizontalAlignment.CENTER);
      titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
      titleStyle.setBorderLeft(BorderStyle.THIN);
      titleStyle.setBorderBottom(BorderStyle.THIN);
      titleStyle.setFont(boldFont);

      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setAlignment(HorizontalAlignment.CENTER);
      headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
      headerStyle.setBorderTop(BorderStyle.THIN);
      headerStyle.setBorderBottom(BorderStyle.THIN);
      headerStyle.setBorderLeft(BorderStyle.THIN);
      headerStyle.setBorderRight(BorderStyle.THIN);
      headerStyle.setFont(boldFont);

      CellStyle dataStyle = workbook.createCellStyle();
      dataStyle.setAlignment(HorizontalAlignment.CENTER);
      dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
      dataStyle.setBorderTop(BorderStyle.THIN);
      dataStyle.setBorderBottom(BorderStyle.THIN);
      dataStyle.setBorderLeft(BorderStyle.THIN);
      dataStyle.setBorderRight(BorderStyle.THIN);

      Row title = sheet.createRow(0);
      Cell titleCell = title.createCell(0);
      titleCell.setCellValue(schoolName + " - Loans Report");
      titleCell.setCellStyle(titleStyle);
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

      Row header = sheet.createRow(1);
      String[] headers = {
          "Loan ID",
          "Book ISBN",
          "Member ID",
          "Issued At",
          "Due At",
          "Returned At",
          "Status",
          "Fine"
      };
      for (int i = 0; i < headers.length; i++) {
        Cell cell = header.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      int rowIndex = 2;
      for (LoanRecord loan : loans) {
        Row row = sheet.createRow(rowIndex++);
        Cell loanId = row.createCell(0);
        loanId.setCellValue(loan.getId());
        loanId.setCellStyle(dataStyle);

        Cell bookIsbn = row.createCell(1);
        bookIsbn.setCellValue(loan.getBookIsbn());
        bookIsbn.setCellStyle(dataStyle);

        Cell memberId = row.createCell(2);
        memberId.setCellValue(loan.getMemberIdentifier());
        memberId.setCellStyle(dataStyle);

        Cell issuedAt = row.createCell(3);
        issuedAt.setCellValue(String.valueOf(loan.getIssuedAt()));
        issuedAt.setCellStyle(dataStyle);

        Cell dueAt = row.createCell(4);
        dueAt.setCellValue(String.valueOf(loan.getDueAt()));
        dueAt.setCellStyle(dataStyle);

        Cell returnedAt = row.createCell(5);
        returnedAt.setCellValue(String.valueOf(loan.getReturnedAt()));
        returnedAt.setCellStyle(dataStyle);

        Cell status = row.createCell(6);
        status.setCellValue(loan.getStatus());
        status.setCellStyle(dataStyle);

        Cell fine = row.createCell(7);
        fine.setCellValue(loan.getFineAmount() == null ? "0" : loan.getFineAmount().toString());
        fine.setCellStyle(dataStyle);
      }

      double[] widths = {
          7.33203125,
          14.109375,
          10.6640625,
          26.33203125,
          26.33203125,
          26.33203125,
          10.109375,
          4.6640625
      };
      for (int i = 0; i < widths.length; i++) {
        sheet.setColumnWidth(i, (int) Math.round(widths[i] * 256));
      }

      try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
        workbook.write(out);
      }
    }
  }

  private float renderHeader(PDDocument document,
                             PDPageContentStream content,
                             float y,
                             String schoolName,
                             String title,
                             String logoPath)
      throws IOException {
    float x = 50;
    if (logoPath != null && !logoPath.isBlank()) {
      java.nio.file.Path path = java.nio.file.Path.of(logoPath);
      if (java.nio.file.Files.exists(path)) {
        try {
          org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject image =
              org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromFileByContent(path.toFile(), document);
          float height = 40;
          float width = 40;
          content.drawImage(image, x, y - height + 10, width, height);
          x += 50;
        } catch (IOException ex) {
          // Ignore logo issues.
        }
      }
    }

    content.setFont(PDType1Font.HELVETICA_BOLD, 16);
    content.beginText();
    content.newLineAtOffset(x, y);
    content.showText(schoolName);
    content.endText();

    content.setFont(PDType1Font.HELVETICA, 12);
    content.beginText();
    content.newLineAtOffset(x, y - 18);
    content.showText(title);
    content.endText();

    return y - 45;
  }

  private float renderTableHeader(PDPageContentStream content, float y, float margin) throws IOException {
    content.setFont(PDType1Font.HELVETICA_BOLD, 11);
    content.beginText();
    content.newLineAtOffset(margin, y);
    content.showText("ISBN");
    content.endText();

    content.beginText();
    content.newLineAtOffset(margin + 100, y);
    content.showText("Title");
    content.endText();

    content.beginText();
    content.newLineAtOffset(margin + 320, y);
    content.showText("Author");
    content.endText();

    content.beginText();
    content.newLineAtOffset(margin + 470, y);
    content.showText("Available");
    content.endText();

    return y - 20;
  }

  private void addFooters(PDDocument document, String footerText) throws IOException {
    int totalPages = document.getNumberOfPages();
    for (int i = 0; i < totalPages; i++) {
      PDPage page = document.getPage(i);
      try (PDPageContentStream content = new PDPageContentStream(document, page,
          PDPageContentStream.AppendMode.APPEND, true)) {
        content.setFont(PDType1Font.HELVETICA, 9);
        content.beginText();
        content.newLineAtOffset(50, 30);
        content.showText(footerText == null || footerText.isBlank() ? "Generated by EduShelf" : footerText);
        content.endText();

        content.beginText();
        content.newLineAtOffset(500, 30);
        content.showText("Page " + (i + 1) + " of " + totalPages);
        content.endText();
      }
    }
  }

  private String safe(String value) {
    return value == null ? "-" : value;
  }

  private String truncate(String value, int max) {
    if (value == null) {
      return "-";
    }
    if (value.length() <= max) {
      return value;
    }
    return value.substring(0, Math.max(0, max - 3)) + "...";
  }
}
