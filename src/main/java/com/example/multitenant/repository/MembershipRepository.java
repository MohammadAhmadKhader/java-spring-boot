package com.example.multitenant.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.multitenant.models.Membership;
import com.example.multitenant.models.Organization;
import com.example.multitenant.models.binders.MembershipKey;

@Repository
public interface MembershipRepository extends GenericRepository<Membership, MembershipKey>, JpaSpecificationExecutor<Membership> {
    @EntityGraph(attributePaths = "user")
    @Query("SELECT m FROM Membership m WHERE m.organization = :organization AND m.isMember = true")
    public Page<Membership> findByOrganizationAndIsMemberTrue(@Param("organization") Organization organization, Pageable pageable);

    @EntityGraph(attributePaths = {"organizationRoles", "organizationRoles.organizationPermissions"})
    @Query("SELECT m FROM Membership m WHERE m.organization = :organization AND m.user.id = :userId AND m.isMember = true")
    public Membership findUserMembershipWithRolesAndPermissions(@Param("organization") Organization organization, @Param("userId") long userId);

    @EntityGraph(attributePaths = {"organizationRoles"})
    @Query("SELECT m FROM Membership m WHERE m.organization = :organization AND m.user.id = :userId AND m.isMember = true")
    public Membership findUserMembershipWithRoles(@Param("organization") Organization organization, @Param("userId") long userId);
}
