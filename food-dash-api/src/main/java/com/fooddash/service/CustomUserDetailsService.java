package com.fooddash.service;

import com.fooddash.model.User;
import com.fooddash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = loadDomainUserByEmail(email);
		return toUserDetails(user);
	}

	public User loadDomainUserByEmail(String email) {
		return userRepository.findByEmail(email.toLowerCase())
				.orElseThrow(() -> new UsernameNotFoundException("User not found for email: " + email));
	}

	public UserDetails toUserDetails(User user) {
		return org.springframework.security.core.userdetails.User.builder()
				.username(user.getEmail())
				.password(user.getPasswordHash())
				.roles(user.getRole().name())
				.disabled(!user.isActive())
				.build();
	}
}
