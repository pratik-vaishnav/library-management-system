package com.assistive.library.server.repository;

import com.assistive.library.server.model.Book;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
  Optional<Book> findByIsbn(String isbn);

  List<Book> findByUpdatedAtAfter(Instant after);
}
