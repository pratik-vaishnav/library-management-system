package com.assistive.library.server.controller;

import com.assistive.library.server.dto.MemberLookupResponse;
import com.assistive.library.server.dto.MemberRequest;
import com.assistive.library.server.model.Member;
import com.assistive.library.server.service.MemberService;
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
@RequestMapping("/api/members")
public class MemberController {
  private final MemberService memberService;

  public MemberController(MemberService memberService) {
    this.memberService = memberService;
  }

  @GetMapping
  public List<Member> listMembers() {
    return memberService.listAll();
  }

  @GetMapping("/member-id/{memberId}")
  public MemberLookupResponse getByMemberId(@PathVariable String memberId) {
    return MemberLookupResponse.from(memberService.getByMemberId(memberId));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
  public Member createMember(@Valid @RequestBody MemberRequest request) {
    return memberService.create(request);
  }
}
