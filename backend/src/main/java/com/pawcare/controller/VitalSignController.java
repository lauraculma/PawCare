package com.pawcare.controller;

import com.pawcare.dto.vital.VitalSignRequest;
import com.pawcare.dto.vital.VitalSignResponse;
import com.pawcare.security.UserPrincipal;
import com.pawcare.service.VitalSignService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api")
public class VitalSignController {

    private final VitalSignService vitalSignService;

    public VitalSignController(VitalSignService vitalSignService) {
        this.vitalSignService = vitalSignService;
    }

    @PostMapping("/vitals")
    public ResponseEntity<VitalSignResponse> ingest(@Valid @RequestBody VitalSignRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        VitalSignResponse created = vitalSignService.ingest(request, principal);
        return ResponseEntity.created(URI.create("/api/pets/" + created.petId() + "/vitals")).body(created);
    }

    @GetMapping("/pets/{petId}/vitals")
    public Page<VitalSignResponse> list(
            @PathVariable Long petId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 50, sort = "recordedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return vitalSignService.list(petId, from, to, principal, pageable);
    }
}
