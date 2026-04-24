package com.ecommerce.authservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.authservice.authservice.AuthService;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.repository.UserRepository;
import com.ecommerce.authservice.util.JwtUtil;

@RestController
@RequestMapping("/auth")
public class AuthController {

	@Autowired
	AuthService authService;
	@Autowired
	UserRepository userRepository;
	
	@GetMapping("/test")
	public String test() {
	    return "Secure API working after deployemnet 1";
	}
	
	@PostMapping("/signup")
	public String signup(@RequestBody User user) {
		return authService.registerUser(user);
	}
	
	@PostMapping("/login")
	public String login(@RequestBody User user) {
		return authService.login(user);
	}
	@PostMapping("/loginRefreshToken")
	public String loginRefreshToken(@RequestBody User user) {
		return authService.loginRefreshToken(user);
	}
	
	@DeleteMapping("/delete/{id}")
	public String deleteById(@PathVariable Long id) {
		return authService.deleteById(id);
	}
	
}
