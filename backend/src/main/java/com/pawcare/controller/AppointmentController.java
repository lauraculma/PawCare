package com.pawcare.controller;

import com.pawcare.dto.appointment.AppointmentRequest;
import com.pawcare.dto.appointment.AppointmentResponse;
import com.pawcare.dto.appointment.AppointmentUpdateRequest;
import com.pawcare.dto.appointment.StatusUpdateRequest;
import com.pawcare.entity.AppointmentStatus;
import com.pawcare.security.UserPrincipal;
import com.pawcare.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public Page<AppointmentResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = "scheduledAt") Pageable pageable) {
        return appointmentService.list(principal, veterinarianId, petId, status, from, to, pageable);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return appointmentService.get(id, principal);
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        AppointmentResponse created = appointmentService.create(request, principal);
        return ResponseEntity.created(URI.create("/api/appointments/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public AppointmentResponse update(@PathVariable Long id,
                                      @Valid @RequestBody AppointmentUpdateRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return appointmentService.update(id, request, principal);
    }

    @PatchMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return appointmentService.cancel(id, principal);
    }

    @PatchMapping("/{id}/status")
    public AppointmentResponse updateStatus(@PathVariable Long id,
                                            @Valid @RequestBody StatusUpdateRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return appointmentService.updateStatus(id, request, principal);
    }
}
