package com.pawcare.controller;

import com.pawcare.dto.clinical.ClinicalRecordRequest;
import com.pawcare.dto.clinical.ClinicalRecordResponse;
import com.pawcare.security.UserPrincipal;
import com.pawcare.service.ClinicalRecordService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/clinical-records")
public class ClinicalRecordController {

    private final ClinicalRecordService clinicalRecordService;

    public ClinicalRecordController(ClinicalRecordService clinicalRecordService) {
        this.clinicalRecordService = clinicalRecordService;
    }

    @GetMapping
    public Page<ClinicalRecordResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long appointmentId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return clinicalRecordService.list(principal, petId, appointmentId, pageable);
    }

    @GetMapping("/{id}")
    public ClinicalRecordResponse get(@PathVariable Long id,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return clinicalRecordService.get(id, principal);
    }

    @PostMapping
    public ResponseEntity<ClinicalRecordResponse> create(@Valid @RequestBody ClinicalRecordRequest request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        ClinicalRecordResponse created = clinicalRecordService.create(request, principal);
        return ResponseEntity.created(URI.create("/api/clinical-records/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ClinicalRecordResponse update(@PathVariable Long id,
                                         @Valid @RequestBody ClinicalRecordRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return clinicalRecordService.update(id, request, principal);
    }
}
