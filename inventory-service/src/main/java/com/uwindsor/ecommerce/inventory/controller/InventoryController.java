package com.uwindsor.ecommerce.inventory.controller;

import com.uwindsor.ecommerce.inventory.entity.Inventory;
import com.uwindsor.ecommerce.inventory.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*") // Allow frontend to call directly if needed
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @GetMapping
    public ResponseEntity<List<Inventory>> getAllInventory() {
        return ResponseEntity.ok(inventoryRepository.findAll());
    }

    @PostMapping("/{productId}/add")
    public ResponseEntity<Inventory> addStock(@PathVariable Long productId, @RequestParam("amount") int amount) {
        Inventory inv = inventoryRepository.findById(productId).orElse(null);
        if (inv != null) {
            inv.setStock(inv.getStock() + amount);
            inventoryRepository.save(inv);
            return ResponseEntity.ok(inv);
        }
        return ResponseEntity.notFound().build();
    }
}

