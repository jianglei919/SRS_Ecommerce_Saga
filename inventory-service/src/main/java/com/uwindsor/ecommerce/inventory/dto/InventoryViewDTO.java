package com.uwindsor.ecommerce.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryViewDTO {
    private Long productId;
    private String productName;
    private Integer price;
    private Integer stock;
    private Integer reserved;
    private Integer available;
    private String lastUpdateSource;
    private String lastAction;
    private LocalDateTime lastUpdateAt;
}
