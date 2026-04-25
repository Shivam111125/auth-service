package com.ecommerce.authservice.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.authservice.entity.AdvancePayment;

public interface AdvancePaymentRepository extends JpaRepository<AdvancePayment, Long> {
	List<AdvancePayment> findByUserIdOrderByPaidAtDesc(Long userId);

	@Query("""
			select ap from AdvancePayment ap
			where ap.user.id = :userId
			and (
				ap.paymentMonth = :paymentMonth
				or (ap.paymentMonth is null and ap.paidAt >= :start and ap.paidAt < :end)
			)
			order by ap.paidAt desc
			""")
	List<AdvancePayment> findHistoryByUserIdAndMonth(
			@Param("userId") Long userId,
			@Param("paymentMonth") String paymentMonth,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);

	@Query("""
			select sum(ap.amount) from AdvancePayment ap
			where ap.user.id = :userId
			and (
				ap.paymentMonth = :paymentMonth
				or (ap.paymentMonth is null and ap.paidAt >= :start and ap.paidAt < :end)
			)
			""")
	BigDecimal sumAmountByUserIdAndMonth(
			@Param("userId") Long userId,
			@Param("paymentMonth") String paymentMonth,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);
}
