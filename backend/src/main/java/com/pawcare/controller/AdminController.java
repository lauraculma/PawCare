package com.pawcare.controller;

import com.pawcare.dto.auth.UserResponse;
import com.pawcare.dto.admin.AdminUserUpdateRequest;
import com.pawcare.dto.admin.VeterinarianScheduleResponse;
import com.pawcare.dto.admin.WeeklyShiftUpdateRequest;
import com.pawcare.service.AdminService;
import com.pawcare.service.VeterinarianShiftService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final VeterinarianShiftService shiftService;

    public AdminController(AdminService adminService, VeterinarianShiftService shiftService) {
        this.adminService = adminService;
        this.shiftService = shiftService;
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

    @GetMapping("/users")
    public Page<UserResponse> users(@PageableDefault(size = 50, sort = "fullName") Pageable pageable) {
        return adminService.users(pageable);
    }

    @PutMapping("/users/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateRequest request) {
        return adminService.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/schedules")
    public List<VeterinarianScheduleResponse> schedules() {
        return shiftService.listSchedules();
    }

    @PutMapping("/veterinarians/{id}/schedule")
    public VeterinarianScheduleResponse updateSchedule(@PathVariable Long id,
            @Valid @RequestBody WeeklyShiftUpdateRequest request) {
        return shiftService.saveSchedule(id, request);
    }
}
