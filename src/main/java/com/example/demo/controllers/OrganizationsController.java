package com.example.demo.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dtos.apiResponse.ApiResponses;
import com.example.demo.dtos.organizations.OrganizationCreateDTO;
import com.example.demo.dtos.organizations.OrganizationUpdateDTO;
import com.example.demo.services.organizations.OrganizationsService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Validated
@RestController
@RequestMapping("/api/organizations")
public class OrganizationsController {
    
    private final OrganizationsService organizationsService;
    
    public OrganizationsController(OrganizationsService organizationsService) {
        this.organizationsService = organizationsService;
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('view:organization')")
    public ResponseEntity<Object> createOrganization(@Valid @RequestBody OrganizationCreateDTO dto) {
        var newOrg = this.organizationsService.create(dto.toModel());

        var respBody = ApiResponses.OneKey("organization", newOrg.toViewDTO());
        return ResponseEntity.status(HttpStatus.CREATED).body(respBody);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateOrganization(@PathVariable Integer id, @Valid @RequestBody OrganizationUpdateDTO dto) {
        var updatedOrg = this.organizationsService.update(id, dto.toModel());
        
        var respBody = ApiResponses.OneKey("organization", updatedOrg.toViewDTO());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respBody);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteOrganization(@PathVariable Integer id) {
        var isDeleted = this.organizationsService.findThenDeleteById(id);
        if(!isDeleted) {
            return ResponseEntity.badRequest().body(ApiResponses.GetNotFoundErr("organization", id));
        }
        
        return ResponseEntity.noContent().build();
    }
    
}
