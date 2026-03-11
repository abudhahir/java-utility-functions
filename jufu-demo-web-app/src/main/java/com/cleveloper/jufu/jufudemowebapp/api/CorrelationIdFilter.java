package com.cleveloper.jufu.jufudemowebapp.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final ThreadLocal<String> correlationId = new ThreadLocal<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String corrId = request.getHeader(CORRELATION_ID_HEADER);
        if (corrId == null || corrId.isEmpty()) {
            corrId = UUID.randomUUID().toString();
        }

        correlationId.set(corrId);
        response.setHeader(CORRELATION_ID_HEADER, corrId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            correlationId.remove();
        }
    }

    public static String getCorrelationId() {
        return correlationId.get();
    }
}

