package com.shopsphere.payment.repository;

import com.shopsphere.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByStripePaymentIntentId(String intentId);
    Optional<Payment> findByOrderId(Long orderId);
}
