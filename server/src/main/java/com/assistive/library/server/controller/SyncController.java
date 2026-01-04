package com.assistive.library.server.controller;

import com.assistive.library.server.dto.SyncPullResponse;
import com.assistive.library.server.dto.SyncPushRequest;
import com.assistive.library.server.dto.SyncPushResponse;
import com.assistive.library.server.service.SyncService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {
  private final SyncService syncService;

  public SyncController(SyncService syncService) {
    this.syncService = syncService;
  }

  @PostMapping("/push")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
  public SyncPushResponse push(@Valid @RequestBody SyncPushRequest request) {
    return syncService.push(request);
  }

  @GetMapping("/pull")
  public SyncPullResponse pull(@RequestParam(value = "since", required = false)
                               @DateTimeFormat(iso = ISO.DATE_TIME) Instant since) {
    return syncService.pull(since);
  }
}
