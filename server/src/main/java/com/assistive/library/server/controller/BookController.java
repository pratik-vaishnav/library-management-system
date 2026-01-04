package com.assistive.library.server.controller;

import com.assistive.library.server.dto.BookLookupResponse;
import com.assistive.library.server.dto.BookRequest;
import com.assistive.library.server.model.Book;
import com.assistive.library.server.service.BookService;
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
@RequestMapping("/api/books")
public class BookController {
  private final BookService bookService;

  public BookController(BookService bookService) {
    this.bookService = bookService;
  }

  @GetMapping
  public List<Book> listBooks() {
    return bookService.listAll();
  }

  @GetMapping("/isbn/{isbn}")
  public BookLookupResponse getByIsbn(@PathVariable String isbn) {
    return BookLookupResponse.from(bookService.getByIsbn(isbn));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
  public Book createBook(@Valid @RequestBody BookRequest request) {
    return bookService.create(request);
  }
}
