package com.example.multitenant.dtos.globalpermissions;

import java.io.Serializable;

import com.example.multitenant.models.GlobalPermission;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
public class GlobalPermissionWithRolesViewDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer id;
    
    private String name;

    public GlobalPermissionWithRolesViewDTO(GlobalPermission perm) {
        setId(perm.getId());
        setName(perm.getName());
    }
}
