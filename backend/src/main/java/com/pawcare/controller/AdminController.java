package com.pawcare.controller;

import com.pawcare.dto.auth.UserResponse;
import com.pawcare.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/veterinarians/pending")
    public Page<UserResponse> pendingVeterinarians(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return adminService.pendingVeterinarians(pageable);
    }

    @PatchMapping("/veterinarians/{id}/approve")
    public UserResponse approveVeterinarian(@PathVariable Long id) {
        return adminService.approveVeterinarian(id);
    }
}
