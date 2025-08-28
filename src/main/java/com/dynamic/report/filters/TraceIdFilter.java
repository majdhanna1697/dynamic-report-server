package com.dynamic.report.filters;

import com.dynamic.report.services.RSAService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class TraceIdFilter extends OncePerRequestFilter {

    private final RSAService rsaService;
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    public TraceIdFilter(RSAService rsaService) {
        this.rsaService = rsaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString();
            }
            response.setHeader(TRACE_ID_HEADER, traceId);
            MDC.put("traceId", traceId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }

}
