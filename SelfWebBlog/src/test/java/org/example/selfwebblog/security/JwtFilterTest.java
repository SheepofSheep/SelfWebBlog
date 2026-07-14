package org.example.selfwebblog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.example.selfwebblog.entity.User;
import org.example.selfwebblog.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtFilterTest {

    @Test
    void usesCurrentDatabaseRoleWhenTokenVersionMatches() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserService userService = mock(UserService.class);
        SessionCookieService cookieService = mock(SessionCookieService.class);
        User user = user(9L, "USER", 3);
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.getUserId("token")).thenReturn(9L);
        when(jwtUtil.getTokenVersion("token")).thenReturn(3);
        when(userService.getById(9L)).thenReturn(user);
        MockHttpServletRequest request = authorizedRequest();
        when(cookieService.readSessionToken(request)).thenReturn("token");
        FilterChain chain = mock(FilterChain.class);

        new JwtFilter(jwtUtil, userService, cookieService)
                .doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute("userId")).isEqualTo(9L);
        assertThat(request.getAttribute("role")).isEqualTo("USER");
        verify(chain).doFilter(same(request), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsTokenAfterServerVersionChanges() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        UserService userService = mock(UserService.class);
        SessionCookieService cookieService = mock(SessionCookieService.class);
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.getUserId("token")).thenReturn(9L);
        when(jwtUtil.getTokenVersion("token")).thenReturn(2);
        when(userService.getById(9L)).thenReturn(user(9L, "ADMIN", 3));
        MockHttpServletRequest request = authorizedRequest();
        when(cookieService.readSessionToken(request)).thenReturn("token");

        new JwtFilter(jwtUtil, userService, cookieService).doFilter(
                request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(request.getAttribute("userId")).isNull();
        assertThat(request.getAttribute("role")).isNull();
    }

    private MockHttpServletRequest authorizedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(SessionCookieService.SESSION_COOKIE, "token"));
        return request;
    }

    private User user(Long id, String role, int tokenVersion) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setTokenVersion(tokenVersion);
        return user;
    }
}
