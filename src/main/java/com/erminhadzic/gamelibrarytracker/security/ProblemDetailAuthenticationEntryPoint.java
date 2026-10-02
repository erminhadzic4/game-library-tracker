package com.erminhadzic.gamelibrarytracker.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

// Called by Spring Security when a request without a valid token reaches a protected endpoint.
// That happens in the filter chain, outside Spring MVC, so GlobalExceptionHandler would never see it.
// Handing the exception to MVC's exception resolver lets the same handler write the 401 body.
@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    // Several HandlerExceptionResolver beans exist; this is the one that calls @RestControllerAdvice classes
    public ProblemDetailAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) {
        resolver.resolveException(request, response, null, authException);
    }
}
