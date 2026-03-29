package com.uwindsor.ecommerce.inventory.controller;

import com.uwindsor.ecommerce.inventory.dto.InventoryViewDTO;
import com.uwindsor.ecommerce.inventory.entity.Inventory;
import com.uwindsor.ecommerce.inventory.entity.InventoryLog;
import com.uwindsor.ecommerce.inventory.repository.InventoryLogRepository;
import com.uwindsor.ecommerce.inventory.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*") // Allow frontend to call directly if needed
public class InventoryController {

    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public InventoryController(InventoryRepository inventoryRepository,
            InventoryLogRepository inventoryLogRepository) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryLogRepository = inventoryLogRepository;
    }

    @GetMapping
    public ResponseEntity<List<InventoryViewDTO>> getAllInventory() {
        List<InventoryViewDTO> view = inventoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Inventory::getProductId))
                .map(this::toViewDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(view);
    }

    @PostMapping("/{productId}/add")
    public ResponseEntity<Inventory> addStock(@PathVariable Long productId, @RequestParam("amount") int amount) {
        Inventory inv = inventoryRepository.findById(Objects.requireNonNull(productId)).orElse(null);
        if (inv != null) {
            if (inv.getStock() + amount < inv.getReserved()) {
                return ResponseEntity.badRequest().build();
            }
            inv.setStock(inv.getStock() + amount);
            inventoryRepository.save(inv);
            return ResponseEntity.ok(inv);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{productId}/stock")
    @SuppressWarnings("null")
    public ResponseEntity<?> setStock(@PathVariable Long productId, @RequestParam("value") int value) {
        if (value < 0) {
            return ResponseEntity.badRequest().body("Stock must be >= 0");
        }

        Inventory inv = inventoryRepository.findById(Objects.requireNonNull(productId)).orElse(null);
        if (inv == null) {
            return ResponseEntity.notFound().build();
        }

        if (value < inv.getReserved()) {
            return ResponseEntity.badRequest()
                    .body("Stock cannot be less than reserved quantity: " + inv.getReserved());
        }

        int delta = value - inv.getStock();

        inv.setStock(value);
        inventoryRepository.save(inv);

        InventoryLog manualSetLog = InventoryLog.builder()
                .orderId("MANUAL")
                .productId(inv.getProductId())
                .quantity(Math.abs(delta))
                .action(InventoryLog.InventoryAction.MANUAL_SET)
                .build();
        inventoryLogRepository.save(manualSetLog);

        return ResponseEntity.ok(inv);
    }

    @DeleteMapping("/test-data")
    public ResponseEntity<?> clearTestData() {
        try {
            // Keep product records, but clear runtime reservation state and inventory logs.
            List<Inventory> products = inventoryRepository.findAll();
            for (Inventory product : products) {
                product.setReserved(0);
            }
            inventoryRepository.saveAll(products);
            inventoryLogRepository.deleteAllInBatch();

            return ResponseEntity.ok(Map.of("message", "Inventory test data cleared; products kept"));
        } catch (Exception ex) {
            log.error("Failed to clear inventory test data", ex);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to clear inventory test data"));
        }
    }

    private InventoryViewDTO toViewDto(Inventory inventory) {
        Optional<InventoryLog> latestLog = inventoryLogRepository
                .findTopByProductIdOrderByTimestampDesc(inventory.getProductId());

        String lastAction = latestLog.map(log -> log.getAction().name()).orElse("-");
        String source = latestLog
                .map(log -> log.getAction() == InventoryLog.InventoryAction.MANUAL_SET ? "MANUAL" : "SAGA")
                .orElse("-");

        return InventoryViewDTO.builder()
                .productId(inventory.getProductId())
                .productName(inventory.getProductName())
                .price(inventory.getPrice())
                .stock(inventory.getStock())
                .reserved(inventory.getReserved())
                .available(inventory.getAvailable())
                .lastAction(lastAction)
                .lastUpdateSource(source)
                .lastUpdateAt(latestLog.map(InventoryLog::getTimestamp).orElse(null))
                .build();
    }
}
