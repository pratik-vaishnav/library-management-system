package com.assistive.library.server.dto;

import com.assistive.library.server.model.Book;
import java.time.Instant;

public class BookLookupResponse {
  private String isbn;
  private String title;
  private String author;
  private String category;
  private String rackLocation;
  private int totalQuantity;
  private int availableQuantity;
  private boolean enabled;
  private Instant updatedAt;

  public static BookLookupResponse from(Book book) {
    BookLookupResponse response = new BookLookupResponse();
    response.setIsbn(book.getIsbn());
    response.setTitle(book.getTitle());
    response.setAuthor(book.getAuthor());
    response.setCategory(book.getCategory());
    response.setRackLocation(book.getRackLocation());
    response.setTotalQuantity(book.getTotalQuantity());
    response.setAvailableQuantity(book.getAvailableQuantity());
    response.setEnabled(book.isEnabled());
    response.setUpdatedAt(book.getUpdatedAt());
    return response;
  }

  public String getIsbn() {
    return isbn;
  }

  public void setIsbn(String isbn) {
    this.isbn = isbn;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getRackLocation() {
    return rackLocation;
  }

  public void setRackLocation(String rackLocation) {
    this.rackLocation = rackLocation;
  }

  public int getTotalQuantity() {
    return totalQuantity;
  }

  public void setTotalQuantity(int totalQuantity) {
    this.totalQuantity = totalQuantity;
  }

  public int getAvailableQuantity() {
    return availableQuantity;
  }

  public void setAvailableQuantity(int availableQuantity) {
    this.availableQuantity = availableQuantity;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
