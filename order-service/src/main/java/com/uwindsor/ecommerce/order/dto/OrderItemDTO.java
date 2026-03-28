package com.uwindsor.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Order Item DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Long productId;
    private Integer quantity;
}
