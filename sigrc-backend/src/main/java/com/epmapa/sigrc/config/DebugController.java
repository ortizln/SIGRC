package com.epmapa.sigrc.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @GetMapping("/request-info")
    public Map<String, String> requestInfo(HttpServletRequest request) {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("method", request.getMethod());
        info.put("uri", request.getRequestURI());
        info.put("queryString", request.getQueryString() != null ? request.getQueryString() : "");
        info.put("remoteAddr", request.getRemoteAddr());
        info.put("xForwardedFor", request.getHeader("X-Forwarded-For") != null ? request.getHeader("X-Forwarded-For") : "");
        info.put("host", request.getHeader("Host") != null ? request.getHeader("Host") : "");
        info.put("origin", request.getHeader("Origin") != null ? request.getHeader("Origin") : "");
        info.put("scheme", request.getScheme());
        info.put("serverPort", String.valueOf(request.getServerPort()));
        info.put("contextPath", request.getContextPath());
        info.put("servletPath", request.getServletPath());
        return info;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
