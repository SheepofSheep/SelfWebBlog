package org.example.selfwebblog.controller;

import jakarta.servlet.http.HttpServletRequest;

public final class AuthHelper {

    private AuthHelper() {}

    public static boolean isAdmin(HttpServletRequest request) {
        return "ADMIN".equals(request.getAttribute("role"));
    }

    public static Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    public static String getRole(HttpServletRequest request) {
        return (String) request.getAttribute("role");
    }
}
