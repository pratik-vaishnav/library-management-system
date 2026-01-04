package com.assistive.library.server.repository;

import com.assistive.library.server.model.Loan;
import com.assistive.library.server.model.LoanStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {
  List<Loan> findByStatusAndDueAtBefore(LoanStatus status, Instant before);

  List<Loan> findByUpdatedAtAfter(Instant after);

  Optional<Loan> findByExternalRef(String externalRef);

  long countByMember_IdAndStatus(Long memberId, LoanStatus status);
}
