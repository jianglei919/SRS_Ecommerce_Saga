package com.uwindsor.ecommerce.order.controller;

import com.uwindsor.ecommerce.order.repository.OrderRepository;
import com.uwindsor.ecommerce.order.repository.SagaLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Dashboard Controller - Serves the real-time dashboard for saga monitoring
 * Shows order status, inventory levels, and saga progress
 */
@Slf4j
@Controller
public class DashboardController {

    private final OrderRepository orderRepository;
    private final SagaLogRepository sagaLogRepository;

    public DashboardController(OrderRepository orderRepository,
                             SagaLogRepository sagaLogRepository) {
        this.orderRepository = orderRepository;
        this.sagaLogRepository = sagaLogRepository;
    }

    /**
     * GET /dashboard - Serve the dashboard page
     * Loads all orders and saga logs for display
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.info("Loading dashboard");
        model.addAttribute("orders", orderRepository.findAll());
        model.addAttribute("sagas", sagaLogRepository.findAll());
        return "dashboard";
    }

    /**
     * GET /api/dashboard/data - REST endpoint for dashboard data refresh
     * Used by AJAX calls for real-time updates
     */
    @GetMapping("/api/dashboard/data")
    public org.springframework.http.ResponseEntity<?> getDashboardData() {
        return org.springframework.http.ResponseEntity.ok(new Object() {
            public final Object orders = orderRepository.findAll();
            public final Object sagas = sagaLogRepository.findAll();
        });
    }
}
