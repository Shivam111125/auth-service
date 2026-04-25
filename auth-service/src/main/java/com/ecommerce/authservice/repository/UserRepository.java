package com.ecommerce.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.authservice.entity.User;

public interface UserRepository extends JpaRepository<User, Long>{
		Optional<User> findByEmail(String email);
		Optional<User> findByIdAndOwnerId(Long id, Long ownerId);
}
