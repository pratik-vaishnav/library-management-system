package com.assistive.library.desktop.ai;

import com.assistive.library.desktop.data.BookDao;
import com.assistive.library.desktop.data.LoanDao;
import com.assistive.library.desktop.data.MemberDao;
import com.assistive.library.desktop.data.SettingsDao;
import com.assistive.library.desktop.sync.SyncManager;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class AiSummaryService {
  private final SettingsDao settingsDao;
  private final BookDao bookDao;
  private final MemberDao memberDao;
  private final LoanDao loanDao;
  private final SyncManager syncManager;

  public AiSummaryService(SettingsDao settingsDao,
                          BookDao bookDao,
                          MemberDao memberDao,
                          LoanDao loanDao,
                          SyncManager syncManager) {
    this.settingsDao = settingsDao;
    this.bookDao = bookDao;
    this.memberDao = memberDao;
    this.loanDao = loanDao;
    this.syncManager = syncManager;
  }

  public String generateDailySummary() throws IOException, InterruptedException {
    String baseUrl = getOllamaBaseUrl();
    OllamaClient client = new OllamaClient(baseUrl);
    String model = getOllamaModel();
    if (model == null || model.isBlank()) {
      List<String> models = client.listModels();
      if (models.isEmpty()) {
        throw new IOException("No Ollama models available. Run: ollama pull <model>");
      }
      model = models.get(0);
    }

    Snapshot snapshot = buildSnapshot();
    String prompt = buildPrompt(snapshot);
    String response = client.generate(model, prompt);
    if (response == null || response.isBlank()) {
      return "No summary returned by Ollama.";
    }
    return response.trim();
  }

  private Snapshot buildSnapshot() {
    int totalBooks = bookDao.countAll();
    int totalMembers = memberDao.countAll();
    int issuedToday = loanDao.countIssuedToday();
    int returnedToday = loanDao.countReturnedToday();
    int overdue = loanDao.countOverdue();
    int pendingSync = syncManager.pendingCount();
    List<LoanDao.BookCount> topBooks = loanDao.topBooks(3);
    return new Snapshot(totalBooks, totalMembers, issuedToday, returnedToday, overdue, pendingSync, topBooks);
  }

  private String buildPrompt(Snapshot snapshot) {
    StringBuilder builder = new StringBuilder();
    builder.append("You are an assistant for a school librarian. ");
    builder.append("Write a concise daily summary in 3-5 bullet points. ");
    builder.append("Be friendly and actionable. Avoid technical jargon.\n\n");
    builder.append("Date: ").append(LocalDate.now()).append("\n");
    builder.append("Total books: ").append(snapshot.totalBooks()).append("\n");
    builder.append("Total members: ").append(snapshot.totalMembers()).append("\n");
    builder.append("Issued today: ").append(snapshot.issuedToday()).append("\n");
    builder.append("Returned today: ").append(snapshot.returnedToday()).append("\n");
    builder.append("Overdue: ").append(snapshot.overdue()).append("\n");
    builder.append("Pending sync: ").append(snapshot.pendingSync()).append("\n");
    if (!snapshot.topBooks().isEmpty()) {
      builder.append("Top borrowed books: ");
      for (int i = 0; i < snapshot.topBooks().size(); i++) {
        LoanDao.BookCount book = snapshot.topBooks().get(i);
        if (i > 0) {
          builder.append(", ");
        }
        builder.append(book.title()).append(" (").append(book.total()).append(")");
      }
      builder.append("\n");
    }
    builder.append("\nReturn only the bullets, no extra headings.");
    return builder.toString();
  }

  private String getOllamaBaseUrl() {
    String fromSettings = settingsDao.getValue("ai.ollama.baseUrl");
    if (fromSettings != null && !fromSettings.isBlank()) {
      return fromSettings.trim();
    }
    String fromEnv = System.getenv("OLLAMA_BASE_URL");
    if (fromEnv != null && !fromEnv.isBlank()) {
      return fromEnv.trim();
    }
    return "http://localhost:11434";
  }

  private String getOllamaModel() {
    String fromSettings = settingsDao.getValue("ai.ollama.model");
    if (fromSettings != null && !fromSettings.isBlank()) {
      return fromSettings.trim();
    }
    String fromEnv = System.getenv("OLLAMA_MODEL");
    if (fromEnv != null && !fromEnv.isBlank()) {
      return fromEnv.trim();
    }
    return "";
  }

  private record Snapshot(int totalBooks,
                          int totalMembers,
                          int issuedToday,
                          int returnedToday,
                          int overdue,
                          int pendingSync,
                          List<LoanDao.BookCount> topBooks) {
  }
}
