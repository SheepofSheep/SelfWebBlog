package org.example.selfwebblog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CsrfFilterTest {
    private final SessionCookieService cookieService = new SessionCookieService(false, 3600);
    private final CsrfFilter filter = new CsrfFilter(cookieService, new ObjectMapper());

    @Test
    void rejectsMutationWithoutMatchingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/posts");
        request.setAttribute(RequestTraceFilter.ATTRIBUTE, "test-request-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("请求校验已过期");
        assertThat(response.getContentAsString()).contains("test-request-id");
    }

    @Test
    void allowsMutationWithDoubleSubmitToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/posts/1/like");
        request.setCookies(new Cookie(SessionCookieService.CSRF_COOKIE, "csrf-value"));
        request.addHeader(SessionCookieService.CSRF_HEADER, "csrf-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
