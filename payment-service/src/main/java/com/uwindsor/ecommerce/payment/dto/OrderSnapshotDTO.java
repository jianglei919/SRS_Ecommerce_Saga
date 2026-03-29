package com.uwindsor.ecommerce.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSnapshotDTO {
    private String orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;
}
