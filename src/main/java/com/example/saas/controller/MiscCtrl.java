package com.example.saas.controller;

import com.example.saas.dto.Dtos.AuditLogResponse;
import com.example.saas.dto.Dtos.DashboardResponse;
import com.example.saas.service.AuditService;
import com.example.saas.service.DashboardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MiscCtrl {

    private final DashboardService dashboardService;
    private final AuditService auditService;

    public MiscCtrl(DashboardService dashboardService, AuditService auditService) {
        this.dashboardService = dashboardService;
        this.auditService = auditService;
    }

    @GetMapping("/api/v1/dashboard")
    public DashboardResponse dashboard() {
        return dashboardService.get();
    }

    @GetMapping("/api/v1/audit-logs")
    public Page<AuditLogResponse> auditLogs(@PageableDefault(size = 20) Pageable pageable) {
        return auditService.list(pageable);
    }
}
