package com.uwindsor.ecommerce.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * PaymentReservationFailedEvent - Published on payment reservation failure
 * Sent back to Order Service to trigger compensation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReservationFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sagaId;
    private String orderId;
    private Boolean success = false;
    private String reason;
}
