package com.assistive.library.desktop.sync;

import com.assistive.library.desktop.api.BookSyncItem;
import com.assistive.library.desktop.api.LoanSyncItem;
import com.assistive.library.desktop.api.MemberSyncItem;
import com.assistive.library.desktop.api.PolicyApi;
import com.assistive.library.desktop.api.SyncApi;
import com.assistive.library.desktop.api.SyncPullResponse;
import com.assistive.library.desktop.api.SyncPushRequest;
import com.assistive.library.desktop.api.SyncPushResponse;
import com.assistive.library.desktop.data.BookDao;
import com.assistive.library.desktop.data.LoanDao;
import com.assistive.library.desktop.data.MemberDao;
import com.assistive.library.desktop.data.SettingsDao;
import com.assistive.library.desktop.data.SyncQueueDao;
import com.assistive.library.desktop.model.BookRecord;
import com.assistive.library.desktop.model.MemberRecord;
import com.assistive.library.desktop.model.SyncQueueItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SyncManager {
  public static final String ENTITY_BOOK = "BOOK";
  public static final String ENTITY_MEMBER = "MEMBER";
  public static final String ENTITY_LOAN = "LOAN";
  public static final String DEVICE_ID_KEY = "device_id";
  public static final String LAST_SYNC_KEY = "last_sync";

  private final SyncApi syncApi;
  private final PolicyApi policyApi;
  private final SyncQueueDao syncQueueDao;
  private final BookDao bookDao;
  private final MemberDao memberDao;
  private final LoanDao loanDao;
  private final SettingsDao settingsDao;
  private final ObjectMapper mapper;

  public SyncManager(SyncApi syncApi,
                     PolicyApi policyApi,
                     SyncQueueDao syncQueueDao,
                     BookDao bookDao,
                     MemberDao memberDao,
                     LoanDao loanDao,
                     SettingsDao settingsDao) {
    this.syncApi = syncApi;
    this.policyApi = policyApi;
    this.syncQueueDao = syncQueueDao;
    this.bookDao = bookDao;
    this.memberDao = memberDao;
    this.loanDao = loanDao;
    this.settingsDao = settingsDao;
    this.mapper = new ObjectMapper();
    this.mapper.registerModule(new JavaTimeModule());
  }

  public String ensureDeviceId() {
    String deviceId = settingsDao.getValue(DEVICE_ID_KEY);
    if (deviceId == null || deviceId.isBlank()) {
      deviceId = UUID.randomUUID().toString();
      settingsDao.setValue(DEVICE_ID_KEY, deviceId);
    }
    return deviceId;
  }

  public int pendingCount() {
    return syncQueueDao.countPending();
  }

  public void sync() throws IOException, InterruptedException {
    pushPending();
    pullUpdates();
    try {
      cachePolicy();
    } catch (IOException | InterruptedException ex) {
      // Ignore policy fetch failures; sync results still apply.
    }
    settingsDao.setValue(LAST_SYNC_KEY, Instant.now().toString());
  }

  public void enqueueBook(BookRecord record) {
    String clientRef = UUID.randomUUID().toString();
    BookSyncItem item = new BookSyncItem();
    item.setClientRef(clientRef);
    item.setIsbn(record.getIsbn());
    item.setTitle(record.getTitle());
    item.setAuthor(record.getAuthor());
    item.setCategory(record.getCategory());
    item.setRackLocation(record.getRackLocation());
    item.setTotalQuantity(record.getTotalQuantity());
    item.setAvailableQuantity(record.getAvailableQuantity());
    item.setEnabled(record.isEnabled());
    item.setUpdatedAt(record.getUpdatedAt());
    enqueue(clientRef, ENTITY_BOOK, record.getIsbn(), "UPSERT", item);
  }

  public void enqueueMember(MemberRecord record) {
    String clientRef = UUID.randomUUID().toString();
    MemberSyncItem item = new MemberSyncItem();
    item.setClientRef(clientRef);
    item.setMemberId(record.getMemberId());
    item.setName(record.getName());
    item.setType(record.getType());
    item.setClassOrDepartment(record.getClassOrDepartment());
    item.setContactDetails(record.getContactDetails());
    item.setActive(record.isActive());
    item.setUpdatedAt(record.getUpdatedAt());
    enqueue(clientRef, ENTITY_MEMBER, record.getMemberId(), "UPSERT", item);
  }

  public void enqueueLoan(String externalRef,
                          String deviceId,
                          String bookIsbn,
                          String memberId,
                          Instant issuedAt,
                          Instant dueAt,
                          Instant returnedAt,
                          String status,
                          BigDecimal fineAmount,
                          Instant updatedAt) {
    String clientRef = UUID.randomUUID().toString();
    LoanSyncItem item = new LoanSyncItem();
    item.setClientRef(clientRef);
    item.setExternalRef(externalRef);
    item.setSourceDeviceId(deviceId);
    item.setBookIsbn(bookIsbn);
    item.setMemberId(memberId);
    item.setIssuedAt(issuedAt);
    item.setDueAt(dueAt);
    item.setReturnedAt(returnedAt);
    item.setStatus(status);
    item.setFineAmount(fineAmount);
    item.setUpdatedAt(updatedAt);
    enqueue(clientRef, ENTITY_LOAN, externalRef, "UPSERT", item);
  }

  private void enqueue(String clientRef, String entityType, String entityId, String action, Object payload) {
    try {
      String json = mapper.writeValueAsString(payload);
      syncQueueDao.enqueue(clientRef, entityType, entityId, action, json);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to enqueue sync payload", ex);
    }
  }

  private void pushPending() throws IOException, InterruptedException {
    List<SyncQueueItem> pending = syncQueueDao.listPending();
    if (pending.isEmpty()) {
      return;
    }

    List<BookSyncItem> books = new ArrayList<>();
    List<MemberSyncItem> members = new ArrayList<>();
    List<LoanSyncItem> loans = new ArrayList<>();
    List<String> clientRefs = new ArrayList<>();

    for (SyncQueueItem item : pending) {
      String clientRef = item.getClientRef();
      if (clientRef == null || clientRef.isBlank()) {
        clientRef = UUID.randomUUID().toString();
        syncQueueDao.updateClientRef(item.getId(), clientRef);
      }
      clientRefs.add(clientRef);
      switch (item.getEntityType()) {
        case ENTITY_BOOK -> {
          BookSyncItem payload = mapper.readValue(item.getPayload(), BookSyncItem.class);
          payload.setClientRef(clientRef);
          books.add(payload);
        }
        case ENTITY_MEMBER -> {
          MemberSyncItem payload = mapper.readValue(item.getPayload(), MemberSyncItem.class);
          payload.setClientRef(clientRef);
          members.add(payload);
        }
        case ENTITY_LOAN -> {
          LoanSyncItem payload = mapper.readValue(item.getPayload(), LoanSyncItem.class);
          payload.setClientRef(clientRef);
          loans.add(payload);
        }
        default -> {
        }
      }
    }

    SyncPushRequest request = new SyncPushRequest();
    request.setDeviceId(ensureDeviceId());
    request.setBooks(books);
    request.setMembers(members);
    request.setLoans(loans);

    SyncPushResponse response = syncApi.push(request);
    if (response == null) {
      return;
    }

    List<com.assistive.library.desktop.api.SyncItemResult> results = response.getResults();
    if (results == null || results.isEmpty()) {
      syncQueueDao.markSyncedByClientRefs(clientRefs);
      return;
    }

    List<String> accepted = new ArrayList<>();
    List<String> handled = new ArrayList<>();
    for (com.assistive.library.desktop.api.SyncItemResult result : results) {
      if (result.getClientRef() == null) {
        continue;
      }
      handled.add(result.getClientRef());
      String status = result.getStatus() == null ? "FAILED" : result.getStatus();
      if ("ACCEPTED".equalsIgnoreCase(status)) {
        accepted.add(result.getClientRef());
      } else if ("CONFLICT".equalsIgnoreCase(status)) {
        syncQueueDao.markStatusByClientRef(result.getClientRef(), "CONFLICT",
            result.getMessage() == null ? "Conflict detected" : result.getMessage());
      } else {
        syncQueueDao.markStatusByClientRef(result.getClientRef(), "FAILED",
            result.getMessage() == null ? "Rejected by server" : result.getMessage());
      }
    }

    syncQueueDao.markSyncedByClientRefs(accepted);
    for (String ref : clientRefs) {
      if (!handled.contains(ref)) {
        syncQueueDao.markStatusByClientRef(ref, "FAILED", "No status returned by server");
      }
    }
  }

  private void pullUpdates() throws IOException, InterruptedException {
    SyncPullResponse response = syncApi.pull();
    if (response == null) {
      return;
    }

    if (response.getBooks() != null) {
      for (BookSyncItem item : response.getBooks()) {
        BookRecord record = new BookRecord();
        record.setIsbn(item.getIsbn());
        record.setTitle(item.getTitle());
        record.setAuthor(item.getAuthor());
        record.setCategory(item.getCategory());
        record.setRackLocation(item.getRackLocation());
        record.setTotalQuantity(item.getTotalQuantity());
        record.setAvailableQuantity(item.getAvailableQuantity());
        record.setEnabled(item.isEnabled());
        bookDao.upsert(record);
      }
    }

    if (response.getMembers() != null) {
      for (MemberSyncItem item : response.getMembers()) {
        MemberRecord record = new MemberRecord();
        record.setMemberId(item.getMemberId());
        record.setName(item.getName());
        record.setType(item.getType());
        record.setClassOrDepartment(item.getClassOrDepartment());
        record.setContactDetails(item.getContactDetails());
        record.setActive(item.isActive());
        memberDao.upsert(record);
      }
    }

    if (response.getLoans() != null) {
      for (LoanSyncItem item : response.getLoans()) {
        if (item.getBookIsbn() == null || item.getMemberId() == null) {
          continue;
        }
        BookRecord book = bookDao.findByIsbn(item.getBookIsbn());
        MemberRecord member = memberDao.findByMemberId(item.getMemberId());
        if (book == null || member == null) {
          continue;
        }
        String externalRef = item.getExternalRef() == null ? UUID.randomUUID().toString() : item.getExternalRef();
        String deviceId = item.getSourceDeviceId() == null ? ensureDeviceId() : item.getSourceDeviceId();
        String status = item.getStatus() == null ? "ISSUED" : item.getStatus();
        loanDao.upsertFromSync(book.getId(), member.getId(), externalRef, deviceId,
            item.getIssuedAt(), item.getDueAt(), item.getReturnedAt(), status, item.getFineAmount());
      }
    }
  }

  private void cachePolicy() throws IOException, InterruptedException {
    if (policyApi == null) {
      return;
    }
    com.assistive.library.desktop.api.PolicyResponse policy = policyApi.getPolicy();
    if (policy == null) {
      return;
    }
    settingsDao.setValue("loan.dueDays", String.valueOf(policy.getDueDays()));
    settingsDao.setValue("loan.finePerDay", String.valueOf(policy.getFinePerDay()));
    settingsDao.setValue("loan.maxActive", String.valueOf(policy.getMaxActiveLoans()));
  }
}
