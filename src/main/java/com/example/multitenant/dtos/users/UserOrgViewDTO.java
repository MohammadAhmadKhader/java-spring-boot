package com.example.multitenant.dtos.users;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

import com.example.multitenant.dtos.globalroles.GlobalRoleViewDTO;
import com.example.multitenant.dtos.organizationroles.OrgRoleViewDTO;
import com.example.multitenant.models.*;

import lombok.*;

@Getter
@Setter
public class UserOrgViewDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String firstName;
    private String lastName;
    private String email;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrgRoleViewDTO> roles;

    public UserOrgViewDTO(User user, Membership membership) {
        setId(user.getId());
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
        setEmail(user.getEmail());
        setCreatedAt(user.getCreatedAt());
        setUpdatedAt(user.getUpdatedAt());
        setRoles(membership.getOrganizationRoles().stream().map((r) -> r.toViewDTO()).toList());
        setAvatarUrl(user.getAvatarUrl());
    }

    public UserOrgViewDTO() {
        
    }
}
