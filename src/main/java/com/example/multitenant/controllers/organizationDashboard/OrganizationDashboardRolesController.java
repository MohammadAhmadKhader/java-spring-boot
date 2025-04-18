package com.example.multitenant.controllers.organizationDashboard;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.multitenant.common.resolvers.contract.HandlePage;
import com.example.multitenant.common.resolvers.contract.HandleSize;
import com.example.multitenant.common.validators.contract.ValidateNumberId;
import com.example.multitenant.dtos.apiResponse.ApiResponses;
import com.example.multitenant.dtos.organizationroles.AssignOrganizationPermissionsDTO;
import com.example.multitenant.dtos.organizationroles.AssignOrganizationRoleDTO;
import com.example.multitenant.dtos.organizationroles.OrganizationRoleCreateDTO;
import com.example.multitenant.dtos.organizationroles.OrganizationRoleUpdateDTO;
import com.example.multitenant.dtos.organizationroles.UnAssignOrganizationPermissionsDTO;
import com.example.multitenant.services.membership.MemberShipService;
import com.example.multitenant.services.organizations.OrganizationsService;
import com.example.multitenant.services.security.OrganizationRolesService;
import com.example.multitenant.utils.AppUtils;

import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/organizations/dashboard/roles")
public class OrganizationDashboardRolesController {
    private final OrganizationsService organizationsService;
    private final OrganizationRolesService organizationRolesService;
    private final MemberShipService memberShipService;

    public OrganizationDashboardRolesController(OrganizationsService organizationsService, OrganizationRolesService organizationRolesService, MemberShipService memberShipService) {
        this.organizationsService = organizationsService;
        this.organizationRolesService = organizationRolesService;
        this.memberShipService = memberShipService;
    }

    @GetMapping("")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@organizationPermissions.ROLE_VIEW)")
    public ResponseEntity<Object> getRoles(@HandlePage Integer page, @HandleSize Integer size) { 
        var tenantId = AppUtils.getTenantId();

        var roles = this.organizationRolesService.findAllRoles(page, size, tenantId);
        var count = roles.getTotalElements();
        var rolesViews = roles.map((con) -> {
            return con.toViewDTO();
        }).toList();
        
        var res = ApiResponses.GetAllResponse("roles", rolesViews, count, page, size);
        
        return ResponseEntity.ok().body(res);
    }

    @PostMapping("")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@organizationPermissions.ROLE_CREATE)")
    public ResponseEntity<Object> createRole(@Valid @RequestBody OrganizationRoleCreateDTO dto) {
        var orgId = AppUtils.getTenantId();
        var orgRole = this.organizationRolesService.findByNameAndOrganizationId(dto.getName(), orgId);
        if(orgRole != null) {
            return ResponseEntity.badRequest().body(ApiResponses.GetErrResponse("role with such name already exists"));
        }

        var newOrgRole = this.organizationRolesService.createWithBasicPerms(dto.toModel(), orgId);
        var respBody = ApiResponses.OneKey("role", newOrgRole.toViewDTO());

        return ResponseEntity.status(HttpStatus.CREATED).body(respBody);
    }
        
    @PutMapping("/{id}")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@organizationPermissions.ROLE_UPDATE)")
    public ResponseEntity<Object> updateRole(@ValidateNumberId @PathVariable Integer id, @Valid @RequestBody OrganizationRoleUpdateDTO dto) {
        var orgId = AppUtils.getTenantId();
        var updatedRole = this.organizationRolesService.findThenUpdate(id, orgId, dto.toModel());
        if(updatedRole == null) {
            return ResponseEntity.badRequest().body(ApiResponses.GetNotFoundErr("role", id));
        }

        var respBody = ApiResponses.OneKey("role", updatedRole.toViewDTO());
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respBody);
    }
    

    @DeleteMapping("/{id}")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@organizationPermissions.ROLE_DELETE)")
    public ResponseEntity<Object> deleteRole(@ValidateNumberId @PathVariable Integer id) {
        var tenantId = AppUtils.getTenantId();
        this.organizationRolesService.deleteRole(id, tenantId);
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assign")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@organizationPermissions.ROLE_ASSIGN)")
    public ResponseEntity<Object> assignRole(@Valid @RequestBody AssignOrganizationRoleDTO dto) {  
        var tenantId = AppUtils.getTenantId();

        var updatedRole = this.memberShipService.assignRole(dto.getRoleId(), tenantId, dto.getUserId());
        var respBody = ApiResponses.OneKey("role", updatedRole.toViewDTO());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respBody);
    }

    @DeleteMapping("/un-assign/{id}/{userId}")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@organizationPermissions.ROLE_UN_ASSIGN)")
    public ResponseEntity<Object> unAssignRole(@ValidateNumberId @PathVariable Integer id, @ValidateNumberId @PathVariable Long userId) {
        var tenantId = AppUtils.getTenantId();
        this.memberShipService.unAssignRole(id, tenantId, userId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/permissions/assign")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@organizationPermissions.PERMISSION_UN_ASSIGN)")
    public ResponseEntity<Object> assignPermissions(@Valid @RequestBody AssignOrganizationPermissionsDTO dto) {
        var tenantId = AppUtils.getTenantId();
        var organization = this.organizationsService.findById(tenantId);
        if(organization == null) {
            var errRes = ApiResponses.GetErrResponse(String.format("tenant with id: '%s' was not found" ,tenantId));
            return ResponseEntity.badRequest().body(errRes);
        }

        var updatedRole = this.organizationRolesService.assignPermissionsToRole(dto.getRoleId(), dto.getPermissionsIds(), tenantId);
        var respBody = ApiResponses.OneKey("role", updatedRole.toViewDTO());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respBody);
    }

    @DeleteMapping("/permissions/un-assign/{id}")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@organizationPermissions.PERMISSION_UN_ASSIGN)")
    public ResponseEntity<Object> unAssignPermissions(@ValidateNumberId @PathVariable Integer id, @Valid @RequestBody UnAssignOrganizationPermissionsDTO dto) {
        var tenantId = AppUtils.getTenantId();
        var organization = this.organizationsService.findById(tenantId);
        if(organization == null) {
            var errRes = ApiResponses.GetErrResponse(String.format("tenant with id: '%s' was not found" ,tenantId));
            return ResponseEntity.badRequest().body(errRes);
        }

        this.organizationRolesService.unAssignPermissionsFromRole(id, dto.getPermissionsIds(), tenantId);

        return ResponseEntity.noContent().build();
    }
}
