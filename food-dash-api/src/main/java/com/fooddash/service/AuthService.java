package com.fooddash.service;

import com.fooddash.dto.LoginRequest;
import com.fooddash.dto.LoginResponse;
import com.fooddash.dto.RegisterRequest;
import com.fooddash.dto.UserResponse;
import com.fooddash.model.RefreshToken;
import com.fooddash.model.Role;
import com.fooddash.model.User;
import com.fooddash.repository.RefreshTokenRepository;
import com.fooddash.repository.UserRepository;
import com.fooddash.util.JwtService;
import com.fooddash.util.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AuthenticationManager authenticationManager;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final CustomUserDetailsService customUserDetailsService;

	@Transactional
	public UserResponse register(RegisterRequest request) {
		String normalizedEmail = request.getEmail().trim().toLowerCase();
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new IllegalArgumentException("Email already registered");
		}

		Role role = request.getRole() == null ? Role.CUSTOMER : request.getRole();
		User user = User.builder()
				.email(normalizedEmail)
				.passwordHash(passwordEncoder.encode(request.getPassword()))
				.fullName(request.getFullName().trim())
				.phone(request.getPhone())
				.role(role)
				.isActive(true)
				.build();

		User saved = userRepository.save(user);
		return UserMapper.toResponse(saved);
	}

	@Transactional
	public LoginResponse login(LoginRequest request) {
		String normalizedEmail = request.getEmail().trim().toLowerCase();
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));

		User user = customUserDetailsService.loadDomainUserByEmail(normalizedEmail);
		String accessToken = jwtService.generateAccessToken(user);
		String refreshToken = jwtService.generateRefreshToken(user);

		refreshTokenRepository.deleteAllByUser_Id(user.getId());
		refreshTokenRepository.save(RefreshToken.builder()
				.user(user)
				.token(refreshToken)
				.expiresAt(jwtService.extractExpiration(refreshToken))
				.revoked(false)
				.build());

		return LoginResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.user(UserMapper.toResponse(user))
				.build();
	}
}
