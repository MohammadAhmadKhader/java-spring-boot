package com.example.multitenant.dtos.auth;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.multitenant.dtos.users.UserViewDTO;
import com.example.multitenant.models.User;
import com.fasterxml.jackson.annotation.*;
import lombok.*;

public class UserPrincipal implements UserDetails {
    // we must explicitly declare one, since we using long live sessions
    private static final long serialVersionUID = 1L;
    private final User user;

    @JsonCreator
    public UserPrincipal(@JsonProperty User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (var role : this.user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

            for (var perm : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(perm.getName()));
            }
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return this.user.getPassword();
    }

    @Override
    public String getUsername() {
        return this.user.getEmail();
    }

    public User getUser(){
        return this.user;
    }
}
