package com.assistive.library.server.dto;

import com.assistive.library.server.model.Member;
import java.time.Instant;

public class MemberLookupResponse {
  private String memberId;
  private String name;
  private String type;
  private String classOrDepartment;
  private String contactDetails;
  private boolean active;
  private Instant updatedAt;

  public static MemberLookupResponse from(Member member) {
    MemberLookupResponse response = new MemberLookupResponse();
    response.setMemberId(member.getMemberId());
    response.setName(member.getName());
    response.setType(member.getType().name());
    response.setClassOrDepartment(member.getClassOrDepartment());
    response.setContactDetails(member.getContactDetails());
    response.setActive(member.isActive());
    response.setUpdatedAt(member.getUpdatedAt());
    return response;
  }

  public String getMemberId() {
    return memberId;
  }

  public void setMemberId(String memberId) {
    this.memberId = memberId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getClassOrDepartment() {
    return classOrDepartment;
  }

  public void setClassOrDepartment(String classOrDepartment) {
    this.classOrDepartment = classOrDepartment;
  }

  public String getContactDetails() {
    return contactDetails;
  }

  public void setContactDetails(String contactDetails) {
    this.contactDetails = contactDetails;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
