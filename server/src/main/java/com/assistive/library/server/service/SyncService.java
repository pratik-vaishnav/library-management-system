package com.assistive.library.server.service;

import com.assistive.library.server.dto.BookSyncItem;
import com.assistive.library.server.dto.LoanSyncItem;
import com.assistive.library.server.dto.MemberSyncItem;
import com.assistive.library.server.dto.SyncItemResult;
import com.assistive.library.server.dto.SyncPullResponse;
import com.assistive.library.server.dto.SyncPushRequest;
import com.assistive.library.server.dto.SyncPushResponse;
import com.assistive.library.server.model.Book;
import com.assistive.library.server.model.Loan;
import com.assistive.library.server.model.Member;
import com.assistive.library.server.repository.BookRepository;
import com.assistive.library.server.repository.LoanRepository;
import com.assistive.library.server.repository.MemberRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncService {
  private final BookRepository bookRepository;
  private final MemberRepository memberRepository;
  private final LoanRepository loanRepository;
  private final SettingsService settingsService;

  public SyncService(BookRepository bookRepository,
                     MemberRepository memberRepository,
                     LoanRepository loanRepository,
                     SettingsService settingsService) {
    this.bookRepository = bookRepository;
    this.memberRepository = memberRepository;
    this.loanRepository = loanRepository;
    this.settingsService = settingsService;
  }

  @Transactional
  public SyncPushResponse push(SyncPushRequest request) {
    List<SyncItemResult> results = new ArrayList<>();
    int booksAccepted = upsertBooks(nullSafe(request.getBooks()), results);
    int membersAccepted = upsertMembers(nullSafe(request.getMembers()), results);
    int loansAccepted = upsertLoans(request.getDeviceId(), nullSafe(request.getLoans()), results);
    return new SyncPushResponse(booksAccepted, membersAccepted, loansAccepted, results);
  }

  public SyncPullResponse pull(Instant since) {
    List<Book> books = since == null ? bookRepository.findAll() : bookRepository.findByUpdatedAtAfter(since);
    List<Member> members = since == null ? memberRepository.findAll() : memberRepository.findByUpdatedAtAfter(since);
    List<Loan> loans = since == null ? loanRepository.findAll() : loanRepository.findByUpdatedAtAfter(since);
    return new SyncPullResponse(
        Instant.now(),
        books.stream().map(this::toBookSyncItem).toList(),
        members.stream().map(this::toMemberSyncItem).toList(),
        loans.stream().map(this::toLoanSyncItem).toList());
  }

  private int upsertBooks(List<BookSyncItem> items, List<SyncItemResult> results) {
    int accepted = 0;
    for (BookSyncItem item : items) {
      String entityId = item.getIsbn();
      String clientRef = item.getClientRef();
      if (item.getIsbn() == null || item.getTitle() == null) {
        results.add(new SyncItemResult(clientRef, entityId, "REJECTED", "ISBN and title are required"));
        continue;
      }
      if (item.getTotalQuantity() < 0 || item.getAvailableQuantity() < 0
          || item.getAvailableQuantity() > item.getTotalQuantity()) {
        results.add(new SyncItemResult(clientRef, entityId, "REJECTED", "Invalid stock quantities"));
        continue;
      }
      Book book = bookRepository.findByIsbn(item.getIsbn().trim())
          .orElseGet(Book::new);
      if (!item.isForce() && item.getUpdatedAt() == null && book.getId() != null) {
        results.add(new SyncItemResult(clientRef, entityId, "REJECTED", "updatedAt required for existing records"));
        continue;
      }
      if (!item.isForce() && !shouldApply(item.getUpdatedAt(), book.getUpdatedAt())) {
        results.add(new SyncItemResult(clientRef, entityId, "CONFLICT", "Server has newer book record"));
        continue;
      }
      book.setIsbn(item.getIsbn().trim());
      book.setTitle(item.getTitle().trim());
      book.setAuthor(trimOrNull(item.getAuthor()));
      book.setCategory(trimOrNull(item.getCategory()));
      book.setRackLocation(trimOrNull(item.getRackLocation()));
      book.setTotalQuantity(item.getTotalQuantity());
      book.setAvailableQuantity(item.getAvailableQuantity());
      book.setEnabled(item.isEnabled());
      bookRepository.save(book);
      accepted++;
      results.add(new SyncItemResult(clientRef, item.getIsbn(), "ACCEPTED", null));
    }
    return accepted;
  }

  private int upsertMembers(List<MemberSyncItem> items, List<SyncItemResult> results) {
    int accepted = 0;
    for (MemberSyncItem item : items) {
      String entityId = item.getMemberId();
      String clientRef = item.getClientRef();
      if (item.getMemberId() == null || item.getName() == null || item.getType() == null) {
        results.add(new SyncItemResult(clientRef, entityId, "REJECTED", "Member ID, name, and type are required"));
        continue;
      }
      Member member = memberRepository.findByMemberId(item.getMemberId().trim())
          .orElseGet(Member::new);
      if (!item.isForce() && item.getUpdatedAt() == null && member.getId() != null) {
        results.add(new SyncItemResult(clientRef, entityId, "REJECTED", "updatedAt required for existing records"));
        continue;
      }
      if (!item.isForce() && !shouldApply(item.getUpdatedAt(), member.getUpdatedAt())) {
        results.add(new SyncItemResult(clientRef, entityId, "CONFLICT", "Server has newer member record"));
        continue;
      }
      member.setMemberId(item.getMemberId().trim());
      member.setName(item.getName().trim());
      member.setType(item.getType());
      member.setClassOrDepartment(trimOrNull(item.getClassOrDepartment()));
      member.setContactDetails(trimOrNull(item.getContactDetails()));
      member.setActive(item.isActive());
      memberRepository.save(member);
      accepted++;
      results.add(new SyncItemResult(clientRef, item.getMemberId(), "ACCEPTED", null));
    }
    return accepted;
  }

  private int upsertLoans(String deviceId, List<LoanSyncItem> items, List<SyncItemResult> results) {
    int accepted = 0;
    for (LoanSyncItem item : items) {
      String entityId = item.getExternalRef();
      String clientRef = item.getClientRef();
      if (item.getExternalRef() == null || item.getBookIsbn() == null || item.getMemberId() == null) {
        results.add(new SyncItemResult(clientRef, entityId, "REJECTED", "Loan externalRef, book ISBN, and member ID are required"));
        continue;
      }
      Book book = bookRepository.findByIsbn(item.getBookIsbn().trim()).orElse(null);
      Member member = memberRepository.findByMemberId(item.getMemberId().trim()).orElse(null);
      if (book == null || member == null) {
        results.add(new SyncItemResult(clientRef, entityId, "REJECTED", "Book or member not found"));
        continue;
      }
      Loan loan = loanRepository.findByExternalRef(item.getExternalRef().trim())
          .orElseGet(Loan::new);
      if (!item.isForce() && item.getUpdatedAt() == null && loan.getId() != null) {
        results.add(new SyncItemResult(clientRef, entityId, "REJECTED", "updatedAt required for existing records"));
        continue;
      }
      if (!item.isForce() && !shouldApply(item.getUpdatedAt(), loan.getUpdatedAt())) {
        results.add(new SyncItemResult(clientRef, entityId, "CONFLICT", "Server has newer loan record"));
        continue;
      }

      String status = item.getStatus() == null ? "ISSUED" : item.getStatus().name();
      if (item.getFineAmount() != null && item.getFineAmount().signum() < 0) {
        results.add(new SyncItemResult(clientRef, entityId, "REJECTED", "Fine amount cannot be negative"));
        continue;
      }

      if (item.getIssuedAt() != null && item.getDueAt() != null) {
        long allowedDays = settingsService.getDueDays(14);
        long actualDays = java.time.Duration.between(item.getIssuedAt(), item.getDueAt()).toDays();
        if (actualDays < 0) {
          results.add(new SyncItemResult(clientRef, entityId, "REJECTED", "Due date cannot be before issue date"));
          continue;
        }
        if (actualDays > allowedDays) {
          results.add(new SyncItemResult(clientRef, entityId, "REJECTED", "Due date exceeds policy limit"));
          continue;
        }
      }
      int maxActive = settingsService.getMaxActiveLoans(5);
      long activeLoans = loanRepository.countByMember_IdAndStatus(member.getId(), com.assistive.library.server.model.LoanStatus.ISSUED);
      if ("ISSUED".equals(status) && loan.getId() == null && activeLoans >= maxActive) {
        results.add(new SyncItemResult(clientRef, entityId, "REJECTED", "Active loan limit reached"));
        continue;
      }

      loan.setExternalRef(item.getExternalRef().trim());
      loan.setSourceDeviceId(Objects.requireNonNullElse(item.getSourceDeviceId(), deviceId));
      loan.setBook(book);
      loan.setMember(member);

      if (loan.getId() == null) {
        Instant issuedAt = item.getIssuedAt() == null ? Instant.now() : item.getIssuedAt();
        loan.setIssuedAt(issuedAt);
        loan.setDueAt(item.getDueAt() == null ? issuedAt : item.getDueAt());
      } else {
        if (item.getIssuedAt() != null) {
          loan.setIssuedAt(item.getIssuedAt());
        }
        if (item.getDueAt() != null) {
          loan.setDueAt(item.getDueAt());
        }
      }

      if (item.getReturnedAt() != null) {
        loan.setReturnedAt(item.getReturnedAt());
      }
      loan.setStatus(item.getStatus() == null ? com.assistive.library.server.model.LoanStatus.ISSUED : item.getStatus());
      if (item.getFineAmount() != null) {
        loan.setFineAmount(item.getFineAmount());
      }
      loanRepository.save(loan);
      accepted++;
      results.add(new SyncItemResult(clientRef, item.getExternalRef(), "ACCEPTED", null));
    }
    return accepted;
  }

  private boolean shouldApply(Instant incoming, Instant existing) {
    if (incoming == null || existing == null) {
      return true;
    }
    return incoming.isAfter(existing);
  }

  private String trimOrNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private <T> List<T> nullSafe(List<T> items) {
    return items == null ? Collections.emptyList() : items;
  }

  private BookSyncItem toBookSyncItem(Book book) {
    BookSyncItem item = new BookSyncItem();
    item.setIsbn(book.getIsbn());
    item.setTitle(book.getTitle());
    item.setAuthor(book.getAuthor());
    item.setCategory(book.getCategory());
    item.setRackLocation(book.getRackLocation());
    item.setTotalQuantity(book.getTotalQuantity());
    item.setAvailableQuantity(book.getAvailableQuantity());
    item.setEnabled(book.isEnabled());
    item.setUpdatedAt(book.getUpdatedAt());
    return item;
  }

  private MemberSyncItem toMemberSyncItem(Member member) {
    MemberSyncItem item = new MemberSyncItem();
    item.setMemberId(member.getMemberId());
    item.setName(member.getName());
    item.setType(member.getType());
    item.setClassOrDepartment(member.getClassOrDepartment());
    item.setContactDetails(member.getContactDetails());
    item.setActive(member.isActive());
    item.setUpdatedAt(member.getUpdatedAt());
    return item;
  }

  private LoanSyncItem toLoanSyncItem(Loan loan) {
    LoanSyncItem item = new LoanSyncItem();
    item.setExternalRef(loan.getExternalRef());
    item.setSourceDeviceId(loan.getSourceDeviceId());
    if (loan.getBook() != null) {
      item.setBookIsbn(loan.getBook().getIsbn());
    }
    if (loan.getMember() != null) {
      item.setMemberId(loan.getMember().getMemberId());
    }
    item.setIssuedAt(loan.getIssuedAt());
    item.setDueAt(loan.getDueAt());
    item.setReturnedAt(loan.getReturnedAt());
    item.setStatus(loan.getStatus());
    item.setFineAmount(loan.getFineAmount());
    item.setUpdatedAt(loan.getUpdatedAt());
    return item;
  }
}
