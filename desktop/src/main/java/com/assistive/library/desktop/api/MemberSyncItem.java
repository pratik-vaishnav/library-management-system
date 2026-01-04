package com.assistive.library.desktop.api;

import java.time.Instant;

public class MemberSyncItem {
  private String clientRef;
  private boolean force;
  private String memberId;
  private String name;
  private String type;
  private String classOrDepartment;
  private String contactDetails;
  private boolean active;
  private Instant updatedAt;

  public String getClientRef() {
    return clientRef;
  }

  public void setClientRef(String clientRef) {
    this.clientRef = clientRef;
  }

  public boolean isForce() {
    return force;
  }

  public void setForce(boolean force) {
    this.force = force;
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
