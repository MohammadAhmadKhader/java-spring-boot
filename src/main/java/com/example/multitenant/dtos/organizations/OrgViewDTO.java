package com.example.multitenant.dtos.organizations;

import java.time.Instant;
import java.util.List;

import com.example.multitenant.dtos.users.UserViewDTO;
import com.example.multitenant.models.Organization;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrgViewDTO {
    private Integer id;

    private String name;

    private String industry;

    private String imageUrl;

    private Instant createdAt;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<UserViewDTO> users;

    public OrgViewDTO(Organization org) {
        setId(org.getId());
        setName(org.getName());
        setIndustry(org.getIndustry());
        setCreatedAt(org.getCreatedAt());
        setImageUrl(org.getImageUrl());
    }
}
