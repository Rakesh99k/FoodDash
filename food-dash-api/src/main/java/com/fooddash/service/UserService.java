package com.fooddash.service;

import com.fooddash.dto.AdminUpdateUserRequest;
import com.fooddash.dto.ChangePasswordRequest;
import com.fooddash.dto.UpdateProfileRequest;
import com.fooddash.dto.UserResponse;
import com.fooddash.exception.ResourceNotFoundException;
import com.fooddash.model.Role;
import com.fooddash.model.User;
import com.fooddash.repository.RefreshTokenRepository;
import com.fooddash.repository.UserRepository;
import com.fooddash.util.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticatedUserService authenticatedUserService;

	@Transactional(readOnly = true)
	public UserResponse getCurrentUserProfile() {
		return UserMapper.toResponse(getCurrentUser());
	}

	@Transactional
	public UserResponse updateCurrentUserProfile(UpdateProfileRequest request) {
		User user = getCurrentUser();
		String normalizedPhone = normalizePhone(request.getPhone());
		validatePhoneUniqueness(normalizedPhone, user.getId());
		user.setFullName(request.getFullName().trim());
		user.setPhone(normalizedPhone);
		return UserMapper.toResponse(userRepository.save(user));
	}

	@Transactional
	public void changeCurrentUserPassword(ChangePasswordRequest request) {
		User user = getCurrentUser();
		if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
			throw new IllegalArgumentException("Old password is incorrect");
		}
		if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
			throw new IllegalArgumentException("New password must be different from old password");
		}
		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);
		refreshTokenRepository.deleteAllByUser_Id(user.getId());
	}

	@Transactional(readOnly = true)
	public Page<UserResponse> listUsers(Role role, Pageable pageable) {
		Page<User> users =
				role == null ? userRepository.findAll(pageable) : userRepository.findAllByRole(role, pageable);
		return users.map(UserMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public UserResponse getUserById(Long id) {
		return UserMapper.toResponse(findById(id));
	}

	@Transactional
	public UserResponse updateUserByAdmin(Long id, AdminUpdateUserRequest request) {
		User user = findById(id);
		String normalizedEmail = request.getEmail().trim().toLowerCase();
		String normalizedPhone = normalizePhone(request.getPhone());

		if (userRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
			throw new IllegalArgumentException("Email already registered");
		}
		validatePhoneUniqueness(normalizedPhone, id);

		user.setEmail(normalizedEmail);
		user.setFullName(request.getFullName().trim());
		user.setPhone(normalizedPhone);
		if (request.getRole() != null) {
			user.setRole(request.getRole());
		}
		if (request.getActive() != null) {
			user.setActive(request.getActive());
		}

		User saved = userRepository.save(user);
		if (!saved.isActive()) {
			refreshTokenRepository.deleteAllByUser_Id(saved.getId());
		}
		return UserMapper.toResponse(saved);
	}

	@Transactional
	public UserResponse deactivateUser(Long id) {
		User user = findById(id);
		user.setActive(false);
		User saved = userRepository.save(user);
		refreshTokenRepository.deleteAllByUser_Id(saved.getId());
		return UserMapper.toResponse(saved);
	}

	private User getCurrentUser() {
		return authenticatedUserService.getCurrentUser();
	}

	private User findById(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
	}

	private void validatePhoneUniqueness(String phone, Long userId) {
		if (phone != null && userRepository.existsByPhoneAndIdNot(phone, userId)) {
			throw new IllegalArgumentException("Phone already registered");
		}
	}

	private String normalizePhone(String phone) {
		if (phone == null) {
			return null;
		}
		String normalized = phone.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
