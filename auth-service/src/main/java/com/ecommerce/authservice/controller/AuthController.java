package com.ecommerce.authservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.authservice.authservice.AuthService;
import com.ecommerce.authservice.dto.AdvancePaymentRequest;
import com.ecommerce.authservice.dto.AdvancePaymentExportRequest;
import com.ecommerce.authservice.dto.UserNoteRequest;
import com.ecommerce.authservice.dto.WorkerRequest;
import com.ecommerce.authservice.dto.WorkerSummaryResponse;
import com.ecommerce.authservice.entity.AdvancePayment;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.entity.UserNote;
import com.ecommerce.authservice.repository.UserRepository;

@RestController
@RequestMapping("/auth")
public class AuthController {

	@Autowired
	AuthService authService;
	@Autowired
	UserRepository userRepository;
	
	@GetMapping("/test")
	public String test() {
	    return "Secure API working after deployemnet 2";
	}
	
	@PostMapping("/signup")
	public String signup(@RequestBody User user) {
		return authService.registerUser(user);
	}
	
	@PostMapping("/login")
	public Object login(@RequestBody User user) {
		return authService.login(user);
	}
	@PostMapping("/loginRefreshToken")
	public String loginRefreshToken(@RequestBody User user) {
		return authService.loginRefreshToken(user);
	}
	
	@DeleteMapping("/delete/{id}")
	public String deleteById(@PathVariable Long id, @RequestParam(required = false) String month,
			Authentication authentication) {
		return authService.deleteById(id, month, authentication.getName());
	}
	
	@GetMapping("/getAllUser")
	public List<WorkerSummaryResponse> getAllUser(@RequestParam(required = false) String month, Authentication authentication) {
		return authService.getAllUser(month, authentication.getName());
	}
	
	@PutMapping("/update/{id}")
	public User updateUser(@PathVariable Long id, @RequestBody User user, Authentication authentication) {
		return authService.updateUser(id, user, authentication.getName());
	}

	@PostMapping("/workers")
	public User createWorker(@RequestBody WorkerRequest request, Authentication authentication) {
		return authService.createWorker(request, authentication.getName());
	}

	@PostMapping("/users/{id}/advance-payments")
	public AdvancePayment addAdvancePayment(@PathVariable Long id, @RequestBody AdvancePaymentRequest request,
			Authentication authentication) {
		return authService.addAdvancePayment(id, request, authentication.getName());
	}

	@GetMapping("/users/{id}/advance-payments")
	public List<AdvancePayment> getAdvancePaymentHistory(@PathVariable Long id,
			@RequestParam(required = false) String month,
			Authentication authentication) {
		return authService.getAdvancePaymentHistory(id, month, authentication.getName());
	}

	@PutMapping("/users/{userId}/advance-payments/{paymentId}")
	public AdvancePayment updateAdvancePayment(@PathVariable Long userId, @PathVariable Long paymentId,
			@RequestBody AdvancePaymentRequest request, Authentication authentication) {
		return authService.updateAdvancePayment(userId, paymentId, request, authentication.getName());
	}

	@PostMapping("/exports/advance-payments")
	public ResponseEntity<byte[]> exportAdvancePayments(@RequestParam String format,
			@RequestBody AdvancePaymentExportRequest request,
			Authentication authentication) {
		AuthService.ExportFile exportFile = authService.exportAdvancePayments(request, format, authentication.getName());
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportFile.fileName() + "\"")
				.header(HttpHeaders.CONTENT_TYPE, exportFile.contentType())
				.body(exportFile.content());
	}

	@PostMapping("/users/{id}/notes")
	public UserNote createUserNote(@PathVariable Long id, @RequestBody UserNoteRequest request) {
		return authService.createUserNote(id, request);
	}

	@GetMapping("/users/{id}/notes")
	public List<UserNote> getUserNotes(@PathVariable Long id) {
		return authService.getUserNotes(id);
	}

	@DeleteMapping("/users/{userId}/notes/{noteId}")
	public String deleteUserNote(@PathVariable Long userId, @PathVariable Long noteId) {
		return authService.deleteUserNote(userId, noteId);
	}

	@PutMapping("/users/{userId}/notes/{noteId}")
	public UserNote updateUserNote(@PathVariable Long userId, @PathVariable Long noteId, @RequestBody UserNoteRequest request) {
		return authService.updateUserNote(userId, noteId, request);
	}

	@PutMapping("/users/{userId}/notes/{noteId}/star")
	public UserNote updateUserNoteStar(@PathVariable Long userId, @PathVariable Long noteId, @RequestParam boolean starred) {
		return authService.updateUserNoteStar(userId, noteId, starred);
	}
	
}
