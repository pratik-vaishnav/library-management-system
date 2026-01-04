package com.assistive.library.server.dto;

public class SyncItemResult {
  private String clientRef;
  private String entityId;
  private String status;
  private String message;

  public SyncItemResult() {
  }

  public SyncItemResult(String clientRef, String entityId, String status, String message) {
    this.clientRef = clientRef;
    this.entityId = entityId;
    this.status = status;
    this.message = message;
  }

  public String getClientRef() {
    return clientRef;
  }

  public void setClientRef(String clientRef) {
    this.clientRef = clientRef;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
