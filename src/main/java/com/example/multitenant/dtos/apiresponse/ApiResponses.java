package com.example.multitenant.dtos.apiresponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;

import com.example.multitenant.services.cache.BlackListedIpsCacheService;
import com.example.multitenant.utils.ConsoleColorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiResponses {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static Map<String, Object> GetAllResponse(String collectionKey, List<?> list, long count, Integer page, Integer size) {
        return Map.of(collectionKey, list, "count", count,"page", page,"size", size);
    }

    public static Map<String, Object> GetNestedAllResponse(String collectionKey, Object list, long count, Integer page, Integer size) {
        return Map.of(collectionKey, list, "count", count,"page", page,"size", size);
    }

    public static Map<String, Object> OneKey(String modelKey, Object collection) {
        return Map.of(modelKey, collection);
    }

    public static <TCursor> Map<Object, Object> CursorResponse(String modelKey, Object collection, boolean hasNext, TCursor nextCursor) {
        var map = new HashMap<>();
        map.put(modelKey, collection);
        map.put("hasNext", hasNext);
        map.put("nextCursor", nextCursor);
        
        return map;
    }

    public static void SendErrMissingRequiredHeader(HttpServletResponse response, String header) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        var body = Map.of("error", "missing required header: " + header);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    public static void SendErrInvalidTenantId(HttpServletResponse response, String tenantId) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        var body = Map.of("error", String.format("invalid tenantId received: '%s' must be an integer", tenantId));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
    
    public static <K, V> Map<K, V> Keys(K k1, V v1, K k2, V v2) {
        return Map.of(k1, v1, k2, v2);
    }

    public static <K, V> Map<K, V> Keys(K k1, V v1, K k2, V v2,  K k3, V v3) {
        return Map.of(k1, v1, k2, v2, k3, v3);
    }

    public static Map<String, Object> InvalidEmailOrPassword() {
        return Map.of("error", "invalid email or password");
    }

    public static Map<String, Object> GetErrResponse(Exception e) {
        var stackTrace = e.getStackTrace();
        var st = new ArrayList<>();

        if (stackTrace.length > 0) {
            StackTraceElement element = stackTrace[0];

            String fileName = element.getFileName();
            int lineNumber = element.getLineNumber();

            st.add("file"+ " " +fileName);
            st.add("line number"+ " " +lineNumber);
            st.add("class"+ " " +element.getClassName());
            st.add("method"+ " " +element.getMethodName());
        }

        return Map.of("error", e.getMessage(),"trace",st);
    }

    public static Map<String, Object> GetErrResponse(String errorMessage) {
        return Map.of("error", errorMessage);
    }

    public static Map<String, Object> GetErrIdIsRequired(String idName ,Serializable id) {
        return Map.of("error", String.format("%s is required", idName, id));
    }

    public static Map<String, Object> GetInternalErr() {
        return Map.of("error", "Internal server error");
    }

    public static Map<String, Object> GetNotFoundErr(String resourceName, Serializable id) {
        return Map.of("error", String.format("%s with id '%s' was not found", resourceName, id));
    }

    public static Map<String, Object> GetNotFoundErr(String resourceName) {
        return Map.of("error", String.format("%s with was not found", resourceName));
    }

    public static Map<String, Object> GetInternalErr(String message) {
        log.error("[Internal server error] : {}", message);

        return Map.of("error", "Internal server error");
    }

    public static Map<String, Object> StripeError(String message) {
        log.error(ConsoleColorUtils.red("[Stripe Error] : {}"), message);

        return Map.of("error", "Internal server error");
    }

    public static Map<String, Object> Unauthorized() {
        return Map.of("error", "unauthorized");
    }

    public static Map<String, Object> Forbidden() {
        return Map.of("error", "Forbidden");
    }

    public static void SendErrTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        var body = Map.of("error", "Rate limit exceeded. Try again later.");

        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(body));
            writer.flush();
        }
    }

    public static void SendBlockedIpResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        var body = Map.of("error", BlackListedIpsCacheService.defaultLockedMessage);

        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(body));
            writer.flush();
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> MapKeys(Pair<String, Object>... pairs) {
        return List.of(pairs).stream()
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }
}
