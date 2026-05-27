package com.fooddash.service;

import com.fooddash.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

	private final CustomUserDetailsService customUserDetailsService;

	public User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AuthorizationDeniedException("Unauthenticated");
		}
		return customUserDetailsService.loadDomainUserByEmail(authentication.getName());
	}
}
