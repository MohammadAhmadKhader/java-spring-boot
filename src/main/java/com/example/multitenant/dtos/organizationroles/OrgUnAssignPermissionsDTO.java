package com.example.multitenant.dtos.organizationroles;

import java.util.Set;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
public class OrgUnAssignPermissionsDTO {
    @NotEmpty(message = "one permissions Id is required at least")
    private Set<@NotNull(message = "permission id cannot be null") 
               @Positive(message = "permission id must be positive") Integer> permissionsIds;
}
