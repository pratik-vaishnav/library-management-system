package com.assistive.library.server.service;

import com.assistive.library.server.dto.BookRequest;
import com.assistive.library.server.model.Book;
import com.assistive.library.server.repository.BookRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class BookService {
  private final BookRepository bookRepository;

  public BookService(BookRepository bookRepository) {
    this.bookRepository = bookRepository;
  }

  public List<Book> listAll() {
    return bookRepository.findAll();
  }

  public Book create(BookRequest request) {
    bookRepository.findByIsbn(request.getIsbn())
        .ifPresent(existing -> {
          throw new IllegalArgumentException("ISBN already exists");
        });

    if (request.getTotalQuantity() < 0) {
      throw new IllegalArgumentException("Total quantity must be zero or greater");
    }

    Book book = new Book();
    book.setIsbn(request.getIsbn().trim());
    book.setTitle(request.getTitle().trim());
    book.setAuthor(trimOrNull(request.getAuthor()));
    book.setCategory(trimOrNull(request.getCategory()));
    book.setRackLocation(trimOrNull(request.getRackLocation()));
    book.setTotalQuantity(request.getTotalQuantity());
    book.setAvailableQuantity(request.getTotalQuantity());
    return bookRepository.save(book);
  }

  public Book getByIsbn(String isbn) {
    return bookRepository.findByIsbn(isbn.trim())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
  }

  private String trimOrNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
