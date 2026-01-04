package com.assistive.library.server.service;

import com.assistive.library.server.dto.MemberRequest;
import com.assistive.library.server.model.Member;
import com.assistive.library.server.repository.MemberRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class MemberService {
  private final MemberRepository memberRepository;

  public MemberService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
  }

  public List<Member> listAll() {
    return memberRepository.findAll();
  }

  public Member create(MemberRequest request) {
    memberRepository.findByMemberId(request.getMemberId())
        .ifPresent(existing -> {
          throw new IllegalArgumentException("Member ID already exists");
        });

    Member member = new Member();
    member.setMemberId(request.getMemberId().trim());
    member.setName(request.getName().trim());
    member.setType(request.getType());
    member.setClassOrDepartment(trimOrNull(request.getClassOrDepartment()));
    member.setContactDetails(trimOrNull(request.getContactDetails()));
    return memberRepository.save(member);
  }

  public Member getByMemberId(String memberId) {
    return memberRepository.findByMemberId(memberId.trim())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
  }

  private String trimOrNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
