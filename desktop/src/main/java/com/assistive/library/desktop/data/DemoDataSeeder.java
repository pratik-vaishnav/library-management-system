package com.assistive.library.desktop.data;

import com.assistive.library.desktop.model.BookRecord;
import com.assistive.library.desktop.model.MemberRecord;
import com.assistive.library.desktop.sync.SyncManager;

public class DemoDataSeeder {
  private final BookDao bookDao;
  private final MemberDao memberDao;
  private final SyncManager syncManager;
  private final SettingsDao settingsDao;

  public DemoDataSeeder(BookDao bookDao,
                        MemberDao memberDao,
                        SyncManager syncManager,
                        SettingsDao settingsDao) {
    this.bookDao = bookDao;
    this.memberDao = memberDao;
    this.syncManager = syncManager;
    this.settingsDao = settingsDao;
  }

  public SeedResult seed() {
    if ("true".equalsIgnoreCase(settingsDao.getValue("demo.seeded"))) {
      return new SeedResult(0, 0, "Demo data already seeded.");
    }

    int booksAdded = 0;
    int membersAdded = 0;

    for (BookSeed seed : BookSeed.samples()) {
      if (bookDao.findByIsbn(seed.isbn()) != null) {
        continue;
      }
      BookRecord record = new BookRecord();
      record.setIsbn(seed.isbn());
      record.setTitle(seed.title());
      record.setAuthor(seed.author());
      record.setCategory(seed.category());
      record.setRackLocation(seed.rack());
      record.setTotalQuantity(seed.total());
      record.setAvailableQuantity(seed.total());
      record.setEnabled(true);
      BookRecord inserted = bookDao.insert(record);
      syncManager.enqueueBook(inserted);
      booksAdded++;
    }

    for (MemberSeed seed : MemberSeed.samples()) {
      if (memberDao.findByMemberId(seed.memberId()) != null) {
        continue;
      }
      MemberRecord record = new MemberRecord();
      record.setMemberId(seed.memberId());
      record.setName(seed.name());
      record.setType(seed.type());
      record.setClassOrDepartment(seed.classOrDept());
      record.setContactDetails(seed.contact());
      record.setActive(true);
      MemberRecord inserted = memberDao.insert(record);
      syncManager.enqueueMember(inserted);
      membersAdded++;
    }

    settingsDao.setValue("demo.seeded", "true");
    return new SeedResult(booksAdded, membersAdded, "Demo data seeded successfully.");
  }

  public static class SeedResult {
    private final int booksAdded;
    private final int membersAdded;
    private final String message;

    public SeedResult(int booksAdded, int membersAdded, String message) {
      this.booksAdded = booksAdded;
      this.membersAdded = membersAdded;
      this.message = message;
    }

    public int getBooksAdded() {
      return booksAdded;
    }

    public int getMembersAdded() {
      return membersAdded;
    }

    public String getMessage() {
      return message;
    }
  }

  private record BookSeed(String isbn, String title, String author, String category, String rack, int total) {
    static BookSeed[] samples() {
      return new BookSeed[] {
          new BookSeed("9780000000001", "Mathematics Basics", "R. Sharma", "Mathematics", "A1", 5),
          new BookSeed("9780000000002", "Science Explorer", "K. Mehta", "Science", "A2", 4),
          new BookSeed("9780000000003", "English Grammar Guide", "S. Patel", "Language", "B1", 6),
          new BookSeed("9780000000004", "World History Primer", "N. Gupta", "History", "B2", 3),
          new BookSeed("9780000000005", "Computer Fundamentals", "A. Singh", "Technology", "C1", 5),
          new BookSeed("9780000000006", "Art and Craft Ideas", "M. Desai", "Arts", "C2", 2)
      };
    }
  }

  private record MemberSeed(String memberId, String name, String type, String classOrDept, String contact) {
    static MemberSeed[] samples() {
      return new MemberSeed[] {
          new MemberSeed("STU-001", "Aarav Patel", "STUDENT", "Class 6-A", "9990001111"),
          new MemberSeed("STU-002", "Diya Shah", "STUDENT", "Class 7-B", "9990002222"),
          new MemberSeed("STU-003", "Ishaan Mehta", "STUDENT", "Class 8-A", "9990003333"),
          new MemberSeed("STU-004", "Anaya Joshi", "STUDENT", "Class 9-C", "9990004444"),
          new MemberSeed("TEA-001", "Mrs. Kavita Rao", "TEACHER", "Science Dept", "9990005555"),
          new MemberSeed("TEA-002", "Mr. Rajiv Kumar", "TEACHER", "Math Dept", "9990006666")
      };
    }
  }
}
