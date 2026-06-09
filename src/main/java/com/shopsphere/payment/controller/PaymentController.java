package com.shopsphere.payment.controller;

import com.shopsphere.payment.service.PaymentService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/create-intent")
    public ResponseEntity<Map<String, String>> createIntent(@RequestBody Map<String, Object> body,
                                                             @RequestHeader("X-User-Email") String userEmail) {
        Long       orderId     = Long.valueOf(body.get("orderId").toString());
        String     orderNumber = body.get("orderNumber").toString();
        BigDecimal amount      = new BigDecimal(body.get("amount").toString());
        return ResponseEntity.ok(paymentService.createPaymentIntent(orderId, orderNumber, amount, userEmail));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload,
                                           @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            switch (event.getType()) {
                case "payment_intent.succeeded" -> {
                    PaymentIntent pi = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElseThrow();
                    paymentService.handleWebhookSuccess(pi.getId());
                }
                case "payment_intent.payment_failed" -> {
                    PaymentIntent pi = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElseThrow();
                    String reason = pi.getLastPaymentError() != null ? pi.getLastPaymentError().getMessage() : "Unknown";
                    paymentService.handleWebhookFailure(pi.getId(), reason);
                }
                default -> log.debug("Unhandled event: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Webhook error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Webhook error");
        }
        return ResponseEntity.ok("received");
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "payment-service is running"));
    }
}
