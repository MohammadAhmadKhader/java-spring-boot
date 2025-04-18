package com.example.multitenant.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.multitenant.services.security.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    CustomUserDetailsService customUserDetailsService;
    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults()).
        csrf((csrf) -> {
            csrf.disable();
        })
        .authorizeHttpRequests((auth) -> {
            auth.requestMatchers("/api/auth/**").permitAll();
            auth.requestMatchers("/api/auth/login", "/api/auth/register").anonymous();
            auth.anyRequest().authenticated();
        })
        .httpBasic(Customizer.withDefaults())
        .formLogin((fl)->{
            fl.disable();
        }).sessionManagement((sessionManagement) ->{
          sessionManagement.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
        });

        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfiguration() {
        CorsConfiguration corsConfigs = new CorsConfiguration();
        corsConfigs.setAllowCredentials(true);
        corsConfigs.setAllowedHeaders(List.of("*"));
        corsConfigs.addAllowedOrigin("http://localhost:8080/api");
        corsConfigs.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfigs);

        return source;
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }


    // @Bean
    // public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
    //     ProviderManager providerManager = (ProviderManager) authenticationConfiguration.getAuthenticationManager();
    //     providerManager.setEraseCredentialsAfterAuthentication(false);
    //     return providerManager;
    // }

    // @Bean 
    // AuthenticationProvider authenticationProvider() {
    //     DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    //     provider.setUserDetailsService(customUserDetailsService);

    //     return provider;
    // }
}
