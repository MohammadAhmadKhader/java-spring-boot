package com.example.multitenant.controllers.organizationdashboard;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.multitenant.common.annotations.contract.CheckRestricted;
import com.example.multitenant.common.resolvers.contract.*;
import com.example.multitenant.common.validators.contract.ValidateNumberId;
import com.example.multitenant.dtos.apiresponse.ApiResponses;
import com.example.multitenant.dtos.organizationroles.*;
import com.example.multitenant.models.enums.LogEventType;
import com.example.multitenant.services.cache.*;
import com.example.multitenant.services.logs.LogsService;
import com.example.multitenant.services.membership.MemberShipService;
import com.example.multitenant.services.organizations.OrgsService;
import com.example.multitenant.services.security.OrgRolesService;
import com.example.multitenant.utils.AppUtils;
import com.example.multitenant.utils.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@CheckRestricted
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/organizations/dashboard/roles")
public class OrgDashboardRolesController {

    private final OrgsService organizationsService;
    private final OrgRolesService organizationRolesService;
    private final MemberShipService memberShipService;
    private final AuthCacheService authCacheService;
    private final LogsService logsService;

    @GetMapping("")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@orgPermissions.ROLE_VIEW)")
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
    @PreAuthorize("@customSPEL.hasOrgAuthority(@orgPermissions.ROLE_CREATE)")
    public ResponseEntity<Object> createRole(@Valid @RequestBody OrgRoleCreateDTO dto) {
        var orgId = AppUtils.getTenantId();
        var orgRole = this.organizationRolesService.findByNameAndOrganizationId(dto.getName(), orgId);
        if(orgRole != null) {
            return ResponseEntity.badRequest().body(ApiResponses.GetErrResponse("role with such name already exists"));
        }

        var newOrgRole = this.organizationRolesService.createWithBasicPerms(dto.toModel(), orgId);
        this.authCacheService.invalidateOrgRolesCache(orgId);

        var respBody = ApiResponses.OneKey("role", newOrgRole.toViewDTO());

        return ResponseEntity.status(HttpStatus.CREATED).body(respBody);
    }
        
    @PutMapping("/{id}")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@orgPermissions.ROLE_UPDATE)")
    public ResponseEntity<Object> updateRole(@ValidateNumberId @PathVariable Integer id, @Valid @RequestBody OrgRoleUpdateDTO dto) {
        var orgId = AppUtils.getTenantId();
        var updatedRole = this.organizationRolesService.findThenUpdate(id, orgId, dto.toModel());
        if(updatedRole == null) {
            return ResponseEntity.badRequest().body(ApiResponses.GetNotFoundErr("role", id));
        }

        this.authCacheService.invalidateOrgRolesCache(orgId);
        var respBody = ApiResponses.OneKey("role", updatedRole.toViewDTO());
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respBody);
    }
    

    @DeleteMapping("/{id}")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@orgPermissions.ROLE_DELETE)")
    public ResponseEntity<Object> deleteRole(@ValidateNumberId @PathVariable Integer id) {
        var tenantId = AppUtils.getTenantId();
        
        this.organizationRolesService.deleteRole(id, tenantId);
        this.authCacheService.handleRoleDeletionInvalidations(tenantId, id);
        
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assign")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@orgPermissions.ROLE_ASSIGN)")
    public ResponseEntity<Object> assignRole(@Valid @RequestBody OrgAssignRoleDTO dto) {  
        var tenantId = AppUtils.getTenantId();
        var user = SecurityUtils.getUserFromAuth();

        var orgRole = this.memberShipService.assignRole(dto.getRoleId(), tenantId, dto.getUserId());
        this.authCacheService.invalidateUserOrgRolesCache(tenantId, dto.getUserId());
        this.logsService.createRolesAssignmentsLog(user, orgRole, dto.getUserId(), tenantId, LogEventType.ROLE_ASSIGN);

        var respBody = ApiResponses.OneKey("role", orgRole.toViewDTO());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respBody);
    }

    @DeleteMapping("/un-assign/{id}/{userId}")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@orgPermissions.ROLE_UN_ASSIGN)")
    public ResponseEntity<Object> unAssignRole(@ValidateNumberId @PathVariable Integer id, @ValidateNumberId @PathVariable Long userId) {
        var tenantId = AppUtils.getTenantId();
        var user = SecurityUtils.getUserFromAuth();

        var orgRole = this.memberShipService.unAssignRole(id, tenantId, userId);
        this.authCacheService.invalidateUserOrgRolesCache(tenantId, userId);
        this.logsService.createRolesAssignmentsLog(user, orgRole, userId, tenantId, LogEventType.ROLE_UNASSIGN);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/permissions/assign")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@orgPermissions.PERMISSION_UN_ASSIGN)")
    public ResponseEntity<Object> assignPermissions(@Valid @RequestBody OrgAssignPermissionsDTO dto) {
        var tenantId = AppUtils.getTenantId();
        var organization = this.organizationsService.findById(tenantId);
        if(organization == null) {
            var errRes = ApiResponses.GetErrResponse(String.format("tenant with id: '%s' was not found" ,tenantId));
            return ResponseEntity.badRequest().body(errRes);
        }

        var updatedRole = this.organizationRolesService.assignPermissionsToRole(dto.getRoleId(), dto.getPermissionsIds(), tenantId);
        this.authCacheService.invalidateOrgRolesCache(tenantId);

        var respBody = ApiResponses.OneKey("role", updatedRole.toViewDTO());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(respBody);
    }

    @DeleteMapping("/permissions/un-assign/{id}")
    @PreAuthorize("@customSPEL.hasOrgAuthority(@orgPermissions.PERMISSION_UN_ASSIGN)")
    public ResponseEntity<Object> unAssignPermissions(@ValidateNumberId @PathVariable Integer id, @Valid @RequestBody OrgUnAssignPermissionsDTO dto) {
        var tenantId = AppUtils.getTenantId();
        var organization = this.organizationsService.findById(tenantId);
        if(organization == null) {
            var errRes = ApiResponses.GetErrResponse(String.format("tenant with id: '%s' was not found" ,tenantId));
            return ResponseEntity.badRequest().body(errRes);
        }

        this.organizationRolesService.unAssignPermissionsFromRole(id, dto.getPermissionsIds(), tenantId);
        this.authCacheService.invalidateOrgRolesCache(tenantId);

        return ResponseEntity.noContent().build();
    }
}
