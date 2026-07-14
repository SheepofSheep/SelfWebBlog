package org.example.selfwebblog.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SessionCookieServiceTest {
    private final SessionCookieService service = new SessionCookieService(false, 3600);

    @Test
    void writesHttpOnlySessionAndReadableCsrfCookies() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.issueSession(response, "signed-token");

        assertThat(response.getHeaders("Set-Cookie"))
                .anySatisfy(value -> assertThat(value)
                        .contains("BLOG_SESSION=signed-token", "HttpOnly", "SameSite=Lax"))
                .anySatisfy(value -> assertThat(value)
                        .contains("XSRF-TOKEN=")
                        .doesNotContain("HttpOnly"));
    }

    @Test
    void validatesMatchingCsrfCookieAndHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(SessionCookieService.CSRF_COOKIE, "same-value"));
        request.addHeader(SessionCookieService.CSRF_HEADER, "same-value");

        assertThat(service.hasValidCsrf(request)).isTrue();
    }
}
