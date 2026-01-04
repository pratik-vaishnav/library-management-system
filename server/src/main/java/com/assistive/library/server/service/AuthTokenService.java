package com.assistive.library.server.service;

import com.assistive.library.server.model.AuthToken;
import com.assistive.library.server.model.User;
import com.assistive.library.server.repository.AuthTokenRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {
  private final AuthTokenRepository authTokenRepository;

  public AuthTokenService(AuthTokenRepository authTokenRepository) {
    this.authTokenRepository = authTokenRepository;
  }

  public Optional<User> findUserForToken(String token) {
    return authTokenRepository.findByToken(token)
        .filter(stored -> !stored.isRevoked())
        .filter(stored -> stored.getExpiresAt().isAfter(Instant.now()))
        .map(AuthToken::getUser)
        .filter(User::isActive);
  }

  public Optional<AuthToken> findToken(String token) {
    return authTokenRepository.findByToken(token);
  }

  public AuthToken save(AuthToken token) {
    return authTokenRepository.save(token);
  }
}
