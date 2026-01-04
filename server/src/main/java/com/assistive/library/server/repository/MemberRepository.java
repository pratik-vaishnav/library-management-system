package com.assistive.library.server.repository;

import com.assistive.library.server.model.Member;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
  Optional<Member> findByMemberId(String memberId);

  List<Member> findByUpdatedAtAfter(Instant after);
}
