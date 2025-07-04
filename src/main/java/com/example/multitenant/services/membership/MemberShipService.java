package com.example.multitenant.services.membership;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.example.multitenant.services.security.*;
import com.example.multitenant.specifications.MembershipSpec;
import com.example.multitenant.specificationsbuilders.MembershipSpecBuilder;
import com.example.multitenant.utils.PageableHelper;
import com.example.multitenant.utils.SecurityUtils;
import com.example.multitenant.utils.VirtualThreadsUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.multitenant.dtos.apiresponse.ApiResponses;
import com.example.multitenant.dtos.membership.MembershipFilter;
import com.example.multitenant.dtos.organizations.*;
import com.example.multitenant.exceptions.*;
import com.example.multitenant.models.*;
import com.example.multitenant.models.binders.*;
import com.example.multitenant.models.enums.*;
import com.example.multitenant.repository.MembershipRepository;
import com.example.multitenant.services.files.FilesService;
import com.example.multitenant.services.organizations.OrgsService;

@RequiredArgsConstructor
@Service
public class MemberShipService {
    private static String defaultSortBy = "joinedAt";
    private static String defaultSortDir = "DESC";

    private final OrgRolesService organizationRolesService;
    private final OrgPermissionsService organizationsPermissionsService;
    private final MembershipRepository membershipRepository;
    private final OrgsService organizationsService;
    private final MemberShipSpecService memberShipSpecificationsService;
    private final MemberShipCrudService memberShipCrudService;
    private final FilesService filesService;

    public Page<Membership> getOrganizaionMemberships(Integer page, Integer size, Integer organizationId) {
        var org = new Organization();
        org.setId(organizationId);
        var pageable = PageRequest.of(page - 1, size, Sort.by("joinedAt","id").descending());
        
        return this.membershipRepository.findByOrgAndIsMemberTrue(org, pageable);
    }
    
    public boolean hasUserJoined(Integer orgId, long userId) {
        var membershipKey = new MembershipKey(orgId, userId);
        var memebership = this.memberShipCrudService.findById(membershipKey);
        if(memebership == null) {
            return false;
        }

        return memebership.isMember();
    }

    @Transactional
    public Membership joinOrganization(Integer orgId, long userId, boolean isRestricted) {
        if(orgId == null || orgId<= 0) {
            throw new InvalidOperationException("invalid organization id");
        }

        var oldMembership = this.findOne(orgId, userId);
        if(oldMembership != null && oldMembership.isMember() == true) {
            throw new InvalidOperationException("user is already a member of this organization");
        }

        var membership = oldMembership == null ? new Membership(orgId, userId) : oldMembership;
        if(isRestricted) {
            throw new AccessDeniedException("user is restricted from this organization");
        }

        var orgUserRole = this.organizationRolesService.
        findByNameAndOrganizationId(DefaultOrganizationRole.ORG_USER.getRoleName(), orgId);
        if(orgUserRole == null) {
            throw new ResourceNotFoundException("organization role");
        }
        
        membership.getOrganizationRoles().add(orgUserRole);
        membership.loadDefaults();

        return this.membershipRepository.save(membership);
    }

    @Transactional
    public Membership createOwnerMembership(Organization org, User user) {
        var membership = new Membership(org.getId(), user.getId());
        var createdMembership = this.membershipRepository.save(membership);

        var roles = this.initializeDefaultRolesAndPermissions(org.getId());
        if(roles.isEmpty()) {
            throw new InvalidOperationException("could not create default roles and permissions");
        }

        createdMembership.getOrganizationRoles().addAll(roles);
        createdMembership.loadDefaults();
        
        return this.membershipRepository.saveAndFlush(createdMembership);
    }

    @Transactional
    public Membership createOrganizationWithOwnerMembership(OrgCreateDTO dto, User user) {
        var org = this.organizationsService.create(dto.toModel());
        var membership = new Membership(org.getId(), user.getId());

        var roles = this.initializeDefaultRolesAndPermissions(org.getId());
        membership.setOrganizationRoles(roles);
        membership.loadDefaults();
        org.getMemberships().add(membership);
        
        return this.membershipRepository.saveAndFlush(membership);
    }

    @Transactional
    public List<Membership> swapOwnerShip(Integer orgId, User currOwner, Long newOwnerId) {
        var org = this.organizationsService.findOneWithOwner(orgId);
        if(org == null) {
            throw new ResourceNotFoundException("organization", orgId);
        }

        var tasksResults = VirtualThreadsUtils.run(
            () -> this.findUserMembershipWithRoles(orgId, newOwnerId),
            () -> this.findUserMembershipWithRoles(orgId, currOwner.getId())
        );

        var newOwnerMembeship = tasksResults.getLeft();
        if(newOwnerMembeship == null || !newOwnerMembeship.isMember()) {
            throw new InvalidOperationException("user must be an organization member");
        }
        
        var owner = org.getOwner();
        if(owner == null || owner.getId() != currOwner.getId()) {
            throw new InvalidOperationException("user must be the owner");
        }

        var currOwnerMembership = tasksResults.getRight();
        if(currOwnerMembership == null || !currOwnerMembership.isMember()) {
            throw new UnknownException("error during fetching current owner membership");
        }

        var isRemovedOwnerRoole = currOwnerMembership.getOrganizationRoles().
        removeIf((role) -> role.getName().equals(DefaultOrganizationRole.ORG_OWNER.getRoleName()));

        if(!isRemovedOwnerRoole) {
            throw new UnknownException("failed to remove owner role during transfering ownership");
        }

        var ownerRole = this.organizationRolesService.
        findByNameAndOrganizationId(DefaultOrganizationRole.ORG_OWNER.getRoleName(), orgId);

        var isAddedOwnerRole = newOwnerMembeship.getOrganizationRoles().add(ownerRole);
        if(!isAddedOwnerRole) {
            throw new UnknownException("failed to add owner role durinh transfering ownership");
        }

        this.organizationsService.setOwner(org, newOwnerMembeship.getUser());

        return this.membershipRepository.saveAllAndFlush(List.of(currOwnerMembership, newOwnerMembeship));
    }

    public Membership kickUserFromOrganization(Integer orgId, long userId) {
        var membershipKey = new MembershipKey(orgId, userId);
        
        var trasksResults = VirtualThreadsUtils.run(
            () -> this.organizationsService.findOneWithOwner(orgId),
            () -> this.membershipRepository.findById(membershipKey)
        );

        var membershipOpt = trasksResults.getRight();
        var organization = trasksResults.getLeft();
        if (membershipOpt.isEmpty() || !membershipOpt.get().isMember()) {
            throw new InvalidOperationException("user is not part of the organization");
        }

        var membership = membershipOpt.get();

        if (organization == null) {
            throw new ResourceNotFoundException("organization", orgId);
        }

        if(organization.getOwner().getId() == membership.getId().getUserId()) {
            throw new InvalidOperationException("can not leave an owned organization you have to delete it, or transfer ownership first");
        }

        membership.setMember(false);

        return this.membershipRepository.save(membership);
    }

    public OrgRole assignRole(Integer orgRoleId, Integer orgId, long userId) {
        var tasksResults = VirtualThreadsUtils.run(
            () -> this.findUserMembershipWithRoles(orgId, userId),
            () -> this.organizationRolesService.findOne(orgRoleId)
        );

        var membership = tasksResults.getLeft();
        var orgRole = tasksResults.getRight();

        if(membership == null) {
            throw new ResourceNotFoundException("membership");
        }

        if(orgRole == null || !orgRole.getOrganizationId().equals(orgId)) {
            throw new ResourceNotFoundException("organization role", orgRoleId);
        }

        membership.getOrganizationRoles().forEach((role) -> {
            if(role.getId().equals(orgRoleId)) {
                throw new InvalidOperationException("user already have the role");
            }
        });

        this.assignRoleToUser(membership, orgRole);
        return orgRole;
    }

    public OrgRole unAssignRole(Integer orgRoleId, Integer orgId, long userId) {
        var tasksResult = VirtualThreadsUtils.run(
            () -> this.findUserMembershipWithRoles(orgId, userId), 
            () -> this.organizationRolesService.findOne(orgRoleId)
        );

        var membership = tasksResult.getLeft();
        var orgRole = tasksResult.getRight();
    
        if(membership == null) {
            throw new ResourceNotFoundException("membership");
        }

        if(orgRole == null) {
            throw new ResourceNotFoundException("organization role", orgRoleId);
        }

        var hasRole = membership.getOrganizationRoles()
            .stream()
            .anyMatch(role -> role.getId().equals(orgRoleId));

        if (!hasRole) {
            throw new InvalidOperationException("user does not have this role assigned");
        }

        this.unAssignRoleToUser(membership, orgRole);
        return orgRole;
    }

    public Membership findOne(Integer orgId, long userId) {
        var membershipKey = new MembershipKey(orgId, userId);
        return this.memberShipCrudService.findById(membershipKey);
    }

    public boolean isMember(Integer orgId, long userId) {
        var membershipKey = new MembershipKey(orgId, userId);
        var probe = new Membership();
        probe.setId(membershipKey);
        probe.setMember(true);

        return this.membershipRepository.exists(Example.of(probe));
    }

    public Membership findUserMembershipWithRolesAndPermissions(Integer orgId, long userId) {
        var organization = new Organization();
        organization.setId(orgId);

        var membership = this.membershipRepository.findUserMembershipWithRolesAndPermissions(organization, userId);
        return membership;
    }

    public List<Long> findUserIdsByOrgIdAndRoleId(Integer orgId, Integer roleId) {
        return this.membershipRepository.findUserIdsByOrgIdAndRoleId(orgId, roleId);
    }

    @Transactional
    public Membership initializeOrganizationWithMembership(Organization org, User owner, MultipartFile image) {
        var createdOrg = this.organizationsService.create(org);
        if(image != null) {
            var fileResponse = this.filesService.uploadFile(image, FilesPath.ORGS_IMAGES);
            createdOrg.setImageUrl(fileResponse.getUrl());
        }
        var membership = this.createOwnerMembership(createdOrg, owner);
        return membership;
    }

    public Membership findUserMembershipWithRoles(Integer orgId, long userId) {
        var membership = this.membershipRepository.findUserMembershipWithRoles(orgId, userId);
        return membership;
    }

    public Membership assignRoleToUser(Membership membership, OrgRole orgRole) {
        if(orgRole.getName().equals(DefaultOrganizationRole.ORG_OWNER.getRoleName())) {
            throw new InvalidOperationException("cant assign organization owner to a user");
        }

        membership.getOrganizationRoles().add(orgRole);
        return this.membershipRepository.save(membership);
    }

    public Membership unAssignRoleToUser(Membership membership, OrgRole orgRole) {
        if(orgRole.getName().equals(DefaultOrganizationRole.ORG_OWNER.getRoleName())) {
            throw new InvalidOperationException("cant un-assign organization owner from a user");
        }

        if(orgRole.getName().equals(DefaultOrganizationRole.ORG_USER.getRoleName())) {
            throw new InvalidOperationException("cant un-assign organization user from a user");
        }

        var isRemoved = membership.getOrganizationRoles().removeIf((role) -> role.getId().equals(orgRole.getId()));
        if (!isRemoved) {
            throw new UnknownException("an error has occured during attempt to un-assign organization role");
        }

        return this.membershipRepository.save(membership);
    }

    public Page<Membership> findActiveOrgMemberShips(Integer orgId, Integer page, Integer size, String sortBy, String sortDir, MembershipFilter filter) {
        var pageable = PageableHelper.HandleSortWithPagination(defaultSortBy, defaultSortDir, sortBy, sortDir, page, size);
        var spec = MembershipSpecBuilder.build(filter, orgId, true);

        return this.memberShipSpecificationsService.findAllWithSpecifications(pageable, spec,null);
    }

    public long countOrganizationMembers(Integer orgId) {
        return this.membershipRepository.countMembersByOrgId(orgId);
    }

    public List<OrgRole> initializeDefaultRolesAndPermissions(Integer orgId) {
        var roles = List.of(
            DefaultOrganizationRole.ORG_OWNER,
            DefaultOrganizationRole.ORG_ADMIN,
            DefaultOrganizationRole.ORG_USER
        );
    
        List<OrgRole> orgRoles = roles.stream()
            .map(role -> createRoleWithPermissions(role, orgId))
            .toList();
    
        return this.organizationRolesService.createMany(orgRoles);
    }

    // * private methods below
    private OrgRole createRoleWithPermissions(DefaultOrganizationRole defaultRole, Integer orgId) {
        var role = new OrgRole(defaultRole.getRoleName());
        role.setDisplayName(defaultRole.getDefaultDisplayName()); // assuming this method exists
        role.setOrganizationId(orgId);
    
        var permissions = this.organizationsPermissionsService.findAllDefaultPermissions(defaultRole);
        role.setOrganizationPermissions(permissions);
    
        return role;
    }
}