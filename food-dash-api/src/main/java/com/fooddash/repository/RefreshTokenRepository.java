package com.fooddash.repository;

import com.fooddash.model.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

	void deleteAllByUser_Id(Long userId);
}
