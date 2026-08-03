package com.epmapa.sigrc.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("REQUEST_LOG");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String remoteAddr = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        String host = request.getHeader("Host");
        String origin = request.getHeader("Origin");
        String contentType = request.getContentType();

        log.info(">>> {} {} {} | from={} forwarded={} host={} origin={} content-type={}",
                method, uri,
                query != null ? "?" + query : "",
                remoteAddr,
                forwarded != null ? forwarded : "-",
                host != null ? host : "-",
                origin != null ? origin : "-",
                contentType != null ? contentType : "-");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.info("    CORS preflight: Access-Control-Request-Method={}, Access-Control-Request-Headers={}",
                    request.getHeader("Access-Control-Request-Method"),
                    request.getHeader("Access-Control-Request-Headers"));
        }

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            log.info("<<< {} {} {} | status={} duration={}ms",
                    method, uri, status, status, duration);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/ws");
    }
}
