package com.example.multitenant.controllers;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.management.RuntimeErrorException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.multitenant.dtos.apiResponse.ApiResponses;
import com.example.multitenant.dtos.auth.LoginDTO;
import com.example.multitenant.dtos.auth.RegisterDTO;
import com.example.multitenant.dtos.auth.ResetPasswordDTO;
import com.example.multitenant.dtos.auth.UserPrincipal;
import com.example.multitenant.exceptions.AsyncOperationException;
import com.example.multitenant.models.enums.DefaultGlobalRole;
import com.example.multitenant.services.cache.RedisService;
import com.example.multitenant.services.security.GlobalRolesService;
import com.example.multitenant.services.users.UsersService;
import com.example.multitenant.utils.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UsersService usersService;
    private final PasswordEncoder passwordEncoder;
    private final GlobalRolesService globalRolesService;
    private final SecurityContextRepository securityRepository;
    private final RedisService redisService;
    
    public AuthController(AuthenticationManager authenticationManager, UsersService usersService,
     PasswordEncoder passwordEncoder, GlobalRolesService globalRolesService, RedisService redisService,
     SecurityContextRepository securityRepository) {
       this.usersService = usersService;
       this.authenticationManager = authenticationManager;
       this.passwordEncoder = passwordEncoder;
       this.globalRolesService = globalRolesService;
       this.redisService = redisService;
       this.securityRepository = securityRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@Valid @RequestBody RegisterDTO registerDTO, HttpServletRequest req) {
        if(this.usersService.existsByEmail(registerDTO.getEmail())) {
            var respBody = ApiResponses.GetErrResponse(String.format("User with email: '%s' already exists", registerDTO.getEmail()));
            return ResponseEntity.badRequest().body(respBody);
        }

        var user = registerDTO.toUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        var role = this.globalRolesService.findByName(DefaultGlobalRole.USER.getRoleName());
        if(role == null) {
            return ResponseEntity.internalServerError().body(ApiResponses.GetInternalErr());
        }

        user.getRoles().add(role);
        var createdUser = this.usersService.create(user);
        var userDTO = createdUser.toViewDTO();

        this.redisService.createSessionWithUser(req, userDTO);
        var respBody = ApiResponses.OneKey("user", userDTO);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(respBody);
    }

    @PatchMapping("/reset-password")
    public ResponseEntity<Object> resetPassword(@Valid @RequestBody ResetPasswordDTO dto, HttpServletRequest req) {
        var principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponses.Unauthorized());
        }
        
        if(!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            var res = ApiResponses.GetErrResponse(String.format("new passsword and confirm new password are not equal"));
            return ResponseEntity.badRequest().body(res);
        }

        if(dto.getOldPassword().equals(dto.getNewPassword())) {
            var res = ApiResponses.GetErrResponse(String.format("old password and new password cant be the same"));
            return ResponseEntity.badRequest().body(res);
        }

        var userId = principal.getUser().getId();
        var user = this.usersService.findById(userId);
        try {
            var isOldPasswordIsIncorrectTask = CompletableFuture.supplyAsync(() -> !this.passwordEncoder.matches(dto.getOldPassword(), user.getPassword()));
            var isNewAndOldPasswordEqualTask = CompletableFuture.supplyAsync(() -> this.passwordEncoder.matches(dto.getNewPassword(), user.getPassword()));
        
            if(isOldPasswordIsIncorrectTask.get()) {
                var res = ApiResponses.GetErrResponse(String.format("old password is incorrect"));
                return ResponseEntity.badRequest().body(res);
            }

            if(isNewAndOldPasswordEqualTask.get()) {
                var res = ApiResponses.GetErrResponse(String.format("new password can't be the same as the current password"));
                return ResponseEntity.badRequest().body(res);
            }

            var encodedNewPassword = this.passwordEncoder.encode(dto.getNewPassword());
            user.setPassword(encodedNewPassword);
            this.usersService.save(user);
        
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        } catch (InterruptedException | ExecutionException e) {
            throw new AsyncOperationException( "an error has occured during async process");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
            dto.getEmail(), 
            dto.getPassword()
        ));
        
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        this.securityRepository.saveContext(context, request, response);
        
        var principal = (UserPrincipal) auth.getPrincipal();
        var user = principal.getUser();
        var userDTO = user.toViewDTO();
        var respBody = ApiResponses.OneKey("user", userDTO);
        
        this.redisService.storeUserInSession(request, userDTO);

        return ResponseEntity.ok().body(respBody);
    }

    @GetMapping("/user")
    public ResponseEntity<Object> getUserBySession(HttpServletRequest req) {
        var session = req.getSession(false);
        if(session == null) {
            var respBody = ApiResponses.Unauthorized();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respBody);
        }

        var user = redisService.getUserFromSession(req);
        if(user == null) {
            var respBody = ApiResponses.GetInternalErr();
            return ResponseEntity.internalServerError().body(respBody);
        }
        
        return ResponseEntity.ok().body(user);
    }

    @DeleteMapping("/logout")
    public ResponseEntity<Object> logout(HttpServletRequest req) {
        var session = req.getSession(false);
        if(session == null) {
            var respBody = ApiResponses.GetErrResponse("cookie was not found");
            return ResponseEntity.badRequest().body(respBody);
        }

        try {
            session.invalidate();
        } catch (Exception e) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.noContent().build();
    }
}
