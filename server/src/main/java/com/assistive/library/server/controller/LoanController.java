package com.assistive.library.server.controller;

import com.assistive.library.server.dto.IssueLoanRequest;
import com.assistive.library.server.dto.LoanLookupResponse;
import com.assistive.library.server.dto.ReturnLoanRequest;
import com.assistive.library.server.model.Loan;
import com.assistive.library.server.service.LoanService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {
  private final LoanService loanService;

  public LoanController(LoanService loanService) {
    this.loanService = loanService;
  }

  @GetMapping
  public List<Loan> listLoans() {
    return loanService.listAll();
  }

  @GetMapping("/overdue")
  public List<Loan> listOverdue() {
    return loanService.listOverdue();
  }

  @GetMapping("/external/{externalRef}")
  public LoanLookupResponse getByExternalRef(@PathVariable String externalRef) {
    return LoanLookupResponse.from(loanService.getByExternalRef(externalRef));
  }

  @PostMapping("/issue")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
  public Loan issue(@Valid @RequestBody IssueLoanRequest request) {
    return loanService.issue(request);
  }

  @PostMapping("/return")
  @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
  public Loan returnLoan(@Valid @RequestBody ReturnLoanRequest request) {
    return loanService.returnLoan(request);
  }
}
