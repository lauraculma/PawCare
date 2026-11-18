package com.pawcare.controller;

import com.pawcare.dto.treatment.TreatmentRequest;
import com.pawcare.dto.treatment.TreatmentResponse;
import com.pawcare.security.UserPrincipal;
import com.pawcare.service.TreatmentService;
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
@RequestMapping("/api/treatments")
public class TreatmentController {

    private final TreatmentService treatmentService;

    public TreatmentController(TreatmentService treatmentService) {
        this.treatmentService = treatmentService;
    }

    @GetMapping
    public Page<TreatmentResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long clinicalRecordId,
            @RequestParam(required = false) Long petId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return treatmentService.list(principal, clinicalRecordId, petId, pageable);
    }

    @GetMapping("/{id}")
    public TreatmentResponse get(@PathVariable Long id,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return treatmentService.get(id, principal);
    }

    @PostMapping
    public ResponseEntity<TreatmentResponse> create(@Valid @RequestBody TreatmentRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        TreatmentResponse created = treatmentService.create(request, principal);
        return ResponseEntity.created(URI.create("/api/treatments/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public TreatmentResponse update(@PathVariable Long id,
                                    @Valid @RequestBody TreatmentRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        return treatmentService.update(id, request, principal);
    }
}
