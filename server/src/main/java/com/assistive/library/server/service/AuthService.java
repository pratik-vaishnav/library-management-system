package com.assistive.library.server.service;

import com.assistive.library.server.dto.LoginRequest;
import com.assistive.library.server.dto.LoginResponse;
import com.assistive.library.server.model.AuthToken;
import com.assistive.library.server.model.User;
import com.assistive.library.server.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final UserRepository userRepository;
  private final AuthTokenService authTokenService;
  private final PasswordEncoder passwordEncoder;
  private final long tokenTtlHours;

  public AuthService(UserRepository userRepository,
                     AuthTokenService authTokenService,
                     PasswordEncoder passwordEncoder,
                     @Value("${library.security.token-ttl-hours:12}") long tokenTtlHours) {
    this.userRepository = userRepository;
    this.authTokenService = authTokenService;
    this.passwordEncoder = passwordEncoder;
    this.tokenTtlHours = tokenTtlHours;
  }

  public LoginResponse login(LoginRequest request) {
    User user = userRepository.findByUsername(request.getUsername().trim())
        .filter(User::isActive)
        .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid username or password");
    }

    AuthToken token = new AuthToken();
    token.setToken(UUID.randomUUID().toString());
    token.setUser(user);
    token.setExpiresAt(Instant.now().plus(tokenTtlHours, ChronoUnit.HOURS));
    authTokenService.save(token);

    return new LoginResponse(token.getToken(), user.getUsername(), user.getRole(), token.getExpiresAt());
  }

  public void logout(String tokenValue) {
    authTokenService.findToken(tokenValue)
        .ifPresent(token -> {
          token.setRevoked(true);
          authTokenService.save(token);
        });
  }
}
