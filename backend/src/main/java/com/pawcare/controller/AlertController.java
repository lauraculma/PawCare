package com.pawcare.controller;

import com.pawcare.dto.vital.AlertResponse;
import com.pawcare.security.UserPrincipal;
import com.pawcare.service.AlertService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public Page<AlertResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Boolean acknowledged,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return alertService.list(principal, petId, acknowledged, pageable);
    }

    @PatchMapping("/{id}/acknowledge")
    public AlertResponse acknowledge(@PathVariable Long id,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return alertService.acknowledge(id, principal);
    }
}
