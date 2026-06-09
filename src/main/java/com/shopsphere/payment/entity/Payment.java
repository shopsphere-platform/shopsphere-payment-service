package com.shopsphere.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long   orderId;
    private String orderNumber;
    private String userEmail;

    @Column(unique = true)
    private String stripePaymentIntentId;
    private String stripeClientSecret;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    private String currency = "usd";

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    private String failureReason;

    @Column(updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    public enum PaymentStatus { PENDING, SUCCEEDED, FAILED, REFUNDED }
}
