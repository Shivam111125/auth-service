package com.ecommerce.authservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.authservice.entity.UserNote;

public interface UserNoteRepository extends JpaRepository<UserNote, Long> {
	List<UserNote> findByUserIdOrderByCreatedAtDesc(Long userId);
	List<UserNote> findByUserIdAndStarredTrueOrderByCreatedAtDesc(Long userId);
	boolean existsByIdAndUserId(Long id, Long userId);
	java.util.Optional<UserNote> findByIdAndUserId(Long id, Long userId);
}
