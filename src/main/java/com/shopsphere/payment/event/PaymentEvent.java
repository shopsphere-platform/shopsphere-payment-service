package com.shopsphere.payment.event;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentEvent {
    private Long       orderId;
    private String     orderNumber;
    private String     userEmail;
    private BigDecimal amount;
    private String     status;
    private String     failureReason;
}
