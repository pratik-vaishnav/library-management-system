package com.assistive.library.server.service;

import com.assistive.library.server.dto.IssueLoanRequest;
import com.assistive.library.server.dto.ReturnLoanRequest;
import com.assistive.library.server.model.Book;
import com.assistive.library.server.model.Loan;
import com.assistive.library.server.model.LoanStatus;
import com.assistive.library.server.model.Member;
import com.assistive.library.server.repository.BookRepository;
import com.assistive.library.server.repository.LoanRepository;
import com.assistive.library.server.repository.MemberRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class LoanService {
  private final BookRepository bookRepository;
  private final MemberRepository memberRepository;
  private final LoanRepository loanRepository;
  private final SettingsService settingsService;
  private final int defaultDueDays;
  private final BigDecimal defaultFinePerDay;
  private final int defaultMaxActive;

  public LoanService(BookRepository bookRepository,
                     MemberRepository memberRepository,
                     LoanRepository loanRepository,
                     SettingsService settingsService,
                     @Value("${library.loans.due-days:14}") int dueDays,
                     @Value("${library.loans.fine-per-day:1.0}") BigDecimal finePerDay,
                     @Value("${library.loans.max-active:5}") int maxActive) {
    this.bookRepository = bookRepository;
    this.memberRepository = memberRepository;
    this.loanRepository = loanRepository;
    this.settingsService = settingsService;
    this.defaultDueDays = dueDays;
    this.defaultFinePerDay = finePerDay;
    this.defaultMaxActive = maxActive;
  }

  public List<Loan> listAll() {
    return loanRepository.findAll();
  }

  public List<Loan> listOverdue() {
    return loanRepository.findByStatusAndDueAtBefore(LoanStatus.ISSUED, Instant.now());
  }

  public Loan getByExternalRef(String externalRef) {
    return loanRepository.findByExternalRef(externalRef)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
  }

  @Transactional
  public Loan issue(IssueLoanRequest request) {
    Book book = bookRepository.findById(request.getBookId())
        .orElseThrow(() -> new IllegalArgumentException("Book not found"));
    Member member = memberRepository.findById(request.getMemberId())
        .orElseThrow(() -> new IllegalArgumentException("Member not found"));

    if (!book.isEnabled() || book.getAvailableQuantity() <= 0) {
      throw new IllegalArgumentException("Book is not available");
    }

    if (!member.isActive()) {
      throw new IllegalArgumentException("Member is not active");
    }

    int maxActive = settingsService.getMaxActiveLoans(defaultMaxActive);
    long activeLoans = loanRepository.countByMember_IdAndStatus(member.getId(), LoanStatus.ISSUED);
    if (activeLoans >= maxActive) {
      throw new IllegalArgumentException("Member has reached the active loan limit");
    }

    Instant issuedAt = Instant.now();
    int dueDays = settingsService.getDueDays(defaultDueDays);
    Instant dueAt = issuedAt.plus(dueDays, ChronoUnit.DAYS);

    Loan loan = new Loan();
    loan.setBook(book);
    loan.setMember(member);
    loan.setIssuedAt(issuedAt);
    loan.setDueAt(dueAt);
    loan.setStatus(LoanStatus.ISSUED);
    loan.setFineAmount(BigDecimal.ZERO);

    book.setAvailableQuantity(book.getAvailableQuantity() - 1);
    bookRepository.save(book);

    return loanRepository.save(loan);
  }

  @Transactional
  public Loan returnLoan(ReturnLoanRequest request) {
    Loan loan = loanRepository.findById(request.getLoanId())
        .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

    if (loan.getStatus() == LoanStatus.RETURNED) {
      throw new IllegalArgumentException("Loan already returned");
    }

    Instant returnedAt = Instant.now();
    loan.setReturnedAt(returnedAt);
    loan.setStatus(LoanStatus.RETURNED);

    long lateDays = ChronoUnit.DAYS.between(loan.getDueAt(), returnedAt);
    if (lateDays > 0) {
      BigDecimal finePerDay = settingsService.getFinePerDay(defaultFinePerDay);
      loan.setFineAmount(finePerDay.multiply(BigDecimal.valueOf(lateDays)));
    } else {
      loan.setFineAmount(BigDecimal.ZERO);
    }

    Book book = loan.getBook();
    int updatedAvailability = Math.min(book.getAvailableQuantity() + 1, book.getTotalQuantity());
    book.setAvailableQuantity(updatedAvailability);
    bookRepository.save(book);

    return loanRepository.save(loan);
  }
}
