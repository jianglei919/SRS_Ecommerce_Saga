package com.uwindsor.ecommerce.inventory.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEventDTO {
    private String sagaId;
    private String orderId;
}
