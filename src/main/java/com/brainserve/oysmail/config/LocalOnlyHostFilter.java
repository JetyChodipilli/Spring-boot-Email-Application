package com.brainserve.oysmail.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Prevent browser DNS rebinding against the no-login, local-only mail composer. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocalOnlyHostFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (!LocalHostGuard.isAllowedHost(request.getHeader("Host"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Open this application using localhost.");
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }
}
