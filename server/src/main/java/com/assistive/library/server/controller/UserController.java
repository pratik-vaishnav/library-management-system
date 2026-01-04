package com.assistive.library.server.controller;

import com.assistive.library.server.dto.CreateUserRequest;
import com.assistive.library.server.dto.UpdateUserRequest;
import com.assistive.library.server.dto.UserResponse;
import com.assistive.library.server.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<UserResponse> listUsers() {
    return userService.listAll();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
    return userService.create(request);
  }

  @PutMapping("/{userId}")
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequest request) {
    return userService.update(userId, request);
  }
}
