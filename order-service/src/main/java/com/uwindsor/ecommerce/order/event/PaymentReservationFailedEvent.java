package com.uwindsor.ecommerce.order.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReservationFailedEvent {
    private String sagaId;
    private String orderId;
    private Boolean success;
    private String reason;
}
