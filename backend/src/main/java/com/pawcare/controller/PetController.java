package com.pawcare.controller;

import com.pawcare.dto.pet.PetRequest;
import com.pawcare.dto.pet.PetResponse;
import com.pawcare.security.UserPrincipal;
import com.pawcare.service.PetService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public Page<PetResponse> list(@AuthenticationPrincipal UserPrincipal principal,
                                  @RequestParam(required = false) String search,
                                  @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return petService.list(principal, search, pageable);
    }

    @GetMapping("/{id}")
    public PetResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return petService.get(id, principal);
    }

    @PostMapping
    public ResponseEntity<PetResponse> create(@Valid @RequestBody PetRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        PetResponse created = petService.create(request, principal);
        return ResponseEntity.created(URI.create("/api/pets/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public PetResponse update(@PathVariable Long id,
                              @Valid @RequestBody PetRequest request,
                              @AuthenticationPrincipal UserPrincipal principal) {
        return petService.update(id, request, principal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        petService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }
}
