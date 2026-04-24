package com.ecommerce.authservice.authservice;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.repository.UserRepository;
import com.ecommerce.authservice.security.refreshtoken.RefreshToken;
import com.ecommerce.authservice.security.refreshtoken.RefreshTokenRepository;
import com.ecommerce.authservice.util.JwtUtil;

@Service
public class AuthService {

	@Autowired
	PasswordEncoder passwordEncoder;
	@Autowired
	UserRepository userRepository;
	@Autowired
	JwtUtil jwtUtil;
	@Autowired
	RefreshTokenRepository refreshRepo;

	public String registerUser(User user) {
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		if(user.getRole()==null || user.getRole().isEmpty()){
			user.setRole("USER");
		}
		userRepository.save(user);
		return "User registered successfully";
	}
	
	public String login(User reqUser) {
		User dbUser = userRepository.findByEmail(reqUser.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));
		if(!passwordEncoder.matches(reqUser.getPassword(), dbUser.getPassword())) {
			throw new RuntimeException("Wrong password");
		}
		return jwtUtil.generateToken(dbUser.getEmail());
	}
	public String loginRefreshToken(User reqUser) {

	    User dbUser = userRepository.findByEmail(reqUser.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));

	    if (!passwordEncoder.matches(reqUser.getPassword(), dbUser.getPassword())) {
	        throw new RuntimeException("Wrong password");
	    }

	    String refreshToken = jwtUtil.generateRefreshToken(dbUser.getEmail());
	    RefreshToken rt = new RefreshToken();
	    rt.setToken(refreshToken);
	    rt.setExpiryDate(LocalDateTime.now().plusDays(7));
	    refreshRepo.save(rt);
	    return refreshToken;
	}

	public String deleteById(Long id) {
		if(!userRepository.existsById(id)) {
			throw new RuntimeException("User not found");
		}
		userRepository.deleteById(id);
		return "user deleted successfully";
	}
	
}
