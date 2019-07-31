package com.github.zkoalas.jwts.util;

import com.github.zkoalas.jwts.annotation.Logical;
import com.github.zkoalas.jwts.provider.Token;
import com.google.common.base.CaseFormat;


import javax.servlet.http.HttpServletRequest;

/**
 * 权限检查工具类
 */
public class SubjectUtil {
    public static final String REQUEST_TOKEN_NAME = "JWT_TOKEN";


    /**
     * 检查是否有指定角色
     *
     * @param token
     * @param roles
     * @param logical
     * @return
     */
    public static boolean hasRole(Token token, String[] roles, Logical logical) {
        if (token == null) {
            return false;
        }
        boolean rs = false;
        for (int i = 0; i < roles.length; i++) {
            if (token.getRoles() != null) {
                rs = contains(token.getRoles(), roles[i]);
            }
            if (logical == (rs ? Logical.OR : Logical.AND)) {
                break;
            }
        }
        return rs;
    }

    public static boolean hasRole(Token token, String roles) {
        return hasRole(token, new String[]{roles}, Logical.OR);
    }

    public static boolean hasRole(HttpServletRequest request, String[] roles, Logical logical) {
        return hasRole(getToken(request), roles, logical);
    }

    public static boolean hasRole(HttpServletRequest request, String roles) {
        return hasRole(getToken(request), new String[]{roles}, Logical.OR);
    }

    /**
     * 检查是否有指定权限
     *
     * @param token
     * @param permissions
     * @param logical
     * @return
     */
    public static boolean hasPermission(Token token, String[] permissions, Logical logical) {
        if (token == null) {
            return false;
        }
        boolean rs = false;
        for (int i = 0; i < permissions.length; i++) {
            if (token.getPermissions() != null) {
                rs = contains(token.getPermissions(), permissions[i]);
            }
            if (logical == (rs ? Logical.OR : Logical.AND)) {
                break;
            }
        }
        return rs;
    }

    public static boolean hasPermission(Token token, String permissions) {
        return hasPermission(token, new String[]{permissions}, Logical.OR);
    }

    public static boolean hasPermission(HttpServletRequest request, String[] permissions, Logical logical) {
        return hasPermission(getToken(request), permissions, logical);
    }

    public static boolean hasPermission(HttpServletRequest request, String permissions) {
        return hasPermission(getToken(request), new String[]{permissions}, Logical.OR);
    }

    /**
     * 从request中获取token
     *
     * @param request
     * @return
     */
    public static Token getToken(HttpServletRequest request) {
        return (Token) request.getAttribute(REQUEST_TOKEN_NAME);
    }

    private static boolean contains(String[] strs, String str) {
        for (int i = 0; i < strs.length; i++) {
            if (strs[i].equals(str)) {
                return true;
            }
        }
        return false;
    }


    public static String getJwtToken(){
        return lowerUnderscoreToHump(REQUEST_TOKEN_NAME);
    }

    /**
     * 下划线转驼峰
     * 举例: JWT_TOKEN  to jwtToken
     * @param fieldName
     * @return
     */
    public static String lowerUnderscoreToHump(String fieldName){
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, fieldName);
    }


    /**
     * 驼峰线转下划
     * 举例: jwtToken  to jwt_token
     * @param fieldName
     * @return
     */
    public static String lowerHumpToUnderscore(String fieldName){
       return  CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
    }
}
