package nvb.dev.urlshortener.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String correlationIdHeader = request.getHeader(CORRELATION_ID_HEADER_NAME);
        String resolvedCorrelationId = resolveCorrelationId(correlationIdHeader);

        MDC.put(CORRELATION_ID_MDC_KEY, resolvedCorrelationId);
        try {
            response.addHeader(CORRELATION_ID_HEADER_NAME, resolvedCorrelationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    private String resolveCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.isBlank())
            return UUID.randomUUID().toString();

        try {
            String givenUUID = UUID.fromString(correlationId).toString();
            if (givenUUID.equalsIgnoreCase(correlationId))
                return givenUUID;
        } catch (IllegalArgumentException ignored) {
        }

        return UUID.randomUUID().toString();
    }
}
