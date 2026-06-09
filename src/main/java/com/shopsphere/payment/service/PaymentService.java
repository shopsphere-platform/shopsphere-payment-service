package com.shopsphere.payment.service;

import com.shopsphere.payment.entity.Payment;
import com.shopsphere.payment.event.PaymentEvent;
import com.shopsphere.payment.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service @RequiredArgsConstructor @Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() { Stripe.apiKey = stripeApiKey; }

    public Map<String, String> createPaymentIntent(Long orderId, String orderNumber, BigDecimal amount, String userEmail) {
        try {
            long cents = amount.multiply(BigDecimal.valueOf(100)).longValue();
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(cents).setCurrency("usd")
                    .putMetadata("orderId", String.valueOf(orderId))
                    .putMetadata("orderNumber", orderNumber)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Payment payment = Payment.builder()
                    .orderId(orderId).orderNumber(orderNumber).userEmail(userEmail)
                    .stripePaymentIntentId(intent.getId())
                    .stripeClientSecret(intent.getClientSecret())
                    .amount(amount).build();
            paymentRepo.save(payment);

            return Map.of("clientSecret", intent.getClientSecret(), "paymentIntentId", intent.getId());
        } catch (Exception e) {
            log.error("Stripe error: {}", e.getMessage());
            throw new RuntimeException("Payment failed: " + e.getMessage());
        }
    }

    public void handleWebhookSuccess(String paymentIntentId) {
        paymentRepo.findByStripePaymentIntentId(paymentIntentId).ifPresent(p -> {
            p.setStatus(Payment.PaymentStatus.SUCCEEDED);
            p.setCompletedAt(LocalDateTime.now());
            paymentRepo.save(p);
            kafkaTemplate.send("payment-completed", p.getOrderId().toString(),
                    PaymentEvent.builder().orderId(p.getOrderId()).orderNumber(p.getOrderNumber())
                            .userEmail(p.getUserEmail()).amount(p.getAmount()).status("SUCCEEDED").build());
            log.info("Payment succeeded: {}", paymentIntentId);
        });
    }

    public void handleWebhookFailure(String paymentIntentId, String reason) {
        paymentRepo.findByStripePaymentIntentId(paymentIntentId).ifPresent(p -> {
            p.setStatus(Payment.PaymentStatus.FAILED);
            p.setFailureReason(reason);
            paymentRepo.save(p);
            kafkaTemplate.send("payment-failed", p.getOrderId().toString(),
                    PaymentEvent.builder().orderId(p.getOrderId()).orderNumber(p.getOrderNumber())
                            .userEmail(p.getUserEmail()).amount(p.getAmount())
                            .status("FAILED").failureReason(reason).build());
        });
    }
}
