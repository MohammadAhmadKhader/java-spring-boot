package com.example.multitenant.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.example.multitenant.dtos.membership.MembershipViewDTO;
import com.example.multitenant.models.binders.MembershipKey;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Entity
@Table(name = "membership")
public class Membership {
    @EmbeddedId
    private MembershipKey id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("organizationId")
    @JoinColumn(name = "organization_id", nullable = false, insertable = false, updatable = false)
    private Organization organization;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(
        name = "membership_roles",
        joinColumns = {
            @JoinColumn(name = "organization_id", referencedColumnName =  "organization_id"),
            @JoinColumn(name = "user_id", referencedColumnName =  "user_id")
        },
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @OrderBy("id ASC")
    private List<OrganizationRole> organizationRoles;

    @CreationTimestamp
    private Instant joinedAt;

    @JsonProperty("isMember")
    @Column(name = "is_member", nullable = false)
    private boolean isMember;

    public Membership(MembershipKey id) {
        this.id = id;
    }

    public Membership(Integer orgId, long userId) {
        this.id = this.getKey(orgId, userId);
        this.user = new User();
        this.user.setId(userId);
        this.organization = new Organization();
        this.organization.setId(orgId);
    }

    public void loadDefaults() {
        this.isMember = true;
    }

    public MembershipViewDTO toViewDTO() {
        return new MembershipViewDTO(this);
    }

    private MembershipKey getKey(Integer orgId, long userId) {
        return new MembershipKey(orgId, userId);
    }
}
