package org.yt.jwts.perm;

import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class SimpleUrlPerm implements IUrlPerm {
    @Override
    public String[] getPermission(HttpServletRequest request, HttpServletResponse response, HandlerMethod handler) {
        return new String[]{request.getRequestURI()};
    }

    @Override
    public String[] getRoles(HttpServletRequest request, HttpServletResponse response, HandlerMethod handler) {
        return new String[0];
    }
}
