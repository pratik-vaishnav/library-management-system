package com.assistive.library.server.config;

import com.assistive.library.server.model.User;
import com.assistive.library.server.service.AuthTokenService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
  private static final String TOKEN_HEADER = "X-Auth-Token";
  private final AuthTokenService authTokenService;

  public AuthTokenFilter(AuthTokenService authTokenService) {
    this.authTokenService = authTokenService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String token = resolveToken(request);
    if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      Optional<User> user = authTokenService.findUserForToken(token);
      if (user.isPresent()) {
        String role = "ROLE_" + user.get().getRole().name();
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                user.get().getUsername(),
                null,
                List.of(new SimpleGrantedAuthority(role)));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String headerToken = request.getHeader(TOKEN_HEADER);
    if (headerToken != null && !headerToken.isBlank()) {
      return headerToken.trim();
    }

    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring("Bearer ".length()).trim();
    }
    return null;
  }
}
