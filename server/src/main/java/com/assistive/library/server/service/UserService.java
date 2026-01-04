package com.assistive.library.server.service;

import com.assistive.library.server.dto.CreateUserRequest;
import com.assistive.library.server.dto.UpdateUserRequest;
import com.assistive.library.server.dto.UserResponse;
import com.assistive.library.server.model.User;
import com.assistive.library.server.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public List<UserResponse> listAll() {
    return userRepository.findAll().stream()
        .map(user -> new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.isActive()))
        .collect(Collectors.toList());
  }

  @Transactional
  public UserResponse create(CreateUserRequest request) {
    userRepository.findByUsername(request.getUsername().trim())
        .ifPresent(existing -> {
          throw new IllegalArgumentException("Username already exists");
        });

    User user = new User();
    user.setUsername(request.getUsername().trim());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setRole(request.getRole());
    user.setActive(request.isActive());

    user = userRepository.save(user);
    return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.isActive());
  }

  @Transactional
  public UserResponse update(Long userId, UpdateUserRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (request.getRole() != null) {
      user.setRole(request.getRole());
    }
    if (request.getActive() != null) {
      user.setActive(request.getActive());
    }
    if (request.getPassword() != null && !request.getPassword().isBlank()) {
      user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    }

    user = userRepository.save(user);
    return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.isActive());
  }
}
