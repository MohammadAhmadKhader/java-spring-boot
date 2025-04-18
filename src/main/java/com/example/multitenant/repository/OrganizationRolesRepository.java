package com.example.multitenant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.multitenant.models.OrganizationRole;

import java.util.Optional;


@Repository
public interface OrganizationRolesRepository extends GenericRepository<OrganizationRole, Integer>, JpaSpecificationExecutor<OrganizationRole> {
    public Optional<OrganizationRole> findByNameAndOrganizationId(String name, Integer organizationId);

    @Query("""
        SELECT r FROM OrganizationRole r
        LEFT JOIN FETCH r.organizationPermissions
        WHERE (r.id = :id AND r.organizationId = :organizationId)
    """)
    OrganizationRole findByIdAndOrgIdWithPermissions(@Param("id") Integer id, @Param("organizationId") Integer organizationId);
}