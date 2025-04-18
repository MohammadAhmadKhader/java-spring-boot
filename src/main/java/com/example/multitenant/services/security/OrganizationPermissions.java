package com.example.multitenant.services.security;

import org.springframework.stereotype.Component;

@Component
public class OrganizationPermissions {
    public static final String ROLE_VIEW = "org-dashboard:role:view";
    public static final String ROLE_CREATE = "org-dashboard:role:create";
    public static final String ROLE_UPDATE = "org-dashboard:role:update";
    public static final String ROLE_DELETE = "org-dashboard:role:delete";
    public static final String ROLE_ASSIGN = "org-dashboard:role:assign";
    public static final String ROLE_UN_ASSIGN = "org-dashboard:role:un-assign";
    public static final String PERMISSION_ASSIGN = "org-dashboard:permission:assign";
    public static final String PERMISSION_UN_ASSIGN = "org-dashboard:permission:un-assign";
    public static final String CONTENT_VIEW = "organization:content:view";
    public static final String CONTENT_CREATE = "organization:content:create";
    public static final String CONTENT_UPDATE = "organization:content:update";
    public static final String CONTENT_DELETE = "organization:content:delete";
    public static final String DASH_CONTENT_VIEW = "org-dashboard:content:view"; // new
    public static final String DASH_CONTENT_DELETE = "org-dashboard:content:delete"; // new
    public static final String DASH_ORGANIZATION_UPDATE = "org-dashboard:organization:update"; // new
    public static final String USER_INVITE = "organization:user:invite";
    public static final String USER_KICK = "organization:user:kick";
    public static final String TRANSFER_OWNERSHIP = "organization:transfer-ownership";
}
