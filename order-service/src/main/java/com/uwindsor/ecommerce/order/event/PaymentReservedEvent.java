package com.uwindsor.ecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReservedEvent {
    private String sagaId;
    private String orderId;
    private Boolean success;
}
