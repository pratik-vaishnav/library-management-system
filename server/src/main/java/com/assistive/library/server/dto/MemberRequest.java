package com.assistive.library.server.dto;

import com.assistive.library.server.model.MemberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MemberRequest {
  @NotBlank
  private String memberId;

  @NotBlank
  private String name;

  @NotNull
  private MemberType type;

  private String classOrDepartment;
  private String contactDetails;

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

  public MemberType getType() {
    return type;
  }

  public void setType(MemberType type) {
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
}
