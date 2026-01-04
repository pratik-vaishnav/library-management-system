package com.assistive.library.server.repository;

import com.assistive.library.server.model.AuthToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
  Optional<AuthToken> findByToken(String token);
}
