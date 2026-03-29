package com.uwindsor.ecommerce.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * PaymentReservedEvent - Published on successful payment reservation
 * Sent back to Order Service to confirm saga progress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReservedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sagaId;
    private String orderId;
    private Boolean success = true;
    private List<?> reservedItems;
}
