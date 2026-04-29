package com.ecommerce.authservice.authservice;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.authservice.dto.AdvancePaymentExportRequest;
import com.ecommerce.authservice.dto.AdvancePaymentRequest;
import com.ecommerce.authservice.dto.StarredNotePreviewResponse;
import com.ecommerce.authservice.dto.UserNoteRequest;
import com.ecommerce.authservice.dto.WorkerRequest;
import com.ecommerce.authservice.dto.WorkerSummaryResponse;
import com.ecommerce.authservice.entity.AdvancePayment;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.entity.UserNote;
import com.ecommerce.authservice.repository.AdvancePaymentRepository;
import com.ecommerce.authservice.repository.RefreshTokenRepository;
import com.ecommerce.authservice.repository.UserNoteRepository;
import com.ecommerce.authservice.repository.UserRepository;
import com.ecommerce.authservice.security.refreshtoken.RefreshToken;
import com.ecommerce.authservice.util.JwtUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class AuthService {

	private static final Set<String> PAYMENT_MODES = Set.copyOf(Arrays.asList(
			"Paytm",
			"PhonePe",
			"Google Pay",
			"WhatsApp Pay",
			"Bank Transfer",
			"Bank",
			"Cash",
			"Cheque"));
	private static final DateTimeFormatter EXPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
	private static final DateTimeFormatter PAYMENT_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

	@Autowired
	PasswordEncoder passwordEncoder;
	@Autowired
	UserRepository userRepository;
	@Autowired
	JwtUtil jwtUtil;
	@Autowired
	RefreshTokenRepository refreshRepo;
	@Autowired
	AdvancePaymentRepository advancePaymentRepository;
	@Autowired
	UserNoteRepository userNoteRepository;

	public String registerUser(User user) {
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		if(user.getRole()==null || user.getRole().isEmpty()){
			user.setRole("USER");
		}
		if (user.getAdvanceAmount() == null) {
			user.setAdvanceAmount(BigDecimal.ZERO);
		}
		userRepository.save(user);
		return "User registered successfully";
	}
	
	public Map<String, Object> login(User reqUser) {
		User dbUser = userRepository.findByEmail(reqUser.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));
		if(!passwordEncoder.matches(reqUser.getPassword(), dbUser.getPassword())) {
			throw new RuntimeException("Wrong password");
		}
		
		String token = jwtUtil.generateToken(dbUser.getEmail());
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("token", token);
		response.put("id", dbUser.getId());
		response.put("name", dbUser.getName());
		response.put("email", dbUser.getEmail());
		response.put("role", dbUser.getRole());
		response.put("advanceAmount", dbUser.getAdvanceAmount());
		response.put("starredNotes", getStarredNotePreviews(dbUser.getId()));
		return response;
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

	public String deleteById(Long id, String month, String ownerEmail) {
		User user = getOwnedWorker(id, ownerEmail);
		YearMonth selectedMonth = parsePaymentMonth(month);
		String selectedMonthValue = selectedMonth.format(PAYMENT_MONTH_FORMATTER);
		String currentDeletedFromMonth = user.getDeletedFromMonth();

		if (currentDeletedFromMonth == null) {
			user.setDeletedFromMonth(selectedMonthValue);
		} else {
			YearMonth existingDeletedMonth = parsePaymentMonth(currentDeletedFromMonth);
			if (selectedMonth.isBefore(existingDeletedMonth)) {
				user.setDeletedFromMonth(selectedMonthValue);
			}
		}

		userRepository.save(user);
		return "user removed from " + selectedMonthValue + " successfully";
	}
	public List<WorkerSummaryResponse> getAllUser(String month, String ownerEmail) {
		User owner = getAuthenticatedUser(ownerEmail);
		YearMonth selectedMonth = parsePaymentMonth(month);
		String selectedMonthValue = selectedMonth.format(PAYMENT_MONTH_FORMATTER);
		return userRepository.findAll().stream()
				.filter(user -> "WORKER".equalsIgnoreCase(user.getRole()))
				.filter(user -> user.getOwner() != null && owner.getId().equals(user.getOwner().getId()))
				.filter(user -> isWorkerVisibleForMonth(user, selectedMonth))
				.map(user -> {
					BigDecimal monthAdvanceAmount = getMonthlyAdvanceAmount(user.getId(), selectedMonthValue);
					String starredNotePreview = getLatestStarredNotePreview(user.getId());
					return new WorkerSummaryResponse(
							user.getId(),
							user.getName(),
							user.getEmail(),
							user.getRole(),
							monthAdvanceAmount,
							defaultAmount(user.getAdvanceAmount()),
							starredNotePreview,
							starredNotePreview != null);
				})
				.toList();
	}
	
	public User updateUser(Long id, User userDetails, String ownerEmail) {
		User user = getOwnedWorker(id, ownerEmail);
		
		if (userDetails.getName() != null && !userDetails.getName().isEmpty()) {
			user.setName(userDetails.getName());
		}
		if (userDetails.getEmail() != null && !userDetails.getEmail().isEmpty()) {
			user.setEmail(userDetails.getEmail());
		}
		
		return userRepository.save(user);
	}

	public User createWorker(WorkerRequest request, String ownerEmail) {
		if (request.getName() == null || request.getName().isBlank()) {
			throw new RuntimeException("Worker name is required");
		}
		if (request.getAdvanceAmount() != null && request.getAdvanceAmount().signum() > 0) {
			if (request.getPaymentMode() == null || request.getPaymentMode().isBlank()) {
				throw new RuntimeException("Payment mode is required when advance amount is added");
			}
			if (!PAYMENT_MODES.contains(request.getPaymentMode())) {
				throw new RuntimeException("Invalid payment mode");
			}
		}

		String paymentMonth = normalizePaymentMonth(request.getMonth());
		User owner = getAuthenticatedUser(ownerEmail);

		User worker = new User();
		worker.setName(request.getName());
		worker.setRole("WORKER");
		worker.setPassword(passwordEncoder.encode("worker@123"));
		worker.setCreatedAt(parsePaymentMonth(request.getMonth()).atDay(1).atStartOfDay());
		worker.setAdvanceAmount(BigDecimal.ZERO);
		worker.setOwner(owner);
		User savedWorker = userRepository.save(worker);

		if (request.getAdvanceAmount() != null && request.getAdvanceAmount().signum() > 0) {
			AdvancePayment payment = new AdvancePayment();
			payment.setAmount(request.getAdvanceAmount());
			payment.setPaymentMode(request.getPaymentMode());
			payment.setNote(normalizeOptionalText(request.getNote()));
			payment.setPaymentMonth(paymentMonth);
			payment.setPaidAt(request.getPaidAt() == null ? LocalDateTime.now() : request.getPaidAt());
			payment.setUser(savedWorker);
			advancePaymentRepository.save(payment);
			savedWorker.setAdvanceAmount(request.getAdvanceAmount());
			savedWorker = userRepository.save(savedWorker);
		}

		return savedWorker;
	}

	public AdvancePayment addAdvancePayment(Long userId, AdvancePaymentRequest request, String ownerEmail) {
		if (request.getAmount() == null || request.getAmount().signum() <= 0) {
			throw new RuntimeException("Advance amount must be greater than 0");
		}
		if (request.getPaymentMode() == null || request.getPaymentMode().isBlank()) {
			throw new RuntimeException("Payment mode is required");
		}
		if (!PAYMENT_MODES.contains(request.getPaymentMode())) {
			throw new RuntimeException("Invalid payment mode");
		}

		String paymentMonth = normalizePaymentMonth(request.getMonth());
		User user = getOwnedWorker(userId, ownerEmail);

		AdvancePayment payment = new AdvancePayment();
		payment.setAmount(request.getAmount());
		payment.setPaymentMode(request.getPaymentMode());
		payment.setNote(normalizeOptionalText(request.getNote()));
		payment.setPaymentMonth(paymentMonth);
		payment.setPaidAt(request.getPaidAt() == null ? LocalDateTime.now() : request.getPaidAt());
		payment.setUser(user);

		AdvancePayment savedPayment = advancePaymentRepository.save(payment);
		BigDecimal currentAdvanceAmount = user.getAdvanceAmount() == null ? BigDecimal.ZERO : user.getAdvanceAmount();
		user.setAdvanceAmount(currentAdvanceAmount.add(request.getAmount()));
		userRepository.save(user);

		return savedPayment;
	}

	public List<AdvancePayment> getAdvancePaymentHistory(Long userId, String month, String ownerEmail) {
		getOwnedWorker(userId, ownerEmail);
		YearMonth selectedMonth = parsePaymentMonth(month);
		return advancePaymentRepository.findHistoryByUserIdAndMonth(
				userId,
				selectedMonth.format(PAYMENT_MONTH_FORMATTER),
				selectedMonth.atDay(1).atStartOfDay(),
				selectedMonth.plusMonths(1).atDay(1).atStartOfDay());
	}

	public ExportFile exportAdvancePayments(AdvancePaymentExportRequest request, String format, String ownerEmail) {
		List<Long> userIds = request == null ? null : request.getUserIds();
		if (userIds == null || userIds.isEmpty()) {
			throw new RuntimeException("Select at least one user");
		}

		YearMonth selectedMonth = parsePaymentMonth(request.getMonth());
		String selectedMonthValue = selectedMonth.format(PAYMENT_MONTH_FORMATTER);
		List<ExportUserData> exportUsers = buildExportUsers(userIds, selectedMonth, ownerEmail);
		String normalizedFormat = format == null ? "" : format.trim().toLowerCase(Locale.ENGLISH);

		if ("excel".equals(normalizedFormat)) {
			return new ExportFile(buildFileName(selectedMonthValue, "xlsx"),
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
					generateExcelExport(exportUsers, selectedMonthValue));
		}
		if ("pdf".equals(normalizedFormat)) {
			return new ExportFile(buildFileName(selectedMonthValue, "pdf"), "application/pdf", generatePdfExport(exportUsers, selectedMonthValue));
		}

		throw new RuntimeException("Invalid export format");
	}

	public UserNote createUserNote(Long userId, UserNoteRequest request) {
		String noteText = request == null ? null : normalizeRequiredText(request.getNote(), "Note is required");
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

		UserNote userNote = new UserNote();
		userNote.setNote(noteText);
		userNote.setStarred(request != null && Boolean.TRUE.equals(request.getStarred()));
		userNote.setUser(user);
		return userNoteRepository.save(userNote);
	}

	public List<UserNote> getUserNotes(Long userId) {
		if (!userRepository.existsById(userId)) {
			throw new RuntimeException("User not found");
		}
		return userNoteRepository.findByUserIdOrderByCreatedAtDesc(userId);
	}

	public String deleteUserNote(Long userId, Long noteId) {
		if (!userRepository.existsById(userId)) {
			throw new RuntimeException("User not found");
		}
		if (!userNoteRepository.existsById(noteId)) {
			throw new RuntimeException("Note not found");
		}
		if (!userNoteRepository.existsByIdAndUserId(noteId, userId)) {
			throw new RuntimeException("Note does not belong to this user");
		}

		userNoteRepository.deleteById(noteId);
		return "Note deleted successfully";
	}

	public UserNote updateUserNote(Long userId, Long noteId, UserNoteRequest request) {
		String noteText = request == null ? null : normalizeRequiredText(request.getNote(), "Note is required");
		if (!userRepository.existsById(userId)) {
			throw new RuntimeException("User not found");
		}

		UserNote note = userNoteRepository.findByIdAndUserId(noteId, userId)
				.orElseThrow(() -> new RuntimeException("Note not found"));
		note.setNote(noteText);
		if (request != null && request.getStarred() != null) {
			note.setStarred(request.getStarred());
		}
		return userNoteRepository.save(note);
	}

	public UserNote updateUserNoteStar(Long userId, Long noteId, boolean starred) {
		if (!userRepository.existsById(userId)) {
			throw new RuntimeException("User not found");
		}

		UserNote note = userNoteRepository.findByIdAndUserId(noteId, userId)
				.orElseThrow(() -> new RuntimeException("Note not found"));
		note.setStarred(starred);
		return userNoteRepository.save(note);
	}

	private String normalizeOptionalText(String value) {
		if (value == null) {
			return null;
		}
		String trimmedValue = value.trim();
		return trimmedValue.isEmpty() ? null : trimmedValue;
	}

	private String normalizeRequiredText(String value, String errorMessage) {
		String normalizedValue = normalizeOptionalText(value);
		if (normalizedValue == null) {
			throw new RuntimeException(errorMessage);
		}
		return normalizedValue;
	}

	private List<StarredNotePreviewResponse> getStarredNotePreviews(Long userId) {
		return userNoteRepository.findByUserIdAndStarredTrueOrderByCreatedAtDesc(userId).stream()
				.map(note -> new StarredNotePreviewResponse(
						note.getId(),
						userId,
						note.getNote(),
						note.getCreatedAt()))
				.toList();
	}

	private String getLatestStarredNotePreview(Long userId) {
		return userNoteRepository.findByUserIdAndStarredTrueOrderByCreatedAtDesc(userId).stream()
				.map(UserNote::getNote)
				.map(this::buildNotePreview)
				.findFirst()
				.orElse(null);
	}

	private String buildNotePreview(String note) {
		if (note == null) {
			return null;
		}
		String normalizedNote = note.trim().replaceAll("\\s+", " ");
		if (normalizedNote.length() <= 80) {
			return normalizedNote;
		}
		return normalizedNote.substring(0, 77) + "...";
	}

	private List<ExportUserData> buildExportUsers(List<Long> userIds, YearMonth paymentMonth, String ownerEmail) {
		List<ExportUserData> exportUsers = new ArrayList<>();
		String paymentMonthValue = paymentMonth.format(PAYMENT_MONTH_FORMATTER);

		for (Long userId : userIds) {
			User user = getOwnedWorker(userId, ownerEmail);
			List<AdvancePayment> history = getMonthlyHistory(userId, paymentMonth);
			history.sort(Comparator.comparing(AdvancePayment::getPaidAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
			exportUsers.add(new ExportUserData(user, history, paymentMonthValue, getMonthlyAdvanceAmount(userId, paymentMonthValue)));
		}

		return exportUsers;
	}

	private byte[] generateExcelExport(List<ExportUserData> exportUsers, String paymentMonth) {
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Advance Report");
			CellStyle headerStyle = workbook.createCellStyle();
			org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerStyle.setFont(headerFont);
			headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

			int rowIndex = 0;
			for (int userIndex = 0; userIndex < exportUsers.size(); userIndex++) {
				ExportUserData exportUser = exportUsers.get(userIndex);
				Row userRow = sheet.createRow(rowIndex++);
				writeCell(userRow, 0, "User Name", headerStyle);
				writeCell(userRow, 1, exportUser.user().getName(), null);
				writeCell(userRow, 2, "Month", headerStyle);
				writeCell(userRow, 3, exportUser.paymentMonth(), null);

				Row totalRow = sheet.createRow(rowIndex++);
				writeCell(totalRow, 0, "Month Total", headerStyle);
				writeCell(totalRow, 1, formatAmount(exportUser.monthAdvanceAmount()), null);
				writeCell(totalRow, 2, "Lifetime Total", headerStyle);
				writeCell(totalRow, 3, formatAmount(exportUser.user().getAdvanceAmount()), null);

				Row historyHeaderRow = sheet.createRow(rowIndex++);
				writeCell(historyHeaderRow, 0, "History Amount", headerStyle);
				writeCell(historyHeaderRow, 1, "Payment Mode", headerStyle);
				writeCell(historyHeaderRow, 2, "Paid At", headerStyle);
				writeCell(historyHeaderRow, 3, "Note", headerStyle);

				if (exportUser.history().isEmpty()) {
					Row emptyRow = sheet.createRow(rowIndex++);
					writeCell(emptyRow, 3, "No payment history", null);
				} else {
					for (AdvancePayment payment : exportUser.history()) {
						Row paymentRow = sheet.createRow(rowIndex++);
						writeCell(paymentRow, 0, formatAmount(payment.getAmount()), null);
						writeCell(paymentRow, 1, payment.getPaymentMode(), null);
						writeCell(paymentRow, 2, formatDateTime(payment.getPaidAt()), null);
						writeCell(paymentRow, 3, payment.getNote(), null);
					}
				}

				if (userIndex < exportUsers.size() - 1) {
					rowIndex++;
				}
			}

			sheet.setColumnWidth(0, 18 * 256);
			sheet.setColumnWidth(1, 22 * 256);
			sheet.setColumnWidth(2, 24 * 256);
			sheet.setColumnWidth(3, 40 * 256);

			workbook.write(outputStream);
			return outputStream.toByteArray();
		} catch (Exception exception) {
			throw new RuntimeException("Failed to generate Excel export", exception);
		}
	}

	private byte[] generatePdfExport(List<ExportUserData> exportUsers, String paymentMonth) {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Document document = new Document();
			PdfWriter.getInstance(document, outputStream);
			document.open();

			Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
			Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD);

			document.add(new Paragraph("Advance Payment Report", titleFont));
			document.add(new Paragraph("Month: " + paymentMonth));
			document.add(new Paragraph(" "));

			for (ExportUserData exportUser : exportUsers) {
				document.add(new Paragraph(
						exportUser.user().getName()
								+ " | Month Total: " + formatAmount(exportUser.monthAdvanceAmount())
								+ " | Lifetime Total: " + formatAmount(exportUser.user().getAdvanceAmount()),
						sectionFont));
				document.add(new Paragraph(" "));

				PdfPTable table = new PdfPTable(4);
				table.setWidthPercentage(100);
				table.setWidths(new float[] { 2.4f, 2.8f, 3.4f, 5.4f });
				addPdfHeaderCell(table, "History Amount");
				addPdfHeaderCell(table, "Payment Mode");
				addPdfHeaderCell(table, "Paid At");
				addPdfHeaderCell(table, "Note");

				if (exportUser.history().isEmpty()) {
					table.addCell("-");
					table.addCell("-");
					table.addCell("-");
					table.addCell("No payment history");
				} else {
					for (AdvancePayment payment : exportUser.history()) {
						table.addCell(formatAmount(payment.getAmount()));
						table.addCell(defaultString(payment.getPaymentMode()));
						table.addCell(formatDateTime(payment.getPaidAt()));
						table.addCell(defaultString(payment.getNote()));
					}
				}

				document.add(table);
				document.add(new Paragraph(" "));
			}

			document.close();
			return outputStream.toByteArray();
		} catch (Exception exception) {
			throw new RuntimeException("Failed to generate PDF export", exception);
		}
	}

	private void addPdfHeaderCell(PdfPTable table, String value) {
		Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
		PdfPCell cell = new PdfPCell(new Phrase(value, headerFont));
		table.addCell(cell);
	}

	private void writeCell(Row row, int cellIndex, String value, CellStyle style) {
		Cell cell = row.createCell(cellIndex);
		cell.setCellValue(value == null ? "" : value);
		if (style != null) {
			cell.setCellStyle(style);
		}
	}

	private String formatDateTime(LocalDateTime value) {
		return value == null ? "" : value.format(EXPORT_DATE_FORMATTER);
	}

	private String formatAmount(BigDecimal value) {
		BigDecimal normalized = value == null ? BigDecimal.ZERO : value.stripTrailingZeros();
		return normalized.scale() <= 0 ? normalized.toPlainString() : normalized.toPlainString();
	}

	private String defaultString(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}

	private BigDecimal getMonthlyAdvanceAmount(Long userId, String paymentMonth) {
		YearMonth selectedMonth = parsePaymentMonth(paymentMonth);
		return defaultAmount(advancePaymentRepository.sumAmountByUserIdAndMonth(
				userId,
				selectedMonth.format(PAYMENT_MONTH_FORMATTER),
				selectedMonth.atDay(1).atStartOfDay(),
				selectedMonth.plusMonths(1).atDay(1).atStartOfDay()));
	}

	private BigDecimal defaultAmount(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private YearMonth parsePaymentMonth(String month) {
		String monthValue = month == null || month.isBlank()
				? YearMonth.now().format(PAYMENT_MONTH_FORMATTER)
				: month.trim();
		try {
			return YearMonth.parse(monthValue, PAYMENT_MONTH_FORMATTER);
		} catch (DateTimeParseException exception) {
			throw new RuntimeException("Month must be in yyyy-MM format");
		}
	}

	private String normalizePaymentMonth(String month) {
		return parsePaymentMonth(month).format(PAYMENT_MONTH_FORMATTER);
	}

	private List<AdvancePayment> getMonthlyHistory(Long userId, YearMonth selectedMonth) {
		return advancePaymentRepository.findHistoryByUserIdAndMonth(
				userId,
				selectedMonth.format(PAYMENT_MONTH_FORMATTER),
				selectedMonth.atDay(1).atStartOfDay(),
				selectedMonth.plusMonths(1).atDay(1).atStartOfDay());
	}

	private boolean isWorkerVisibleForMonth(User user, YearMonth selectedMonth) {
		if (user == null || user.getId() == null) {
			return false;
		}

		String deletedFromMonth = user.getDeletedFromMonth();
		if (deletedFromMonth != null && !deletedFromMonth.isBlank()) {
			YearMonth deletedFrom = parsePaymentMonth(deletedFromMonth);
			if (!selectedMonth.isBefore(deletedFrom)) {
				return false;
			}
		}

		LocalDateTime selectedMonthEnd = selectedMonth.plusMonths(1).atDay(1).atStartOfDay();
		LocalDateTime createdAt = user.getCreatedAt();
		if (createdAt != null) {
			return createdAt.isBefore(selectedMonthEnd);
		}

		List<AdvancePayment> history = advancePaymentRepository.findByUserIdOrderByPaidAtDesc(user.getId());
		if (!history.isEmpty()) {
			LocalDateTime earliestPaidAt = history.stream()
					.map(AdvancePayment::getPaidAt)
					.filter(java.util.Objects::nonNull)
					.min(LocalDateTime::compareTo)
					.orElse(null);
			if (earliestPaidAt != null) {
				return !earliestPaidAt.isAfter(selectedMonthEnd);
			}
		}

		// Legacy rows without any creation/history proof are hidden for past months
		// to avoid showing workers before they actually existed.
		LocalDateTime currentMonthStart = YearMonth.now().atDay(1).atStartOfDay();
		return !selectedMonthEnd.isBefore(currentMonthStart);
	}

	private String buildFileName(String paymentMonth, String extension) {
		return "advance-report-" + paymentMonth + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")) + "." + extension;
	}

	private User getAuthenticatedUser(String email) {
		return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Authenticated user not found"));
	}

	private User getOwnedWorker(Long workerId, String ownerEmail) {
		User owner = getAuthenticatedUser(ownerEmail);
		return userRepository.findByIdAndOwnerId(workerId, owner.getId())
				.orElseThrow(() -> new RuntimeException("Worker not found"));
	}

	public record ExportFile(String fileName, String contentType, byte[] content) {
	}

	private record ExportUserData(User user, List<AdvancePayment> history, String paymentMonth, BigDecimal monthAdvanceAmount) {
	}
	
}
