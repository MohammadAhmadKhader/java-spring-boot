package com.example.multitenant.controllers.organizationDashboard;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.multitenant.common.resolvers.contract.HandlePage;
import com.example.multitenant.common.resolvers.contract.HandleSize;
import com.example.multitenant.common.validators.contract.ValidateNumberId;
import com.example.multitenant.dtos.apiResponse.ApiResponses;
import com.example.multitenant.services.contents.ContentsService;
import com.example.multitenant.services.organizations.OrganizationsService;
import com.example.multitenant.services.security.OrganizationRolesService;

@Validated
@RestController
@RequestMapping("/api/organizations/dashboard/contents")
public class OrganizationDashboardContentsController {
    private final ContentsService contentsService;

    public OrganizationDashboardContentsController(ContentsService contentsService) {
        this.contentsService = contentsService;
    }

    @GetMapping("/users/{userId}/contents")
    @PreAuthorize("@customSPEL.hasOrgAuthority(authentication, #tenantId, @organizationPermissions.CONTENT_VIEW)")
    public ResponseEntity<Object> getContentsByUserId(
        @PathVariable @ValidateNumberId Long userId,
        @HandlePage Integer page, @HandleSize Integer size, 
        @RequestHeader("X-Tenant-ID") String tenantId) {
        
        var contents = this.contentsService.findContentsByUserId(page, size,userId, Integer.parseInt(tenantId));
        var count = contents.getTotalElements();
        var contentsViews = contents.stream().map((con) -> {
            return con.toViewDTO();
        }).toList();

        var bodyResponse = ApiResponses.GetAllResponse("contents", contentsViews, count, page, size);
        
        return ResponseEntity.ok().body(bodyResponse);
    }

    @DeleteMapping("/contents/{id}")
    @PreAuthorize("@customSPEL.hasOrgAuthority(authentication, #tenantId, @globalPermissions.CONTENT_DELETE)")
    public ResponseEntity<Object> deleteContent(@ValidateNumberId @PathVariable Integer id, @RequestHeader("X-Tenant-ID") String tenantId) {
        var content = this.contentsService.findByIdAndOrganizationId(id, Integer.parseInt(tenantId));
        if(content == null) {
            return ResponseEntity.badRequest().body(ApiResponses.GetNotFoundErr("content", id));
        }

        this.contentsService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
