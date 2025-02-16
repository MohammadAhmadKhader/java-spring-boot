package com.example.demo.services.cache;

import java.time.Duration;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.Session;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository.RedisSession;
import org.springframework.stereotype.Service;

import com.example.demo.dtos.auth.UserPrincipal;
import com.example.demo.dtos.users.UserViewDTO;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class RedisService {
    private final RedisIndexedSessionRepository redisIndexedSessionRepository;

    public RedisService(RedisIndexedSessionRepository redisIndexedSessionRepository) {
        this.redisIndexedSessionRepository = redisIndexedSessionRepository;
    }

    public void storeUserInSession(HttpServletRequest request, UserViewDTO user) {
        var session = getSession(request);
        session.setAttribute("user", user);
    }

    public UserViewDTO getUserFromSession(HttpServletRequest request) {
        var session = getSession(request);
        var user = (UserViewDTO) session.getAttribute("user");
        return user;
    }

    public UserViewDTO getUserFromSession(HttpSession session) {
        var user = (UserViewDTO) session.getAttribute("user");
        return user;
    }

    public Map<String, RedisSession> findSessionsByUserId(String userId) {
        return redisIndexedSessionRepository.findByIndexNameAndIndexValue("userId", userId);
    }

    private HttpSession getSession(HttpServletRequest request) {
        var session = request.getSession(false);
        if( session == null) {
            throw new RuntimeException("User session was not found");
        }

        return session;
    }

    public void createSessionWithUser(HttpServletRequest request, UserViewDTO user) {
        var newSession = request.getSession(true);
        newSession.setAttribute("user", user);
    }

    public UserPrincipal getUserPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null || !auth.isAuthenticated()) {
            return null;
        }

        var userPrincipal = (UserPrincipal) auth.getPrincipal();
        if(userPrincipal instanceof UserPrincipal) {
            return userPrincipal;
        }

        return null;
    }

    public Long getUserIdFromSecurityContext() {
        var userPrincipal = getUserPrincipal();
        if(userPrincipal == null) {
            return null;
        }

        return userPrincipal.getUser().getId();
    }
}
