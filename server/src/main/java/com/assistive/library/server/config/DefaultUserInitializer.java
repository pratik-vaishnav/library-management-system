package com.assistive.library.server.config;

import com.assistive.library.server.model.Role;
import com.assistive.library.server.model.User;
import com.assistive.library.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultUserInitializer implements CommandLineRunner {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final String defaultUsername;
  private final String defaultPassword;

  public DefaultUserInitializer(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${library.security.default-admin.username:admin}") String defaultUsername,
                                @Value("${library.security.default-admin.password:admin123}") String defaultPassword) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.defaultUsername = defaultUsername;
    this.defaultPassword = defaultPassword;
  }

  @Override
  public void run(String... args) {
    userRepository.findByUsername(defaultUsername)
        .orElseGet(() -> {
          User user = new User();
          user.setUsername(defaultUsername);
          user.setPasswordHash(passwordEncoder.encode(defaultPassword));
          user.setRole(Role.ADMIN);
          user.setActive(true);
          return userRepository.save(user);
        });
  }
}
