package org.brian.aisupportagent.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void preservesSafeRequestIdAndMakesItAvailableDuringRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "support-request_123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInsideChain = new AtomicReference<>();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                requestIdInsideChain.set(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY))
        );

        assertEquals("support-request_123", response.getHeader(
                RequestCorrelationFilter.REQUEST_ID_HEADER
        ));
        assertEquals("support-request_123", requestIdInsideChain.get());
        assertNull(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY));
    }

    @Test
    void replacesUnsafeRequestIdWithGeneratedUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "unsafe request id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
        });

        String generatedRequestId = response.getHeader(
                RequestCorrelationFilter.REQUEST_ID_HEADER
        );
        assertNotEquals("unsafe request id", generatedRequestId);
        assertDoesNotThrow(() -> UUID.fromString(generatedRequestId));
        assertNull(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY));
    }

    @Test
    void clearsMdcWhenDownstreamProcessingFails() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(ServletException.class, () -> filter.doFilter(
                request,
                response,
                (ignoredRequest, ignoredResponse) -> {
                    throw new ServletException("failure");
                }
        ));

        assertNull(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY));
    }
}
