package com.interviewiq.interviewstarter.controller;

import com.interviewiq.interviewstarter.dto.DashboardResponse;
import com.interviewiq.interviewstarter.dto.DashboardResponse;
import com.interviewiq.interviewstarter.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * REST controller for the dashboard.
 *
 *   GET /dashboard       -> simple stats { totalInterviews, averageScore }
 *   GET /api/dashboard   -> full payload for the dashboard UI
 */
@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

//    /** Original simple endpoint — kept for backward compatibility. */
//    @GetMapping("/dashboard")
//    public DashboardResponse getDashboardStats() {
//        return dashboardService.getStats();
//    }

    /** Full dashboard payload consumed by dashboard.html. */
    @GetMapping("/api/dashboard")
    public DashboardResponse getDashboard() {
        return dashboardService.buildDashboard();
    }
}

